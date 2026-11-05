package com.combatmasteries.tracking;

import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Tracks the run in progress for region-based content (the Inferno). An attempt starts on
 * entering the region and ends at the kill, on death, or on leaving.
 * <p>
 * Holds no RuneLite types so it can be unit tested.
 */
public class AttemptTracker
{
	@Getter
	@Nullable
	private Attempt current;

	/**
	 * Starts an attempt if one for this boss is not already running.
	 */
	public void enterRegion(String boss, int tick)
	{
		if (current == null || !current.getBoss().equals(boss))
		{
			current = new Attempt(boss, tick);
		}
	}

	/**
	 * Ends the attempt for a boss that was just killed.
	 *
	 * @return the attempt, or null if no attempt at this boss was being tracked
	 */
	@Nullable
	public Attempt complete(String boss)
	{
		if (current == null || !current.getBoss().equalsIgnoreCase(boss))
		{
			return null;
		}
		Attempt done = current;
		current = null;
		return done;
	}

	/**
	 * Abandons the current attempt (death, logout, left the region).
	 */
	public void abort()
	{
		current = null;
	}
}
