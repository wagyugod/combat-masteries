package com.combatmasteries.tasks;

import static com.combatmasteries.tasks.Tasks.task;
import static com.combatmasteries.tasks.Tasks.trip;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.combatmasteries.tracking.Attempt;
import com.combatmasteries.tracking.KillDuration;
import com.combatmasteries.tracking.KillEvent;
import com.combatmasteries.tracking.TripTracker;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TaskEvaluatorTest
{
	private static final String TOB = "Theatre of Blood";
	private static final String TOA = "Tombs of Amascut: Expert Mode";
	private static final String LEVI = "Leviathan (awakened)";

	private TaskList taskList;
	private MasteryProgress progress;
	private TripTracker trip;

	@Before
	public void setUp()
	{
		MasteryTask untracked = task("radiant", "Yama", TaskCategory.CHALLENGE);
		untracked.setTracked(false);
		taskList = new TaskList(
			Arrays.asList(
				task("inferno_sub_50", "TzKal-Zuk", TaskCategory.SPEED, TaskCondition.underTime(50 * 60)),
				task("inferno_no_pillar", "TzKal-Zuk", TaskCategory.CHALLENGE, TaskCondition.of(ConditionType.INFERNO_NO_PILLARS, 0)),
				task("tob_duo_sub_24", TOB, TaskCategory.SPEED, TaskCondition.of(ConditionType.TEAM_SIZE, 2), TaskCondition.underTime(24 * 60)),
				task("toa_solo_600", TOA, TaskCategory.CHALLENGE, TaskCondition.of(ConditionType.TEAM_SIZE, 1), TaskCondition.of(ConditionType.MIN_RAID_LEVEL, 600)),
				task("toa_solo_300_sub_20", TOA, TaskCategory.SPEED, TaskCondition.of(ConditionType.TEAM_SIZE, 1), TaskCondition.underTime(20 * 60)),
				task("delve_24", null, TaskCategory.CHALLENGE, TaskCondition.of(ConditionType.MIN_DELVE_LEVEL, 24)),
				trip("levi_x2", ImmutableMap.of(LEVI, 2)),
				untracked
			),
			Arrays.asList(
				new BossDefinition("TzKal-Zuk", Collections.singletonList(9043), false),
				new BossDefinition(TOA, Collections.emptyList(), true)
			));
		progress = new MasteryProgress();
		trip = new TripTracker();
	}

	private static KillDuration time(double seconds)
	{
		return new KillDuration(seconds, seconds, false, null, false);
	}

	private static KillDuration total(double seconds)
	{
		return new KillDuration(seconds, seconds, false, null, true);
	}

	private List<MasteryTask> kill(KillEvent kill, Attempt attempt)
	{
		trip.onKill(kill.getBoss());
		return TaskEvaluator.onKill(taskList, progress, kill, attempt, trip, 1);
	}

	@Test
	public void subTimesAreStrict()
	{
		assertTrue(kill(new KillEvent("TzKal-Zuk", time(50 * 60)), null).isEmpty());
		assertFalse(progress.isCompleted("inferno_sub_50"));

		kill(new KillEvent("TzKal-Zuk", time(50 * 60 - 0.6)), null);
		assertTrue(progress.isCompleted("inferno_sub_50"));
	}

	@Test
	public void bossNamesIgnoreCase()
	{
		kill(new KillEvent("tzkal-zuk", time(44 * 60 + 4)), null);
		assertTrue(progress.isCompleted("inferno_sub_50"));
		assertTrue(taskList.usesTotalTime("tombs of amascut: expert mode"));
	}

	@Test
	public void noPillarNeedsTheWatchedRunToClearThem()
	{
		Attempt run = new Attempt("TzKal-Zuk", 0);
		kill(new KillEvent("TzKal-Zuk", time(3600)), run);
		assertFalse(progress.isCompleted("inferno_no_pillar"));

		run.setPillarsCleared(true);
		kill(new KillEvent("TzKal-Zuk", time(3600)), run);
		assertTrue(progress.isCompleted("inferno_no_pillar"));
	}

	@Test
	public void teamSizeMustMatchExactly()
	{
		kill(new KillEvent(TOB, time(20 * 60), null, 3, null), null);
		assertFalse(progress.isCompleted("tob_duo_sub_24"));
		kill(new KillEvent(TOB, time(23 * 60), null, null, null), null);
		assertFalse(progress.isCompleted("tob_duo_sub_24"));

		kill(new KillEvent(TOB, time(23 * 60), null, 2, null), null);
		assertTrue(progress.isCompleted("tob_duo_sub_24"));
	}

	@Test
	public void raidLevelIsAMinimum()
	{
		kill(new KillEvent(TOA, time(30 * 60), total(35 * 60), 1, 575), null);
		assertFalse(progress.isCompleted("toa_solo_600"));
		kill(new KillEvent(TOA, time(30 * 60), total(35 * 60), 1, 610), null);
		assertTrue(progress.isCompleted("toa_solo_600"));
	}

	@Test
	public void tombsOfAmascutIsTimedOnTotalTime()
	{
		// Fast challenge time, but the total including downtime is too slow
		kill(new KillEvent(TOA, time(18 * 60), total(21 * 60), 1, 300), null);
		assertFalse(progress.isCompleted("toa_solo_300_sub_20"));

		kill(new KillEvent(TOA, time(18 * 60), total(19 * 60), 1, 300), null);
		assertTrue(progress.isCompleted("toa_solo_300_sub_20"));
	}

	@Test
	public void tripTasksNeedTheKillsOnOneInventory()
	{
		kill(new KillEvent(LEVI, time(120)), null);
		trip.restock("opened a bank");
		kill(new KillEvent(LEVI, time(120)), null);
		assertFalse(progress.isCompleted("levi_x2"));

		kill(new KillEvent("Vardorvis (awakened)", time(120)), null);
		List<MasteryTask> done = kill(new KillEvent("LEVIATHAN (Awakened)", time(120)), null);
		assertEquals(1, done.size());
		assertTrue(progress.isCompleted("levi_x2"));
	}

	@Test
	public void deepestDelveCompletesFromTheGameRecord()
	{
		// Level 24 only counts as cleared once level 25 is on record
		assertTrue(TaskEvaluator.onDeepestDelve(taskList, progress, 24, 1).isEmpty());
		assertEquals(1, TaskEvaluator.onDeepestDelve(taskList, progress, 25, 1).size());
		assertTrue(progress.getCompletion("delve_24").isFromPersonalBest());
	}

	@Test
	public void personalBestsCreditMatchingTeamSize()
	{
		List<MasteryTask> done = TaskEvaluator.creditPersonalBests(taskList, progress, (boss, team) ->
		{
			if (boss.equals("TzKal-Zuk") && team == null)
			{
				return 49 * 60.0;
			}
			if (boss.equals(TOB) && team != null && team == 2)
			{
				return 23 * 60.0;
			}
			return null;
		}, 1);
		assertEquals(2, done.size());
		assertTrue(progress.isCompleted("inferno_sub_50"));
		assertTrue(progress.isCompleted("tob_duo_sub_24"));
		// Raid level and pillars cannot come from a PB
		assertFalse(progress.isCompleted("toa_solo_600"));
		assertFalse(progress.isCompleted("inferno_no_pillar"));
	}

	@Test
	public void untrackedTasksCountButNeverComplete()
	{
		assertEquals(8, taskList.size());
		kill(new KillEvent("Yama", time(60)), null);
		assertFalse(progress.isCompleted("radiant"));
	}

	@Test
	public void personalBestKeysMatchChatCommands()
	{
		assertEquals("theatre of blood hard mode solo", MasteryProgress.personalBestKey("Theatre of Blood: Hard Mode", 1));
		assertEquals("theatre of blood 2 players", MasteryProgress.personalBestKey(TOB, 2));
		assertEquals("tzkal-zuk", MasteryProgress.personalBestKey("TzKal-Zuk", null));
	}
}
