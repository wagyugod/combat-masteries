package com.combatmasteries.tasks;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One account's progress. Saved as JSON in the RuneScape profile config.
 */
public class MasteryProgress
{
	private Map<String, Completion> completed = new LinkedHashMap<>();
	/**
	 * Personal bests in seconds as reported by the game, keyed by {@link #personalBestKey}.
	 */
	private Map<String, Double> personalBests = new HashMap<>();

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Completion
	{
		/**
		 * Epoch millis.
		 */
		private long timestamp;
		/**
		 * True when credited from a record the game kept from before the plugin was
		 * watching (a personal best or deepest delve).
		 */
		private boolean fromPersonalBest;
	}

	/**
	 * Personal bests are kept per boss and team size, in the same form RuneLite's Chat
	 * Commands plugin uses ("theatre of blood 2 players", "chambers of xeric challenge mode solo").
	 */
	public static String personalBestKey(String boss, @Nullable Integer teamSize)
	{
		String key = boss.toLowerCase(Locale.ROOT).replace(":", "");
		if (teamSize == null)
		{
			return key;
		}
		return key + (teamSize == 1 ? " solo" : " " + teamSize + " players");
	}

	public boolean isCompleted(String taskId)
	{
		return completed.containsKey(taskId);
	}

	@Nullable
	public Completion getCompletion(String taskId)
	{
		return completed.get(taskId);
	}

	public void complete(String taskId, long timestamp, boolean fromPersonalBest)
	{
		completed.putIfAbsent(taskId, new Completion(timestamp, fromPersonalBest));
	}

	/**
	 * @return the number of completed tasks that still exist in the task list
	 */
	public int countCompleted(TaskList taskList)
	{
		return (int) taskList.getTasks().stream().filter(t -> isCompleted(t.getId())).count();
	}

	@Nullable
	public Double getPersonalBest(String key)
	{
		return personalBests.get(key);
	}

	public void recordPersonalBest(String key, double seconds)
	{
		if (seconds > 0)
		{
			personalBests.merge(key, seconds, Math::min);
		}
	}
}
