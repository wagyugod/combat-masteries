package com.combatmasteries.tasks;

import com.combatmasteries.tracking.Attempt;
import com.combatmasteries.tracking.KillDuration;
import com.combatmasteries.tracking.KillEvent;
import com.combatmasteries.tracking.TripTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/**
 * Decides which tasks a kill completes. Holds no state of its own: completions are written
 * to the given {@link MasteryProgress}.
 */
public final class TaskEvaluator
{
	/**
	 * Game times are multiples of 0.6s printed to two decimals, so allow for float error.
	 */
	private static final double EPSILON = 1e-6;

	private TaskEvaluator()
	{
	}

	/**
	 * @param attempt the region attempt that ended in this kill, or null
	 * @param trip    kills made on the current inventory, including this one
	 * @return tasks newly completed by this kill
	 */
	public static List<MasteryTask> onKill(TaskList taskList, MasteryProgress progress, KillEvent kill,
		@Nullable Attempt attempt, TripTracker trip, long now)
	{
		List<MasteryTask> completed = new ArrayList<>();
		for (MasteryTask task : taskList.getTasks())
		{
			if (!task.isTracked() || progress.isCompleted(task.getId()))
			{
				continue;
			}

			boolean passed;
			if (task.isTrip())
			{
				passed = trip.satisfies(task.getTrip());
			}
			else
			{
				passed = kill.getBoss().equals(task.getBoss())
					&& task.getConditions().stream().allMatch(c -> holds(c, kill, attempt, taskList.usesTotalTime(kill.getBoss())));
			}

			if (passed)
			{
				progress.complete(task.getId(), now, false);
				completed.add(task);
			}
		}
		return completed;
	}

	/**
	 * Completes delve tasks from the game's record of the deepest Doom of Mokhaiotl level.
	 * Deliberately conservative: level N counts as cleared once level N + 1 is on record, so
	 * a task is never awarded for reaching a level without clearing it.
	 */
	public static List<MasteryTask> onDeepestDelve(TaskList taskList, MasteryProgress progress, int deepest, long now)
	{
		List<MasteryTask> completed = new ArrayList<>();
		for (MasteryTask task : taskList.getTasks())
		{
			if (!task.isTracked() || progress.isCompleted(task.getId()) || !task.hasCondition(ConditionType.MIN_DELVE_LEVEL))
			{
				continue;
			}

			boolean passed = task.getConditions().stream()
				.filter(c -> c.getType() == ConditionType.MIN_DELVE_LEVEL)
				.allMatch(c -> deepest > c.getValue());
			if (passed)
			{
				progress.complete(task.getId(), now, true);
				completed.add(task);
			}
		}
		return completed;
	}

	/**
	 * Credits time tasks from personal bests set before the plugin was watching.
	 *
	 * @param personalBest looks up a personal best in seconds by (boss, team size), or null
	 * @return tasks newly completed
	 */
	public static List<MasteryTask> creditPersonalBests(TaskList taskList, MasteryProgress progress,
		BiFunction<String, Integer, Double> personalBest, long now)
	{
		List<MasteryTask> completed = new ArrayList<>();
		for (MasteryTask task : taskList.getTasks())
		{
			if (progress.isCompleted(task.getId()) || !task.isPersonalBestCreditable())
			{
				continue;
			}

			Integer teamSize = task.getConditions().stream()
				.filter(c -> c.getType() == ConditionType.TEAM_SIZE)
				.map(TaskCondition::getValue)
				.findFirst().orElse(null);
			Double pb = personalBest.apply(task.getBoss(), teamSize);
			if (pb == null || pb <= 0)
			{
				continue;
			}

			boolean passed = task.getConditions().stream()
				.filter(c -> c.getType() == ConditionType.UNDER_TIME)
				.allMatch(c -> pb < c.getSeconds() - EPSILON);
			if (passed)
			{
				progress.complete(task.getId(), now, true);
				completed.add(task);
			}
		}
		return completed;
	}

	static boolean holds(TaskCondition condition, KillEvent kill, @Nullable Attempt attempt, boolean totalTime)
	{
		switch (condition.getType())
		{
			case UNDER_TIME:
				KillDuration time = totalTime ? kill.getTotalDuration() : kill.getDuration();
				return time != null && time.getSeconds() < condition.getSeconds() - EPSILON;
			case TEAM_SIZE:
				return kill.getTeamSize() != null && kill.getTeamSize() == condition.getValue();
			case MIN_RAID_LEVEL:
				return kill.getRaidLevel() != null && kill.getRaidLevel() >= condition.getValue();
			case INFERNO_NO_PILLARS:
				return attempt != null && attempt.isPillarsCleared();
			case MIN_DELVE_LEVEL:
				// Judged from the game's record in onDeepestDelve, never from a kill
			default:
				return false;
		}
	}
}
