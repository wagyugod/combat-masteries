package com.combatmasteries.tracking;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * One run of a region-based activity such as the Inferno, from entering to the kill.
 */
@Getter
@ToString
public class Attempt
{
	private final String boss;
	private final int startTick;
	/**
	 * Inferno: true if all three pillars were seen and none was standing when wave 4 began.
	 */
	@Setter
	private boolean pillarsCleared;

	public Attempt(String boss, int startTick)
	{
		this.boss = boss;
		this.startTick = startTick;
	}
}
