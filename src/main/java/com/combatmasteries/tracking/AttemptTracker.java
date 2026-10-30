package com.combatmasteries.tracking;

import javax.annotation.Nullable;
import lombok.Getter;

/**
 * Tracks the attempt in progress. Only one boss is tracked at a time; engaging a
 * different boss abandons the current attempt.
 * <p>
 * Engagement is detected by the plugin (attacking the boss, being in its region) and
 * reported here. This class holds no RuneLite types so it can be unit tested.
 */
public class AttemptTracker
{
	/**
	 * An NPC-engaged attempt is abandoned after this many ticks without combat (60s).
	 */
	static final int IDLE_TIMEOUT_TICKS = 100;

	@Getter
	@Nullable
	private Attempt current;
	/**
	 * True when the current attempt was started by entering a boss region (Inferno,
	 * Fight Caves) rather than by attacking an NPC. Region attempts never idle out; they
	 * end when the player leaves the region.
	 */
	private boolean regionAttempt;

	/**
	 * Notes combat with a boss, starting a new attempt if one is not running.
	 */
	public void engage(String boss, int tick)
	{
		if (current == null || !current.getBoss().equals(boss))
		{
			current = new Attempt(boss, tick);
			regionAttempt = false;
		}
		current.lastActivityTick = tick;
	}

	/**
	 * Starts a region attempt if one for this boss is not already running.
	 */
	public void enterRegion(String boss, int tick)
	{
		if (current == null || !current.getBoss().equals(boss) || !regionAttempt)
		{
			current = new Attempt(boss, tick);
			regionAttempt = true;
		}
	}

	public boolean isRegionAttempt()
	{
		return current != null && regionAttempt;
	}

	public void onDamageTaken(int amount, int tick)
	{
		if (current != null && amount > 0)
		{
			current.damageTaken += amount;
			current.lastActivityTick = tick;
		}
	}

	public void onPrayerActive()
	{
		if (current != null)
		{
			current.prayed = true;
		}
	}

	public void onEat()
	{
		if (current != null)
		{
			current.ate = true;
		}
	}

	public void onDrink()
	{
		if (current != null)
		{
			current.drank = true;
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
		if (current == null || !current.getBoss().equals(boss))
		{
			return null;
		}
		Attempt done = current;
		current = null;
		return done;
	}

	/**
	 * Abandons the current attempt (death, logout, left the region).
	 *
	 * @return the abandoned attempt, or null if none was running
	 */
	@Nullable
	public Attempt abort()
	{
		Attempt aborted = current;
		current = null;
		return aborted;
	}

	/**
	 * @return true if an NPC-engaged attempt has gone idle and was dropped
	 */
	public boolean checkIdle(int tick)
	{
		if (current != null && !regionAttempt && tick - current.lastActivityTick > IDLE_TIMEOUT_TICKS)
		{
			current = null;
			return true;
		}
		return false;
	}
}
