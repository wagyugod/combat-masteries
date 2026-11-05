package com.combatmasteries;

import com.combatmasteries.cosmetic.GlowStyle;
import com.combatmasteries.cosmetic.HelmetGlow;
import com.combatmasteries.tasks.BossDefinition;
import com.combatmasteries.tasks.ConditionType;
import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskEvaluator;
import com.combatmasteries.tasks.TaskList;
import com.combatmasteries.tracking.Attempt;
import com.combatmasteries.tracking.AttemptTracker;
import com.combatmasteries.tracking.DevKill;
import com.combatmasteries.tracking.KillAssembler;
import com.combatmasteries.tracking.KillDuration;
import com.combatmasteries.tracking.KillEvent;
import com.combatmasteries.tracking.KillMessageParser;
import com.combatmasteries.tracking.Supplies;
import com.combatmasteries.tracking.TripTracker;
import com.combatmasteries.ui.CombatAchievementsButton;
import com.combatmasteries.ui.CompletionPopup;
import com.combatmasteries.ui.MasteriesPanel;
import com.combatmasteries.ui.MasteriesWindow;
import com.combatmasteries.ui.window.WindowModel;
import com.combatmasteries.ui.window.WindowView;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
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
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
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
	description = "Endgame PvM speed and challenge tasks beyond Grandmaster, with a glowing-eye Tzkal Slayer Helmet reward",
	tags = {"combat", "achievements", "tasks", "pvm", "grandmaster", "zuk", "helmet", "speedrun"}
)
public class CombatMasteriesPlugin extends Plugin
{
	private static final String PROGRESS_KEY = "progress";
	/**
	 * Where RuneLite's Chat Commands plugin keeps personal bests (read only).
	 */
	private static final String CHAT_COMMANDS_PB_GROUP = "personalbest";
	private static final String INFERNO_BOSS = "TzKal-Zuk";
	/**
	 * The game's colour tag for combat task names: dark green on an opaque chatbox, bright
	 * green on a transparent one.
	 */
	private static final String TASK_COLOUR = "@ach_comp@";
	private static final int PILLAR_DEADLINE_WAVE = 4;
	private static final int PILLAR_COUNT = 3;
	/**
	 * Graphics shown on a player who receives heal other, energy (special attack) transfer,
	 * heal group or vengeance other. Any of these ends a one-inventory trip.
	 */
	private static final Set<Integer> ASSIST_SPOTANIMS = ImmutableSet.of(
		SpotanimID.QUEST_LUNAR_SPELLBOOK_ENERGY_HEAL_SPOT_ANIM,
		SpotanimID.QUEST_LUNAR_SPELLBOOK_HEAL_GROUP_SPOT_ANIM,
		SpotanimID.QUEST_LUNAR_SPELLBOOK_ENERGY_EXCHANGE_SPOT_ANIM,
		SpotanimID.QUEST_LUNAR_SPELLBOOK_ENERGY_EXCHANGE_SPOT_ANIM_LOW,
		SpotanimID.QUEST_LUNAR_SPELLBOOK_VENGEANCE_OTHER_SPOT_ANIM
	);
	/**
	 * Object options that restore stats: pools (POH, Ferox Enclave) and altars.
	 */
	private static final Set<String> RESTORE_OPTIONS = ImmutableSet.of("Drink", "Pray", "Pray-at");
	/**
	 * Carrying a looting bag fails one-inventory tasks: it holds extra supplies.
	 */
	private static final Set<Integer> LOOTING_BAGS = ImmutableSet.of(ItemID.LOOTING_BAG, ItemID.LOOTING_BAG_OPEN);
	private static final Set<MenuAction> OBJECT_ACTIONS = ImmutableSet.of(
		MenuAction.GAME_OBJECT_FIRST_OPTION,
		MenuAction.GAME_OBJECT_SECOND_OPTION,
		MenuAction.GAME_OBJECT_THIRD_OPTION,
		MenuAction.GAME_OBJECT_FOURTH_OPTION,
		MenuAction.GAME_OBJECT_FIFTH_OPTION
	);

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
	private CompletionPopup popup;

	@Inject
	private MasteriesWindow window;

	@Inject
	private CombatAchievementsButton caButton;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	@Named("developerMode")
	boolean developerMode;

	private final KillAssembler killAssembler = new KillAssembler();
	private final AttemptTracker attempts = new AttemptTracker();
	private final TripTracker trip = new TripTracker();
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
	 * Inferno pillars seen since entering the region.
	 */
	private int pillarsSeen;
	/**
	 * Food and potion doses in the inventory, or -1 until the inventory is first seen.
	 */
	private int lastSupplies = -1;
	/**
	 * Highest Tombs of Amascut raid level seen since the current raid started. The level shown
	 * at the end can be lower: missed invocation time limits are taken off it.
	 */
	private int toaPeakRaidLevel;
	/**
	 * True while a looting bag is in the inventory; kills then don't count towards a trip.
	 */
	private boolean holdingLootingBag;
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

		bossByRegion.clear();
		for (BossDefinition boss : taskList.getBosses().values())
		{
			boss.getRegions().forEach(region -> bossByRegion.put(region, boss.getName()));
		}

		panel = new MasteriesPanel(() -> clientThread.invoke(() -> openWindow(WindowView.OVERVIEW)));
		BufferedImage icon = ImageUtil.loadImageResource(CombatMasteriesPlugin.class, "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Combat Masteries")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(window);
		mouseManager.registerMouseListener(window);
		mouseManager.registerMouseWheelListener(window);
		keyManager.registerKeyListener(window);
		clientThread.invokeLater(this::updateCaButton);

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
		window.close();
		overlayManager.remove(window);
		mouseManager.unregisterMouseListener(window);
		mouseManager.unregisterMouseWheelListener(window);
		keyManager.unregisterKeyListener(window);
		clientThread.invoke(() ->
		{
			helmetGlow.setStyle(null);
			caButton.remove();
		});
		killAssembler.reset();
		attempts.abort();
		trip.restock("plugin stopped");
		progress = null;
		completedRegionBoss = null;
		pillarsSeen = 0;
		lastSupplies = -1;
		holdingLootingBag = false;
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
			// Logging out or hopping mid-run abandons it
			window.close();
			attempts.abort();
			killAssembler.reset();
			completedRegionBoss = null;
			pillarsSeen = 0;
			lastSupplies = -1;
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
			return;
		}

		Attempt attempt = attempts.getCurrent();
		if (attempt != null && INFERNO_BOSS.equals(attempt.getBoss()) && KillMessageParser.parseWave(message) == PILLAR_DEADLINE_WAVE)
		{
			int standing = (int) client.getTopLevelWorldView().npcs().stream()
				.filter(npc -> npc.getId() == NpcID.INFERNO_INVISIBLE_3X3)
				.count();
			boolean cleared = pillarsSeen >= PILLAR_COUNT && standing == 0;
			attempt.setPillarsCleared(cleared);
			log.debug("Inferno wave {}: pillars seen {}, standing {}", PILLAR_DEADLINE_WAVE, pillarsSeen, standing);
			if (!cleared)
			{
				taskList.getTasks().stream()
					.filter(t -> t.isTracked() && t.hasCondition(ConditionType.INFERNO_NO_PILLARS) && !progress.isCompleted(t.getId()))
					.forEach(t -> announceFailure(t, standing > 0
						? "A pillar was still standing when wave 4 began."
						: "The run was not watched from the start."));
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		popup.onTick();
		updateCaButton();
		if (progress == null)
		{
			return;
		}

		int tick = client.getTickCount();
		KillEvent kill = killAssembler.onTick(tick);
		if (kill != null)
		{
			handleKill(kill);
		}

		updateRegionAttempt(tick);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		for (int script : CombatAchievementsButton.BUILD_SCRIPTS)
		{
			if (event.getScriptId() == script)
			{
				// A Combat Achievements tab was (re)built along with its title bar
				updateCaButton();
				return;
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc.getId() == NpcID.INFERNO_INVISIBLE_3X3)
		{
			pillarsSeen++;
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			attempts.abort();
			restockTrip("You died.");
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN || event.getGroupId() == InterfaceID.SHARED_BANK)
		{
			restockTrip("You opened a bank.");
		}
		for (int group : CombatAchievementsButton.GROUPS)
		{
			if (event.getGroupId() == group)
			{
				// After the tab's load scripts have built its title bar
				clientThread.invokeLater(this::updateCaButton);
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption());
		if (OBJECT_ACTIONS.contains(event.getMenuAction()) && RESTORE_OPTIONS.contains(option))
		{
			restockTrip("You restored your stats at " + Text.removeTags(event.getMenuTarget()) + ".");
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		ItemContainer inventory = event.getItemContainer();
		int supplies = countSupplies(inventory);
		if (lastSupplies >= 0 && supplies > lastSupplies)
		{
			restockTrip("You gained food or potions.");
		}
		lastSupplies = supplies;

		holdingLootingBag = inventory != null && LOOTING_BAGS.stream().anyMatch(inventory::contains);
		if (holdingLootingBag)
		{
			restockTrip("You had a looting bag in your inventory.");
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		Player local = client.getLocalPlayer();
		if (event.getActor() != local)
		{
			return;
		}

		for (int spotanim : ASSIST_SPOTANIMS)
		{
			if (local.hasSpotAnim(spotanim))
			{
				restockTrip("Another player assisted you.");
				return;
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.DOM_DEEPEST_LEVEL)
		{
			checkDeepestDelve(event.getValue());
		}

		int varbit = event.getVarbitId();
		if (varbit == VarbitID.TOA_CLIENT_RAID_LEVEL_STOP_AUTO_UPDATING && event.getValue() == 1)
		{
			// The raid has started and its level is locked in
			toaPeakRaidLevel = client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL);
		}
		else if (varbit == VarbitID.TOA_CLIENT_RAID_LEVEL
			&& client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL_STOP_AUTO_UPDATING) == 1)
		{
			toaPeakRaidLevel = Math.max(toaPeakRaidLevel, event.getValue());
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
			case "cmdelve":
				if (args.length == 1)
				{
					checkDeepestDelve(Integer.parseInt(args[0]));
				}
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
					devMessage("Unknown tier \"" + String.join(" ", args) + "\". Type ::cmtier followed by ascended, exalted, immortal or off");
				}
				break;
			case "cmattempt":
				devMessage("Attempt: " + attempts.getCurrent() + ", pillars seen: " + pillarsSeen);
				break;
			case "cmhelm":
				devMessage("Helmet: " + helmetGlow.describe());
				break;
			case "cmtoa":
				devMessage("ToA: raid level " + client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL)
					+ ", locked " + client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL_STOP_AUTO_UPDATING)
					+ ", peak this raid " + toaPeakRaidLevel);
				break;
			case "cmdoom":
				devMessage("Doom: deepest " + client.getVarpValue(VarPlayerID.DOM_DEEPEST_LEVEL)
					+ ", previous deepest " + client.getVarpValue(VarPlayerID.DOM_PREVIOUS_DEEPEST_LEVEL)
					+ ", last delve " + client.getVarpValue(VarPlayerID.DOM_LAST_DELVE_LEVEL)
					+ ", current " + client.getVarpValue(VarPlayerID.DOM_CURRENT_LEVEL_TEMP)
					+ ", 8+ clears " + client.getVarpValue(VarPlayerID.DOM_LEVEL_8_PLUS_COMPLETIONS));
				break;
			case "cmtrip":
				devMessage("Trip kills: " + trip.getKills() + ", last restock: " + trip.getLastRestockReason());
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
			pillarsSeen = 0;
			if (attempts.getCurrent() != null)
			{
				// Left the Inferno without a kill
				attempts.abort();
			}
		}
		else if (!boss.equals(completedRegionBoss))
		{
			attempts.enterRegion(boss, tick);
		}
	}

	private int countSupplies(@Nullable ItemContainer inventory)
	{
		if (inventory == null)
		{
			return 0;
		}

		int units = 0;
		for (Item item : inventory.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			ItemComposition def = client.getItemDefinition(item.getId());
			if (Supplies.isConsumable(def.getInventoryActions()))
			{
				units += Supplies.units(def.getName(), item.getQuantity());
			}
		}
		return units;
	}

	/**
	 * Adds the raid context the kill messages don't carry: team size and raid level.
	 */
	private KillEvent withRaidContext(KillEvent kill)
	{
		String boss = kill.getBoss();
		if (boss.startsWith("Theatre of Blood"))
		{
			return kill.withTeamSize(nonZero(countSet(VarbitID.TOB_CLIENT_P0, VarbitID.TOB_CLIENT_P1,
				VarbitID.TOB_CLIENT_P2, VarbitID.TOB_CLIENT_P3, VarbitID.TOB_CLIENT_P4)));
		}
		if (boss.startsWith("Tombs of Amascut"))
		{
			return kill
				.withTeamSize(nonZero(countSet(VarbitID.TOA_CLIENT_P0, VarbitID.TOA_CLIENT_P1, VarbitID.TOA_CLIENT_P2,
					VarbitID.TOA_CLIENT_P3, VarbitID.TOA_CLIENT_P4, VarbitID.TOA_CLIENT_P5, VarbitID.TOA_CLIENT_P6,
					VarbitID.TOA_CLIENT_P7)))
				.withRaidLevel(Math.max(toaPeakRaidLevel, client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL)));
		}
		if (kill.getDuration() != null && kill.getDuration().getTeamSize() != null)
		{
			// Chambers of Xeric prints the team size in its completion message
			return kill.withTeamSize(KillMessageParser.parseTeamSize(kill.getDuration().getTeamSize()));
		}
		return kill;
	}

	private int countSet(int... varbits)
	{
		int count = 0;
		for (int varbit : varbits)
		{
			count += Math.min(client.getVarbitValue(varbit), 1);
		}
		return count;
	}

	@Nullable
	private static Integer nonZero(int value)
	{
		return value > 0 ? value : null;
	}

	private void handleKill(KillEvent rawKill)
	{
		if (progress == null || !isTrackedWorld())
		{
			attempts.complete(rawKill.getBoss());
			return;
		}

		KillEvent kill = withRaidContext(rawKill);
		if (kill.getBoss().startsWith("Tombs of Amascut"))
		{
			toaPeakRaidLevel = 0;
		}
		log.debug("Kill: {}", kill);
		MasteryTier before = currentTier();

		recordPersonalBests(kill);

		Attempt attempt = attempts.complete(kill.getBoss());
		if (attempt != null)
		{
			completedRegionBoss = kill.getBoss();
		}
		if (holdingLootingBag)
		{
			restockTrip("You had a looting bag in your inventory.");
		}
		else
		{
			trip.onKill(kill.getBoss());
		}

		List<MasteryTask> completed = TaskEvaluator.onKill(taskList, progress, kill, attempt, trip, System.currentTimeMillis());
		saveProgress();
		completed.forEach(this::announceTask);

		if (config.creditPersonalBests())
		{
			// The kill message reveals the account's real PB, which may predate the plugin
			creditPersonalBests();
		}

		afterProgressChanged(before);
	}

	private void recordPersonalBests(KillEvent kill)
	{
		KillDuration time = taskList.usesTotalTime(kill.getBoss()) ? kill.getTotalDuration() : kill.getDuration();
		if (time == null)
		{
			return;
		}
		progress.recordPersonalBest(MasteryProgress.personalBestKey(kill.getBoss(), null), time.getPersonalBestSeconds());
		if (kill.getTeamSize() != null)
		{
			// The reported PB may be for another team size; this kill's own time is known to be for this one
			progress.recordPersonalBest(MasteryProgress.personalBestKey(kill.getBoss(), kill.getTeamSize()), time.getSeconds());
		}
	}

	private void checkDeepestDelve(int deepest)
	{
		if (progress == null || deepest <= 0)
		{
			return;
		}

		MasteryTier before = currentTier();
		List<MasteryTask> completed = TaskEvaluator.onDeepestDelve(taskList, progress, deepest, System.currentTimeMillis());
		if (!completed.isEmpty())
		{
			saveProgress();
			completed.forEach(this::announceTask);
			afterProgressChanged(before);
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
			completed.forEach(this::announceTask);
			afterProgressChanged(before);
		}
	}

	/**
	 * @return the best known personal best for a boss and team size, from the game's kill
	 * messages or the Chat Commands plugin, or null if neither knows one
	 */
	@Nullable
	private Double personalBest(String boss, @Nullable Integer teamSize)
	{
		String key = MasteryProgress.personalBestKey(boss, teamSize);
		Double ours = progress == null ? null : progress.getPersonalBest(key);
		Double theirs = null;
		if (!taskList.usesTotalTime(boss))
		{
			// Chat Commands records challenge times, not total times
			theirs = configManager.getRSProfileConfiguration(CHAT_COMMANDS_PB_GROUP, key, double.class);
			if (theirs != null && theirs <= 0)
			{
				theirs = null;
			}
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
		checkDeepestDelve(client.getVarpValue(VarPlayerID.DOM_DEEPEST_LEVEL));
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

	/**
	 * Announces a completed task the way the game announces a combat task: the same chat
	 * message wording and colour tag, and the game's own notification pop-up.
	 */
	private void announceTask(MasteryTask task)
	{
		if (config.chatMessages())
		{
			queueGameMessage("Congratulations, you've completed a combat mastery task: " + TASK_COLOUR + task.getName() + "</col> (1 point).");
		}
		if (config.completionPopup())
		{
			popup.show("Combat Mastery Task Completed!", "Task Completed: <col=ffffff>" + task.getName() + "</col> (1 point)");
		}
		notifier.notify(config.taskNotification(), "Combat mastery task completed: " + task.getName());
	}

	/**
	 * Announces that an attempt at a task has failed, worded like the game's combat task
	 * failure messages.
	 */
	private void announceFailure(MasteryTask task, String reason)
	{
		if (config.chatMessages())
		{
			queueGameMessage("You have failed " + TASK_COLOUR + task.getName() + "</col>: " + reason);
		}
	}

	private void announceTier(MasteryTier tier)
	{
		if (config.chatMessages())
		{
			queueGameMessage("Congratulations, you've unlocked " + TASK_COLOUR + tier.getDisplayName() + "</col>! Your Tzkal Slayer Helmet's eyes now glow.");
		}
		if (config.completionPopup())
		{
			popup.show("Combat Mastery Tier Unlocked!", "<col=ffffff>" + tier.getDisplayName() + "</col>");
		}
		notifier.notify(config.taskNotification(), "Combat Masteries: " + tier.getDisplayName() + " unlocked!");
	}

	/**
	 * Ends the current one-inventory trip, announcing any task it had started on as failed.
	 */
	private void restockTrip(String reason)
	{
		if (progress != null)
		{
			TaskEvaluator.tripTasksInProgress(taskList, progress, trip).forEach(task -> announceFailure(task, reason));
		}
		trip.restock(reason);
	}

	/**
	 * Adds a plain game message, so the game's own colour tags (such as {@link #TASK_COLOUR})
	 * render exactly as they do for real combat tasks.
	 */
	private void queueGameMessage(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.value(message)
			.build());
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
		clientThread.invoke(this::refreshWindow);
	}

	private void updateCaButton()
	{
		caButton.update(this::openWindow);
	}

	/**
	 * Opens the Combat Masteries window on a tab, if logged in to a world that tracks progress. Call on the
	 * client thread.
	 */
	private void openWindow(WindowView view)
	{
		if (progress == null || taskList == null)
		{
			return;
		}
		window.open(view, buildWindowModel());
	}

	/**
	 * Shows the latest progress in the window if it is open, or closes it once logged out.
	 */
	private void refreshWindow()
	{
		if (!window.isOpen())
		{
			return;
		}
		if (progress == null || taskList == null)
		{
			window.close();
			return;
		}
		window.setModel(buildWindowModel());
	}

	private WindowModel buildWindowModel()
	{
		Player local = client.getLocalPlayer();
		String name = local != null && local.getName() != null ? local.getName() : "";
		return WindowModel.build(taskList, progress, name, this::personalBest);
	}

	/**
	 * ::cmkill, e.g. "::cmkill Solo ToB 44:44" or "::cmkill toa 30:00 solo level=600".
	 * Feeds a pretend kill through task evaluation, for testing without the content.
	 */
	private void devKill(String[] args)
	{
		if (progress == null)
		{
			devMessage("Log in to a normal world first");
			return;
		}

		DevKill dev;
		try
		{
			dev = DevKill.parse(args);
		}
		catch (IllegalArgumentException e)
		{
			devMessage(e.getMessage() + ". Examples: ::cmkill Solo ToB 44:44, ::cmkill inferno 44:59 pillars, "
				+ "::cmkill toa 30:00 solo level=600, ::cmkill awakened levi");
			return;
		}

		String boss = dev.getBoss();
		KillDuration time = dev.getSeconds() == null ? null : new KillDuration(dev.getSeconds(), dev.getSeconds(), false, null, false);
		Double totalSeconds = dev.getTotalSeconds() != null ? dev.getTotalSeconds() : dev.getSeconds();
		KillDuration total = totalSeconds == null ? null : new KillDuration(totalSeconds, totalSeconds, false, null, true);
		Attempt attempt = null;
		if (dev.isPillarsCleared())
		{
			attempt = new Attempt(boss, 0);
			attempt.setPillarsCleared(true);
		}

		MasteryTier before = currentTier();
		KillEvent kill = new KillEvent(boss, time, total, dev.getTeamSize(), dev.getRaidLevel());
		trip.onKill(boss);
		List<MasteryTask> completed = TaskEvaluator.onKill(taskList, progress, kill, attempt, trip, System.currentTimeMillis());
		saveProgress();
		completed.forEach(this::announceTask);
		devMessage("Simulated " + boss
			+ (dev.getSeconds() != null ? " in " + KillMessageParser.formatSeconds(dev.getSeconds()) : "")
			+ (dev.getTeamSize() != null ? ", team " + dev.getTeamSize() : "")
			+ (dev.getRaidLevel() != null ? ", level " + dev.getRaidLevel() : "")
			+ ": completed " + completed.size() + " task(s)");
		afterProgressChanged(before);
	}

	private void devMessage(String message)
	{
		queueChat(new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append("[Combat Masteries dev] " + message).build());
	}
}
