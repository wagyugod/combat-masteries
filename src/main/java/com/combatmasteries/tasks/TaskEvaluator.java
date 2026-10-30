package com.combatmasteries.tasks;

import com.combatmasteries.tracking.Attempt;
import com.combatmasteries.tracking.KillEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Decides which tasks a kill completes. Holds no state of its own: streaks and
 * completions are written to the given {@link MasteryProgress}.
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
	 * @param attempt what was observed during the fight, or null if the plugin did not see
	 *                the fight start (then only time conditions can pass)
	 * @return tasks newly completed by this kill
	 */
	public static List<MasteryTask> onKill(TaskList taskList, MasteryProgress progress, KillEvent kill, @Nullable Attempt attempt, long now)
	{
		List<MasteryTask> completed = new ArrayList<>();
		for (MasteryTask task : taskList.getTasks())
		{
			if (!task.getBoss().equals(kill.getBoss()) || progress.isCompleted(task.getId()))
			{
				continue;
			}

			boolean passed = task.getConditions().stream().allMatch(c -> holds(c, kill, attempt));
			if (task.getConsecutive() > 1)
			{
				int streak = passed ? progress.getStreak(task.getId()) + 1 : 0;
				if (streak >= task.getConsecutive())
				{
					progress.complete(task.getId(), now, false);
					completed.add(task);
				}
				else
				{
					progress.setStreak(task.getId(), streak);
				}
			}
			else if (passed)
			{
				progress.complete(task.getId(), now, false);
				completed.add(task);
			}
		}
		return completed;
	}

	/**
	 * A death or abandoned attempt at a boss breaks every streak for it.
	 */
	public static void onAttemptFailed(TaskList taskList, MasteryProgress progress, String boss)
	{
		for (MasteryTask task : taskList.getTasks())
		{
			if (task.getBoss().equals(boss))
			{
				progress.setStreak(task.getId(), 0);
			}
		}
	}

	/**
	 * Credits time-only tasks from personal bests set before the plugin was watching.
	 *
	 * @param personalBest boss name to best time in seconds, or null if unknown
	 * @return tasks newly completed
	 */
	public static List<MasteryTask> creditPersonalBests(TaskList taskList, MasteryProgress progress, Function<String, Double> personalBest, long now)
	{
		List<MasteryTask> completed = new ArrayList<>();
		for (MasteryTask task : taskList.getTasks())
		{
			if (progress.isCompleted(task.getId()) || !task.isRetroCreditable())
			{
				continue;
			}

			Double pb = personalBest.apply(task.getBoss());
			if (pb == null || pb <= 0)
			{
				continue;
			}

			boolean passed = task.getConditions().stream().allMatch(c -> pb <= c.getSeconds() + EPSILON);
			if (passed)
			{
				progress.complete(task.getId(), now, true);
				completed.add(task);
			}
		}
		return completed;
	}

	static boolean holds(TaskCondition condition, KillEvent kill, @Nullable Attempt attempt)
	{
		switch (condition.getType())
		{
			case MAX_TIME:
				return kill.getDuration() != null && kill.getDuration().getSeconds() <= condition.getSeconds() + EPSILON;
			case NO_DAMAGE_TAKEN:
				return attempt != null && !attempt.tookDamage();
			case NO_PRAYER:
				return attempt != null && !attempt.isPrayed();
			case NO_FOOD:
				return attempt != null && !attempt.isAte();
			case NO_POTIONS:
				return attempt != null && !attempt.isDrank();
			default:
				return false;
		}
	}
}
