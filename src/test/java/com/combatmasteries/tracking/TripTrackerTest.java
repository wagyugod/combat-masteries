package com.combatmasteries.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Test;

public class TripTrackerTest
{
	@Test
	public void allFourAwakenedBosses()
	{
		TripTracker trip = new TripTracker();
		ImmutableMap<String, Integer> all = ImmutableMap.of(
			"Duke Sucellus (awakened)", 1, "Leviathan (awakened)", 1, "Whisperer (awakened)", 1, "Vardorvis (awakened)", 1);

		trip.onKill("Duke Sucellus (awakened)");
		trip.onKill("Leviathan (awakened)");
		trip.onKill("Whisperer (awakened)");
		assertFalse(trip.satisfies(all));
		trip.onKill("Vardorvis (Awakened)");
		assertTrue(trip.satisfies(all));
	}

	@Test
	public void restockClearsTheTrip()
	{
		TripTracker trip = new TripTracker();
		trip.onKill("Leviathan (awakened)");
		trip.restock("died");
		trip.onKill("Leviathan (awakened)");
		assertFalse(trip.satisfies(ImmutableMap.of("Leviathan (awakened)", 2)));
		assertEquals("died", trip.getLastRestockReason());
	}

	@Test
	public void emptyRequirementNeverPasses()
	{
		assertFalse(new TripTracker().satisfies(Collections.emptyMap()));
	}
}
