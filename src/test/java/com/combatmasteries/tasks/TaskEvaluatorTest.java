package com.combatmasteries.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.combatmasteries.tracking.Attempt;
import com.combatmasteries.tracking.KillDuration;
import com.combatmasteries.tracking.KillEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TaskEvaluatorTest
{
	private TaskList taskList;
	private MasteryProgress progress;

	@Before
	public void setUp()
	{
		taskList = new TaskList(
			Arrays.asList(
				new MasteryTask("speed", "Speed", "", "Zulrah",
					Collections.singletonList(new TaskCondition(ConditionType.MAX_TIME, 45)), 1),
				new MasteryTask("flawless_speed", "Flawless speed", "", "Zulrah",
					Arrays.asList(new TaskCondition(ConditionType.MAX_TIME, 60), new TaskCondition(ConditionType.NO_DAMAGE_TAKEN, 0)), 1),
				new MasteryTask("streak", "Streak", "", "Vorkath",
					Collections.singletonList(new TaskCondition(ConditionType.NO_DAMAGE_TAKEN, 0)), 3)
			),
			Arrays.asList(
				new BossDefinition("Zulrah", Collections.singletonList("Zulrah"), Collections.emptyList()),
				new BossDefinition("Vorkath", Collections.singletonList("Vorkath"), Collections.emptyList())
			));
		progress = new MasteryProgress();
	}

	private static KillEvent kill(String boss, double seconds)
	{
		return new KillEvent(boss, new KillDuration(seconds, seconds, false, null));
	}

	private static Attempt clean(String boss)
	{
		return Attempt.of(boss, 0, false, false, false);
	}

	private static Attempt hit(String boss)
	{
		return Attempt.of(boss, 7, false, false, false);
	}

	@Test
	public void timeLimitIsInclusive()
	{
		List<MasteryTask> done = TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 45.0), hit("Zulrah"), 1);
		assertEquals(1, done.size());
		assertEquals("speed", done.get(0).getId());
		assertTrue(progress.isCompleted("speed"));
	}

	@Test
	public void oneTickOverFails()
	{
		assertTrue(TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 45.6), hit("Zulrah"), 1).isEmpty());
		assertFalse(progress.isCompleted("speed"));
	}

	@Test
	public void restrictionsNeedAWatchedAttempt()
	{
		// Fast enough, but the plugin never saw the fight start
		TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 50), null, 1);
		assertFalse(progress.isCompleted("flawless_speed"));

		TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 50), clean("Zulrah"), 2);
		assertTrue(progress.isCompleted("flawless_speed"));
	}

	@Test
	public void killWithoutDurationFailsTimeConditions()
	{
		TaskEvaluator.onKill(taskList, progress, new KillEvent("Zulrah", null), clean("Zulrah"), 1);
		assertFalse(progress.isCompleted("speed"));
	}

	@Test
	public void streakNeedsConsecutiveQualifyingKills()
	{
		TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), clean("Vorkath"), 1);
		TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), clean("Vorkath"), 2);
		assertEquals(2, progress.getStreak("streak"));

		TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), hit("Vorkath"), 3);
		assertEquals(0, progress.getStreak("streak"));

		for (int i = 0; i < 2; i++)
		{
			TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), clean("Vorkath"), 4);
		}
		assertFalse(progress.isCompleted("streak"));
		List<MasteryTask> done = TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), clean("Vorkath"), 5);
		assertEquals(1, done.size());
		assertTrue(progress.isCompleted("streak"));
	}

	@Test
	public void deathBreaksStreak()
	{
		TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), clean("Vorkath"), 1);
		TaskEvaluator.onAttemptFailed(taskList, progress, "Vorkath");
		assertEquals(0, progress.getStreak("streak"));
	}

	@Test
	public void killsOfOtherBossesAreIgnored()
	{
		TaskEvaluator.onKill(taskList, progress, kill("Vorkath", 90), clean("Vorkath"), 1);
		TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 30), hit("Zulrah"), 2);
		assertEquals(1, progress.getStreak("streak"));
	}

	@Test
	public void personalBestsCreditOnlyTimeTasks()
	{
		List<MasteryTask> done = TaskEvaluator.creditPersonalBests(taskList, progress, boss -> boss.equals("Zulrah") ? 40.2 : null, 1);
		assertEquals(1, done.size());
		assertTrue(progress.isCompleted("speed"));
		assertTrue(progress.getCompletion("speed").isFromPersonalBest());
		// Needs a watched no-damage kill, so a PB is not enough
		assertFalse(progress.isCompleted("flawless_speed"));
	}

	@Test
	public void slowPersonalBestCreditsNothing()
	{
		assertTrue(TaskEvaluator.creditPersonalBests(taskList, progress, boss -> 46.0, 1).isEmpty());
	}

	@Test
	public void completionsAreNotRepeated()
	{
		TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 40), hit("Zulrah"), 1);
		assertTrue(TaskEvaluator.onKill(taskList, progress, kill("Zulrah", 40), hit("Zulrah"), 2).isEmpty());
		assertEquals(1, progress.getCompletion("speed").getTimestamp());
	}
}
