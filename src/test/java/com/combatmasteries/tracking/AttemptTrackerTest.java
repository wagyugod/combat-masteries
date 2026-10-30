package com.combatmasteries.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AttemptTrackerTest
{
	@Test
	public void recordsWhatHappenedDuringTheFight()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.engage("Vorkath", 100);
		tracker.onDamageTaken(12, 105);
		tracker.onPrayerActive();
		tracker.onEat();

		Attempt attempt = tracker.complete("Vorkath");
		assertNotNull(attempt);
		assertEquals(12, attempt.getDamageTaken());
		assertTrue(attempt.isPrayed());
		assertTrue(attempt.isAte());
		assertFalse(attempt.isDrank());
		assertNull(tracker.getCurrent());
	}

	@Test
	public void nothingIsRecordedOutsideAnAttempt()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.onDamageTaken(30, 1);
		tracker.onEat();
		tracker.engage("Zulrah", 2);

		Attempt attempt = tracker.complete("Zulrah");
		assertNotNull(attempt);
		assertFalse(attempt.tookDamage());
		assertFalse(attempt.isAte());
	}

	@Test
	public void killOfAnotherBossDoesNotCompleteTheAttempt()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.engage("Zulrah", 1);
		assertNull(tracker.complete("Vorkath"));
		assertNotNull(tracker.getCurrent());
	}

	@Test
	public void engagingAnotherBossStartsAFreshAttempt()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.engage("Zulrah", 1);
		tracker.onDamageTaken(5, 2);
		tracker.engage("Vorkath", 3);

		Attempt attempt = tracker.complete("Vorkath");
		assertNotNull(attempt);
		assertFalse(attempt.tookDamage());
	}

	@Test
	public void npcAttemptsGoIdle()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.engage("Zulrah", 0);
		assertFalse(tracker.checkIdle(AttemptTracker.IDLE_TIMEOUT_TICKS));
		assertTrue(tracker.checkIdle(AttemptTracker.IDLE_TIMEOUT_TICKS + 1));
		assertNull(tracker.getCurrent());
	}

	@Test
	public void regionAttemptsDoNotGoIdleOrRestart()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.enterRegion("TzKal-Zuk", 0);
		Attempt started = tracker.getCurrent();
		tracker.onEat();
		tracker.enterRegion("TzKal-Zuk", 50);
		assertSame(started, tracker.getCurrent());
		assertFalse(tracker.checkIdle(10_000));
		assertTrue(tracker.isRegionAttempt());

		Attempt attempt = tracker.complete("TzKal-Zuk");
		assertNotNull(attempt);
		assertTrue(attempt.isAte());
	}
}
