package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskCategory;
import com.combatmasteries.tasks.TaskList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Everything the Combat Masteries window shows, worked out from the task list, the player's progress and
 * their name. The renderer only reads this, so it never touches the client.
 */
@Value
public class WindowModel
{
	/**
	 * Order of the content on the Bosses tab and in the Content filter.
	 */
	public static final List<String> CONTENT_ORDER = Collections.unmodifiableList(Arrays.asList(
		"Inferno",
		"Colosseum",
		"Theatre of Blood",
		"Theatre of Blood: Hard Mode",
		"Chambers of Xeric: CM",
		"Tombs of Amascut",
		"Doom of Mokhaiotl",
		"Awakened DT2",
		"Yama Contracts"));

	/**
	 * Looks up a personal best in seconds for a boss (as named in its kill message) and team size.
	 */
	public interface PersonalBests
	{
		@Nullable
		Double get(String boss, @Nullable Integer teamSize);
	}

	@Value
	public static class Tier
	{
		MasteryTier tier;
		String name;
		/**
		 * Points needed to unlock it.
		 */
		int threshold;
		/**
		 * Points counted towards it, at most the threshold.
		 */
		int progress;

		public boolean isUnlocked()
		{
			return progress >= threshold;
		}
	}

	@Value
	public static class PersonalBest
	{
		String label;
		/**
		 * Formatted time, or "-" when there is none.
		 */
		String value;
	}

	@Value
	public static class Task
	{
		String id;
		String name;
		String description;
		TaskCategory category;
		String content;
		boolean completed;
	}

	@Value
	public static class Content
	{
		String name;
		int tasks;
		int completed;
	}

	@Value
	public static class RewardGroup
	{
		MasteryTier tier;
		List<String> lines;
	}

	String playerName;
	int points;
	int totalTasks;
	List<Tier> tiers;
	List<PersonalBest> personalBests;
	List<Task> tasks;
	List<Content> contents;
	Map<MasteryTier, List<RewardGroup>> rewards;

	/**
	 * @return the first tier not yet unlocked, or null when all are
	 */
	@Nullable
	public Tier nextTier()
	{
		for (Tier tier : tiers)
		{
			if (!tier.isUnlocked())
			{
				return tier;
			}
		}
		return null;
	}

	/**
	 * @return the points of the highest unlocked tier, or 0
	 */
	public int previousThreshold()
	{
		int previous = 0;
		for (Tier tier : tiers)
		{
			if (tier.isUnlocked())
			{
				previous = tier.getThreshold();
			}
		}
		return previous;
	}

	/**
	 * @return the points needed for the top tier
	 */
	public int maxThreshold()
	{
		return tiers.isEmpty() ? 1 : Math.max(1, tiers.get(tiers.size() - 1).getThreshold());
	}

	public String pointsText()
	{
		Tier next = nextTier();
		if (next == null)
		{
			return "Total Points: " + points + " - All tiers unlocked";
		}
		return "Total Points: " + points + " - Next unlock in " + (next.getThreshold() - points) + " points";
	}

	public static WindowModel build(TaskList taskList, MasteryProgress progress, String playerName)
	{
		return build(taskList, progress, playerName, (boss, team) -> progress.getPersonalBest(MasteryProgress.personalBestKey(boss, team)));
	}

	public static WindowModel build(TaskList taskList, MasteryProgress progress, String playerName, PersonalBests personalBests)
	{
		int total = taskList.size();
		int points = progress.countCompleted(taskList);

		List<Tier> tiers = new ArrayList<>();
		for (MasteryTier tier : MasteryTier.values())
		{
			int threshold = tier.threshold(total);
			tiers.add(new Tier(tier, tierName(tier), threshold, Math.min(points, threshold)));
		}

		List<PersonalBest> pbs = new ArrayList<>();
		pbs.add(personalBest("Inferno", personalBests.get("TzKal-Zuk", null)));
		pbs.add(personalBest("Colosseum", personalBests.get("Sol Heredit", null)));
		pbs.add(personalBest("ToB (Duo)", personalBests.get("Theatre of Blood", 2)));
		pbs.add(personalBest("ToB (Trio)", personalBests.get("Theatre of Blood", 3)));
		pbs.add(personalBest("ToB (5-Scale)", personalBests.get("Theatre of Blood", 5)));
		pbs.add(personalBest("CM (Solo)", personalBests.get("Chambers of Xeric Challenge Mode", 1)));
		pbs.add(personalBest("CM (Trio)", personalBests.get("Chambers of Xeric Challenge Mode", 3)));
		pbs.add(personalBest("CM (5-Scale)", personalBests.get("Chambers of Xeric Challenge Mode", 5)));
		pbs.add(personalBest("ToA Expert (Solo)", personalBests.get("Tombs of Amascut: Expert Mode", 1)));

		List<Task> tasks = new ArrayList<>();
		Map<String, int[]> byContent = new LinkedHashMap<>();
		for (String content : CONTENT_ORDER)
		{
			byContent.put(content, new int[2]);
		}
		for (MasteryTask t : taskList.getTasks())
		{
			boolean done = progress.isCompleted(t.getId());
			tasks.add(new Task(t.getId(), t.getName(), t.getDescription(), t.getCategory(), t.getContent(), done));
			int[] counts = byContent.computeIfAbsent(t.getContent(), k -> new int[2]);
			counts[0]++;
			if (done)
			{
				counts[1]++;
			}
		}
		List<Content> contents = new ArrayList<>();
		byContent.forEach((name, counts) -> contents.add(new Content(name, counts[0], counts[1])));

		return new WindowModel(playerName, points, total, Collections.unmodifiableList(tiers),
			Collections.unmodifiableList(pbs), Collections.unmodifiableList(tasks), Collections.unmodifiableList(contents),
			rewards());
	}

	/**
	 * @return "Ascended" for Ascended Grandmaster
	 */
	static String tierName(MasteryTier tier)
	{
		String name = tier.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static PersonalBest personalBest(String label, @Nullable Double seconds)
	{
		return new PersonalBest(label, seconds == null || seconds <= 0 ? "-" : formatTime(seconds));
	}

	/**
	 * Formats a time like the game's personal best messages: m:ss, or h:mm:ss, with hundredths when the
	 * time has them.
	 */
	public static String formatTime(double seconds)
	{
		long hundredths = Math.round(seconds * 100);
		long whole = hundredths / 100;
		long fraction = hundredths % 100;
		long h = whole / 3600;
		long m = (whole % 3600) / 60;
		long s = whole % 60;
		String time = h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
		return fraction == 0 ? time : time + String.format(".%02d", fraction);
	}

	private static Map<MasteryTier, List<RewardGroup>> rewards()
	{
		Map<MasteryTier, List<RewardGroup>> rewards = new LinkedHashMap<>();
		rewards.put(MasteryTier.ASCENDED, Collections.singletonList(new RewardGroup(MasteryTier.ASCENDED, Arrays.asList(
			"<col=ffffff>Tzkal Slayer Helmet:</col> eyes glow red to orange",
			"<col=ffffff>Tzkal Slayer Helmet:</col> light spills onto the metal around the eyes"))));
		rewards.put(MasteryTier.EXALTED, Collections.singletonList(new RewardGroup(MasteryTier.EXALTED, Collections.singletonList(
			"<col=ffffff>Tzkal Slayer Helmet:</col> eyes glow ice blue to azure"))));
		rewards.put(MasteryTier.IMMORTAL, Collections.singletonList(new RewardGroup(MasteryTier.IMMORTAL, Collections.singletonList(
			"<col=ffffff>Tzkal Slayer Helmet:</col> eyes glow Radiant oathplate white"))));
		return Collections.unmodifiableMap(rewards);
	}
}
