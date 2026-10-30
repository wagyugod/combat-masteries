package com.combatmasteries.tracking;

import javax.annotation.Nullable;
import lombok.Value;

/**
 * A kill time reported by the game, along with the personal best it reported.
 */
@Value
public class KillDuration
{
	double seconds;
	double personalBestSeconds;
	boolean newPersonalBest;
	/**
	 * Team size as reported by raid completion messages, e.g. "Solo" or "3 players".
	 */
	@Nullable
	String teamSize;
}
