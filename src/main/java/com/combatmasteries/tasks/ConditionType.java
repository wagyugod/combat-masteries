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
	 * Tombs of Amascut raid level of at least {@code value}, taken as the highest level during
	 * the raid: missed invocation time limits lower the level shown at the end, so a raid with
	 * every invocation (600) finishes reading 575.
	 */
	MIN_RAID_LEVEL,
	/**
	 * Doom of Mokhaiotl: delve levels 1 to {@code value} cleared in one delve. Read from the
	 * game's record of the deepest level, so it is not tied to a kill. Until it is confirmed
	 * whether that record means reached or cleared, a level only counts as cleared once the
	 * next one has been reached.
	 */
	MIN_DELVE_LEVEL,
	/**
	 * Inferno: all three pillars destroyed before wave 4 begins.
	 */
	INFERNO_NO_PILLARS,
}
