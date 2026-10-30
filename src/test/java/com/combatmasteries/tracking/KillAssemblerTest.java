package com.combatmasteries.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class KillAssemblerTest
{
	private static final KillDuration DURATION = new KillDuration(44.4, 44.4, true, null);

	@Test
	public void killCountThenDuration()
	{
		KillAssembler assembler = new KillAssembler();
		assembler.onKillCount("Zulrah", 10);
		assembler.onDuration(DURATION, 10);

		KillEvent kill = assembler.onTick(10);
		assertNotNull(kill);
		assertEquals("Zulrah", kill.getBoss());
		assertEquals(DURATION, kill.getDuration());
		assertNull(assembler.onTick(11));
	}

	@Test
	public void durationThenKillCount()
	{
		KillAssembler assembler = new KillAssembler();
		assembler.onDuration(DURATION, 10);
		assertNull(assembler.onTick(10));
		assembler.onKillCount("TzKal-Zuk", 11);

		KillEvent kill = assembler.onTick(11);
		assertNotNull(kill);
		assertEquals("TzKal-Zuk", kill.getBoss());
		assertEquals(DURATION, kill.getDuration());
	}

	@Test
	public void bossWithoutDurationIsEmittedAfterGracePeriod()
	{
		KillAssembler assembler = new KillAssembler();
		assembler.onKillCount("Kalphite Queen", 10);
		assertNull(assembler.onTick(10));
		assertNull(assembler.onTick(11));

		KillEvent kill = assembler.onTick(12);
		assertNotNull(kill);
		assertNull(kill.getDuration());
	}

	@Test
	public void strayDurationIsDropped()
	{
		KillAssembler assembler = new KillAssembler();
		assembler.onDuration(DURATION, 10);
		for (int tick = 10; tick < 20; tick++)
		{
			assertNull(assembler.onTick(tick));
		}

		assembler.onKillCount("Zulrah", 20);
		assertNull(assembler.onTick(20));
		assertNull(assembler.onTick(21));
		KillEvent kill = assembler.onTick(22);
		assertNotNull(kill);
		assertNull(kill.getDuration());
	}
}
