package com.combatmasteries.tasks;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
	private Map<String, Integer> streaks = new HashMap<>();
	/**
	 * Personal bests in seconds as reported by the game, keyed by boss name.
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
		 * True when credited from a personal best set before the attempt was watched.
		 */
		private boolean fromPersonalBest;
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
		streaks.remove(taskId);
	}

	/**
	 * @return the number of completed tasks that still exist in the task list
	 */
	public int countCompleted(TaskList taskList)
	{
		return (int) taskList.getTasks().stream().filter(t -> isCompleted(t.getId())).count();
	}

	public int getStreak(String taskId)
	{
		return streaks.getOrDefault(taskId, 0);
	}

	void setStreak(String taskId, int streak)
	{
		if (streak <= 0)
		{
			streaks.remove(taskId);
		}
		else
		{
			streaks.put(taskId, streak);
		}
	}

	@Nullable
	public Double getPersonalBest(String boss)
	{
		return personalBests.get(boss);
	}

	public void recordPersonalBest(String boss, double seconds)
	{
		personalBests.merge(boss, seconds, Math::min);
	}
}
