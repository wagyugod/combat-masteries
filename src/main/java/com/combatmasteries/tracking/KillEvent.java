package com.combatmasteries.tracking;

import javax.annotation.Nullable;
import lombok.Value;

/**
 * A completed boss kill, assembled from the kill count message and (if the boss has one)
 * the duration message.
 */
@Value
public class KillEvent
{
	String boss;
	/**
	 * Kill time in seconds, or null if the game did not report one.
	 */
	@Nullable
	KillDuration duration;
}
