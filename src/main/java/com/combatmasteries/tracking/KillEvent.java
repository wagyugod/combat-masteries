package com.combatmasteries.tracking;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/**
 * A completed boss kill or raid, assembled from the kill count message, the duration
 * messages, and (for raids) the team size and raid level read when it completed.
 */
@Value
@With
@AllArgsConstructor
public class KillEvent
{
	String boss;
	/**
	 * The kill or challenge time, or null if the game did not report one.
	 */
	@Nullable
	KillDuration duration;
	/**
	 * The raid's total completion time (including downtime), or null.
	 */
	@Nullable
	KillDuration totalDuration;
	@Nullable
	Integer teamSize;
	/**
	 * Tombs of Amascut raid level, or null.
	 */
	@Nullable
	Integer raidLevel;

	public KillEvent(String boss, @Nullable KillDuration duration)
	{
		this(boss, duration, null, null, null);
	}
}
