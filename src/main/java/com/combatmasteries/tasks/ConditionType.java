package com.combatmasteries.tasks;

/**
 * The building blocks tasks are made from. A task passes when every one of its
 * conditions holds for a single kill.
 */
public enum ConditionType
{
	/**
	 * The game-reported kill time is at most {@code seconds}.
	 */
	MAX_TIME,
	/**
	 * No damage of any kind was taken during the attempt.
	 */
	NO_DAMAGE_TAKEN,
	/**
	 * No prayer was active at any point during the attempt.
	 */
	NO_PRAYER,
	/**
	 * Nothing was eaten during the attempt.
	 */
	NO_FOOD,
	/**
	 * Nothing was drunk during the attempt.
	 */
	NO_POTIONS,
}
