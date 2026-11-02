package com.combatmasteries.tasks;

/**
 * The building blocks tasks are made from. A task passes when every one of its
 * conditions holds for a single kill or raid.
 */
public enum ConditionType
{
	/**
	 * Finished in strictly less than {@code seconds} ("sub 40:00").
	 */
	UNDER_TIME,
	/**
	 * Exactly {@code value} players in the raid (1 = solo).
	 */
	TEAM_SIZE,
	/**
	 * Tombs of Amascut raid level of at least {@code value}.
	 */
	MIN_RAID_LEVEL,
	/**
	 * Doom of Mokhaiotl: deepest delve level of at least {@code value}. Read from the game's
	 * own record, so it is not tied to a kill.
	 */
	MIN_DELVE_LEVEL,
	/**
	 * Inferno: all three pillars destroyed before wave 4 begins.
	 */
	INFERNO_NO_PILLARS,
}
