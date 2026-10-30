package com.combatmasteries;

import com.combatmasteries.cosmetic.GlowStyle;
import com.combatmasteries.cosmetic.HelmetGlow;
import com.combatmasteries.cosmetic.HelmetGlowOverlay;
import com.combatmasteries.tasks.BossDefinition;
import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskEvaluator;
import com.combatmasteries.tasks.TaskList;
import com.combatmasteries.tracking.Attempt;
import com.combatmasteries.tracking.AttemptTracker;
import com.combatmasteries.tracking.KillAssembler;
import com.combatmasteries.tracking.KillDuration;
import com.combatmasteries.tracking.KillEvent;
import com.combatmasteries.tracking.KillMessageParser;
import com.combatmasteries.ui.MasteriesPanel;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.WorldType;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Combat Masteries",
	description = "Ultra-hard combat tasks beyond Grandmaster, with a glowing-eye Tzkal slayer helmet reward",
	tags = {"combat", "achievements", "tasks", "pvm", "grandmaster", "zuk", "helmet"}
)
public class CombatMasteriesPlugin extends Plugin
{
	private static final String PROGRESS_KEY = "progress";
	/**
	 * Where RuneLite's Chat Commands plugin keeps personal bests (read only).
	 */
	private static final String CHAT_COMMANDS_PB_GROUP = "personalbest";
	private static final Set<Integer> PRAYER_VARBITS = Arrays.stream(Prayer.values())
		.map(Prayer::getVarbit)
		.collect(Collectors.toUnmodifiableSet());

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private CombatMasteriesConfig config;

	@Inject
	private Gson gson;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Notifier notifier;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HelmetGlow helmetGlow;

	@Inject
	private HelmetGlowOverlay helmetGlowOverlay;

	@Inject
	@Named("developerMode")
	boolean developerMode;

	private final KillAssembler killAssembler = new KillAssembler();
	private final AttemptTracker attempts = new AttemptTracker();
	private final Map<String, String> bossByNpc = new HashMap<>();
	private final Map<Integer, String> bossByRegion = new HashMap<>();

	private TaskList taskList;
	private MasteriesPanel panel;
	private NavigationButton navButton;

	/**
	 * The logged-in account's progress, or null if not logged in to a standard world.
	 */
	@Nullable
	private MasteryProgress progress;
	/**
	 * Set after a region boss is killed so lingering in its region does not start a new
	 * attempt. Cleared when the player leaves the region.
	 */
	@Nullable
	private String completedRegionBoss;
	/**
	 * Developer mode only: shows a tier's eyes without having unlocked it.
	 */
	@Nullable
	private MasteryTier previewTier;

	@Provides
	CombatMasteriesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatMasteriesConfig.class);
	}

	@Override
	protected void startUp()
	{
		try (InputStream in = CombatMasteriesPlugin.class.getResourceAsStream("tasks.json"))
		{
			taskList = TaskList.load(gson, in);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Unable to load Combat Masteries task list", e);
		}

		bossByNpc.clear();
		bossByRegion.clear();
		for (BossDefinition boss : taskList.getBosses().values())
		{
			boss.getNpcs().forEach(npc -> bossByNpc.put(npc, boss.getName()));
			boss.getRegions().forEach(region -> bossByRegion.put(region, boss.getName()));
		}

		panel = new MasteriesPanel();
		BufferedImage icon = ImageUtil.loadImageResource(CombatMasteriesPlugin.class, "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Combat Masteries")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(helmetGlowOverlay);

		refreshPanel();
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				loadProgress();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(helmetGlowOverlay);
		clientThread.invoke(() -> helmetGlow.setStyle(null));
		killAssembler.reset();
		attempts.abort();
		progress = null;
		completedRegionBoss = null;
		previewTier = null;
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadProgress();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CombatMasteriesConfig.GROUP.equals(event.getGroup()) || PROGRESS_KEY.equals(event.getKey()))
		{
			// Ignore our own progress saves
			return;
		}

		clientThread.invoke(() ->
		{
			if ("creditPersonalBests".equals(event.getKey()) && config.creditPersonalBests())
			{
				creditPersonalBests();
			}
			updateCosmetic();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			// Logging out or hopping mid-fight counts as abandoning it
			failAttempt();
			killAssembler.reset();
			completedRegionBoss = null;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (progress == null
			|| (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM && type != ChatMessageType.FRIENDSCHATNOTIFICATION))
		{
			return;
		}

		String message = event.getMessage();
		int tick = client.getTickCount();

		String boss = KillMessageParser.parseKillCount(message);
		if (boss != null)
		{
			killAssembler.onKillCount(boss, tick);
			return;
		}

		KillDuration duration = KillMessageParser.parseDuration(message);
		if (duration != null)
		{
			killAssembler.onDuration(duration, tick);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (progress == null)
		{
			return;
		}

		int tick = client.getTickCount();
		KillEvent kill = killAssembler.onTick(tick);
		if (kill != null)
		{
			handleKill(kill, attempts.isRegionAttempt());
		}

		updateRegionAttempt(tick);
		checkPrayers();
		if (attempts.checkIdle(tick))
		{
			log.debug("Attempt went idle");
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (progress == null)
		{
			return;
		}

		Player local = client.getLocalPlayer();
		Actor target = event.getActor();
		Hitsplat hitsplat = event.getHitsplat();
		int tick = client.getTickCount();

		if (target == local)
		{
			if (local.getInteracting() instanceof NPC)
			{
				engage((NPC) local.getInteracting(), tick);
			}
			if (isHealthDamage(hitsplat))
			{
				attempts.onDamageTaken(hitsplat.getAmount(), tick);
			}
		}
		else if (target instanceof NPC && hitsplat.isMine())
		{
			engage((NPC) target, tick);
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (progress == null)
		{
			return;
		}

		Player local = client.getLocalPlayer();
		int tick = client.getTickCount();
		if (event.getSource() == local && event.getTarget() instanceof NPC)
		{
			engage((NPC) event.getTarget(), tick);
		}
		else if (event.getSource() instanceof NPC && event.getTarget() == local)
		{
			engage((NPC) event.getSource(), tick);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Catches prayers flicked on and off within a single tick
		if (event.getValue() == 1 && PRAYER_VARBITS.contains(event.getVarbitId()))
		{
			attempts.onPrayerActive();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (attempts.getCurrent() == null)
		{
			return;
		}

		String option = Text.removeTags(event.getMenuOption());
		if ("Eat".equals(option))
		{
			attempts.onEat();
		}
		else if ("Drink".equals(option))
		{
			attempts.onDrink();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			failAttempt();
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		helmetGlow.paint();
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!developerMode)
		{
			return;
		}

		String[] args = event.getArguments();
		switch (event.getCommand().toLowerCase(Locale.ROOT))
		{
			case "cmkill":
				devKill(args);
				break;
			case "cmtier":
				try
				{
					previewTier = args.length == 0 || args[0].equalsIgnoreCase("off") ? null : MasteryTier.valueOf(args[0].toUpperCase(Locale.ROOT));
					updateCosmetic();
					devMessage("Previewing tier: " + (previewTier == null ? "off (showing unlocked tier)" : previewTier.getDisplayName()));
				}
				catch (IllegalArgumentException e)
				{
					devMessage("Usage: ::cmtier ascended|exalted|immortal|off");
				}
				break;
			case "cmattempt":
				devMessage("Attempt: " + attempts.getCurrent());
				break;
			case "cmreset":
				if (progress != null)
				{
					progress = new MasteryProgress();
					saveProgress();
					updateCosmetic();
					refreshPanel();
					devMessage("Progress reset");
				}
				break;
		}
	}

	private void engage(NPC npc, int tick)
	{
		String boss = bossByNpc.get(npc.getName());
		if (boss != null && !attempts.isRegionAttempt())
		{
			attempts.engage(boss, tick);
		}
	}

	private void updateRegionAttempt(int tick)
	{
		if (bossByRegion.isEmpty())
		{
			return;
		}

		Player local = client.getLocalPlayer();
		LocalPoint lp = local == null ? null : local.getLocalLocation();
		if (lp == null)
		{
			return;
		}

		int region = WorldPoint.fromLocalInstance(client, lp).getRegionID();
		String boss = bossByRegion.get(region);
		if (boss == null)
		{
			completedRegionBoss = null;
			if (attempts.isRegionAttempt())
			{
				// Left the Inferno (etc.) without a kill
				failAttempt();
			}
		}
		else if (!boss.equals(completedRegionBoss))
		{
			attempts.enterRegion(boss, tick);
		}
	}

	private void checkPrayers()
	{
		if (attempts.getCurrent() == null || attempts.getCurrent().isPrayed())
		{
			return;
		}

		for (int varbit : PRAYER_VARBITS)
		{
			if (client.getVarbitValue(varbit) == 1)
			{
				attempts.onPrayerActive();
				return;
			}
		}
	}

	private static boolean isHealthDamage(Hitsplat hitsplat)
	{
		if (hitsplat.getAmount() <= 0)
		{
			return false;
		}

		switch (hitsplat.getHitsplatType())
		{
			case HitsplatID.HEAL:
			case HitsplatID.CYAN_UP:
			case HitsplatID.PRAYER_DRAIN:
			case HitsplatID.SANITY_DRAIN:
			case HitsplatID.SANITY_RESTORE:
				return false;
			default:
				return true;
		}
	}

	private void handleKill(KillEvent kill, boolean regionAttempt)
	{
		if (progress == null || !isTrackedWorld())
		{
			attempts.complete(kill.getBoss());
			return;
		}

		log.debug("Kill: {}", kill);
		MasteryTier before = currentTier();

		if (kill.getDuration() != null)
		{
			progress.recordPersonalBest(kill.getBoss(), kill.getDuration().getPersonalBestSeconds());
		}

		Attempt attempt = attempts.complete(kill.getBoss());
		if (attempt != null && regionAttempt)
		{
			completedRegionBoss = kill.getBoss();
		}

		List<MasteryTask> completed = TaskEvaluator.onKill(taskList, progress, kill, attempt, System.currentTimeMillis());
		saveProgress();
		completed.forEach(task -> announceTask(task, false));

		if (config.creditPersonalBests())
		{
			// The kill message reveals the account's real PB, which may predate the plugin
			creditPersonalBests();
		}

		afterProgressChanged(before);
	}

	private void failAttempt()
	{
		Attempt failed = attempts.abort();
		if (failed != null && progress != null)
		{
			log.debug("Attempt failed: {}", failed);
			TaskEvaluator.onAttemptFailed(taskList, progress, failed.getBoss());
			saveProgress();
			refreshPanel();
		}
	}

	private void creditPersonalBests()
	{
		if (progress == null)
		{
			return;
		}

		MasteryTier before = currentTier();
		List<MasteryTask> completed = TaskEvaluator.creditPersonalBests(taskList, progress, this::personalBest, System.currentTimeMillis());
		if (!completed.isEmpty())
		{
			saveProgress();
			completed.forEach(task -> announceTask(task, true));
			afterProgressChanged(before);
		}
	}

	/**
	 * @return the best known personal best for a boss, from the game's kill messages or the
	 * Chat Commands plugin, or null if neither knows one
	 */
	@Nullable
	private Double personalBest(String boss)
	{
		Double ours = progress == null ? null : progress.getPersonalBest(boss);
		Double theirs = configManager.getRSProfileConfiguration(CHAT_COMMANDS_PB_GROUP, boss.toLowerCase(Locale.ROOT).replace(":", ""), double.class);
		if (theirs != null && theirs <= 0)
		{
			theirs = null;
		}
		if (ours == null)
		{
			return theirs;
		}
		return theirs == null ? ours : Math.min(ours, theirs);
	}

	private void loadProgress()
	{
		if (!isTrackedWorld())
		{
			progress = null;
			updateCosmetic();
			refreshPanel();
			return;
		}

		String json = configManager.getRSProfileConfiguration(CombatMasteriesConfig.GROUP, PROGRESS_KEY);
		MasteryProgress loaded = null;
		if (json != null)
		{
			try
			{
				loaded = gson.fromJson(json, MasteryProgress.class);
			}
			catch (JsonSyntaxException e)
			{
				log.warn("Unable to read saved combat mastery progress", e);
			}
		}
		progress = loaded != null ? loaded : new MasteryProgress();

		if (config.creditPersonalBests())
		{
			creditPersonalBests();
		}
		updateCosmetic();
		refreshPanel();
	}

	private void saveProgress()
	{
		if (progress != null)
		{
			configManager.setRSProfileConfiguration(CombatMasteriesConfig.GROUP, PROGRESS_KEY, gson.toJson(progress));
		}
	}

	/**
	 * Mastery progress only counts on normal worlds: not Leagues, Deadman, beta,
	 * speedrunning or tournament worlds.
	 */
	private boolean isTrackedWorld()
	{
		return RuneScapeProfileType.getCurrent(client) == RuneScapeProfileType.STANDARD
			&& !client.getWorldType().contains(WorldType.TOURNAMENT_WORLD);
	}

	@Nullable
	private MasteryTier currentTier()
	{
		return progress == null ? null : MasteryTier.forPoints(progress.countCompleted(taskList), taskList.size());
	}

	private void afterProgressChanged(@Nullable MasteryTier before)
	{
		MasteryTier after = currentTier();
		if (after != null && (before == null || after.ordinal() > before.ordinal()))
		{
			announceTier(after);
		}
		updateCosmetic();
		refreshPanel();
	}

	private void announceTask(MasteryTask task, boolean fromPersonalBest)
	{
		if (config.chatMessages())
		{
			String message = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Congratulations, you've completed a combat mastery task: ")
				.append(ChatColorType.HIGHLIGHT)
				.append(task.getName())
				.append(ChatColorType.NORMAL)
				.append(fromPersonalBest ? " (from your personal best)." : ".")
				.build();
			queueChat(message);
		}
		notifier.notify(config.taskNotification(), "Combat mastery task completed: " + task.getName());
	}

	private void announceTier(MasteryTier tier)
	{
		if (config.chatMessages())
		{
			String message = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("You have unlocked ")
				.append(tier.getColor(), tier.getDisplayName())
				.append(ChatColorType.NORMAL)
				.append("! Your Tzkal slayer helmet's eyes now glow.")
				.build();
			queueChat(message);
		}
		notifier.notify(config.taskNotification(), "Combat Masteries: " + tier.getDisplayName() + " unlocked!");
	}

	private void queueChat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private void updateCosmetic()
	{
		MasteryTier tier = previewTier != null ? previewTier : displayedTier();
		GlowStyle style = config.helmetGlow() && tier != null ? GlowStyle.forTier(tier) : null;
		clientThread.invoke(() -> helmetGlow.setStyle(style));
	}

	/**
	 * @return the tier whose eyes to show: the configured tier, capped at the highest unlocked
	 */
	@Nullable
	private MasteryTier displayedTier()
	{
		MasteryTier unlocked = currentTier();
		if (unlocked == null)
		{
			return null;
		}

		CombatMasteriesConfig.DisplayTier chosen = config.displayTier();
		if (chosen == CombatMasteriesConfig.DisplayTier.HIGHEST)
		{
			return unlocked;
		}
		MasteryTier wanted = MasteryTier.valueOf(chosen.name());
		return wanted.ordinal() <= unlocked.ordinal() ? wanted : unlocked;
	}

	private void refreshPanel()
	{
		if (panel == null || taskList == null)
		{
			return;
		}
		MasteriesPanel p = panel;
		TaskList tasks = taskList;
		MasteryProgress snapshot = progress;
		SwingUtilities.invokeLater(() -> p.update(tasks, snapshot));
	}

	/**
	 * ::cmkill Boss_Name m:ss [dmg] [prayer] [food] [potion]
	 * Feeds a pretend kill through task evaluation, for testing without the boss.
	 */
	private void devKill(String[] args)
	{
		if (progress == null || args.length < 2)
		{
			devMessage("Usage: ::cmkill Boss_Name m:ss [dmg] [prayer] [food] [potion]");
			return;
		}

		String boss = args[0].replace('_', ' ');
		double seconds = KillMessageParser.timeStringToSeconds(args[1]);
		List<String> flags = Arrays.asList(args).subList(2, args.length);
		Attempt attempt = Attempt.of(boss,
			flags.contains("dmg") ? 1 : 0,
			flags.contains("prayer"),
			flags.contains("food"),
			flags.contains("potion"));

		MasteryTier before = currentTier();
		KillEvent kill = new KillEvent(boss, new KillDuration(seconds, seconds, false, null));
		List<MasteryTask> completed = TaskEvaluator.onKill(taskList, progress, kill, attempt, System.currentTimeMillis());
		saveProgress();
		completed.forEach(task -> announceTask(task, false));
		devMessage("Simulated " + boss + " in " + KillMessageParser.formatSeconds(seconds) + ", completed " + completed.size() + " task(s)");
		afterProgressChanged(before);
	}

	private void devMessage(String message)
	{
		queueChat(new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append("[Combat Masteries dev] " + message).build());
	}
}
