package com.combatmasteries.tracking;

import lombok.Getter;
import lombok.ToString;

/**
 * Everything observed during one attempt at a boss, from engagement to kill.
 */
@Getter
@ToString
public class Attempt
{
	private final String boss;
	private final int startTick;
	int lastActivityTick;
	int damageTaken;
	boolean prayed;
	boolean ate;
	boolean drank;

	Attempt(String boss, int startTick)
	{
		this.boss = boss;
		this.startTick = startTick;
		this.lastActivityTick = startTick;
	}

	/**
	 * Builds a finished attempt directly, for tests and developer-mode commands.
	 */
	public static Attempt of(String boss, int damageTaken, boolean prayed, boolean ate, boolean drank)
	{
		Attempt attempt = new Attempt(boss, 0);
		attempt.damageTaken = damageTaken;
		attempt.prayed = prayed;
		attempt.ate = ate;
		attempt.drank = drank;
		return attempt;
	}

	public boolean tookDamage()
	{
		return damageTaken > 0;
	}
}
