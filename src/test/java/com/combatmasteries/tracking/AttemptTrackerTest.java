package com.combatmasteries.tracking;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AttemptTrackerTest
{
	@Test
	public void regionAttemptLastsUntilTheKill()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.enterRegion("TzKal-Zuk", 0);
		Attempt started = tracker.getCurrent();
		started.setPillarsCleared(true);
		tracker.enterRegion("TzKal-Zuk", 50);
		assertSame(started, tracker.getCurrent());

		Attempt done = tracker.complete("TzKal-Zuk");
		assertNotNull(done);
		assertTrue(done.isPillarsCleared());
		assertNull(tracker.getCurrent());
	}

	@Test
	public void killOfAnotherBossDoesNotEndIt()
	{
		AttemptTracker tracker = new AttemptTracker();
		tracker.enterRegion("TzKal-Zuk", 0);
		assertNull(tracker.complete("TzTok-Jad"));
		assertNotNull(tracker.getCurrent());
		tracker.abort();
		assertNull(tracker.getCurrent());
	}
}
