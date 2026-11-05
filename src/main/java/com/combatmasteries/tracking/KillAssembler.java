package com.combatmasteries.tracking;

import javax.annotation.Nullable;

/**
 * Pairs kill count messages with the duration messages around them.
 * <p>
 * The kill count and duration messages arrive within a few ticks of each other, in either
 * order, and some bosses send no duration at all. Raids also send a separate total time. A
 * kill is emitted once its kill count, duration and total time are all present, or once a
 * short window has passed.
 */
public class KillAssembler
{
	static final int WINDOW_TICKS = 4;

	private String pendingBoss;
	private int bossTick;
	private KillDuration duration;
	private int durationTick;
	private KillDuration total;
	private int totalTick;

	public void onKillCount(String boss, int tick)
	{
		pendingBoss = boss;
		bossTick = tick;
	}

	public void onDuration(KillDuration d, int tick)
	{
		if (d.isTotal())
		{
			total = d;
			totalTick = tick;
		}
		else
		{
			duration = d;
			durationTick = tick;
		}
	}

	/**
	 * Called once per game tick, after that tick's chat messages.
	 *
	 * @return a completed kill, or null
	 */
	@Nullable
	public KillEvent onTick(int tick)
	{
		if (pendingBoss == null)
		{
			// Durations with no kill count, e.g. raid rooms or Hallowed Sepulchre floors
			if (duration != null && tick - durationTick > WINDOW_TICKS)
			{
				duration = null;
			}
			if (total != null && tick - totalTick > WINDOW_TICKS)
			{
				total = null;
			}
			return null;
		}

		boolean complete = duration != null && (total != null || !isRaid(pendingBoss));
		if (!complete && tick - bossTick < WINDOW_TICKS)
		{
			return null;
		}

		KillEvent kill = new KillEvent(pendingBoss, duration, total, null, null);
		reset();
		return kill;
	}

	public void reset()
	{
		pendingBoss = null;
		duration = null;
		total = null;
	}

	/**
	 * Raids that also print a total completion time worth waiting for.
	 */
	static boolean isRaid(String boss)
	{
		return boss.startsWith("Theatre of Blood") || boss.startsWith("Tombs of Amascut");
	}
}
