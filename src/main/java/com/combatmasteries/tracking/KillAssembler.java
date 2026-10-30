package com.combatmasteries.tracking;

import javax.annotation.Nullable;

/**
 * Pairs kill count messages with kill duration messages.
 * <p>
 * Most bosses send the kill count first and the duration second, but some send them the
 * other way round, and some send no duration at all. Both messages arrive within a tick
 * of each other, so a kill is emitted once both halves are present, or once a short grace
 * period has passed without a duration.
 */
public class KillAssembler
{
	static final int GRACE_TICKS = 2;

	private String pendingBoss;
	private int bossTick;
	private KillDuration pendingDuration;
	private int durationTick;

	public void onKillCount(String boss, int tick)
	{
		pendingBoss = boss;
		bossTick = tick;
	}

	public void onDuration(KillDuration duration, int tick)
	{
		pendingDuration = duration;
		durationTick = tick;
	}

	/**
	 * Called once per game tick, after that tick's chat messages.
	 *
	 * @return a completed kill, or null
	 */
	@Nullable
	public KillEvent onTick(int tick)
	{
		if (pendingBoss != null && (pendingDuration != null || tick - bossTick >= GRACE_TICKS))
		{
			KillEvent kill = new KillEvent(pendingBoss, pendingDuration);
			reset();
			return kill;
		}

		if (pendingBoss == null && pendingDuration != null && tick - durationTick > GRACE_TICKS)
		{
			// A duration with no kill count, e.g. a raid room or a Hallowed Sepulchre floor
			pendingDuration = null;
		}
		return null;
	}

	public void reset()
	{
		pendingBoss = null;
		pendingDuration = null;
	}
}
