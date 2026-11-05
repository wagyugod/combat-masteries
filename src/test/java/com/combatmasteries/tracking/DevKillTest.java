package com.combatmasteries.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DevKillTest
{
	private static DevKill parse(String command)
	{
		return DevKill.parse(command.split(" "));
	}

	@Test
	public void plainNamesAndTeamWords()
	{
		DevKill kill = parse("Solo ToB 44:44");
		assertEquals("Theatre of Blood", kill.getBoss());
		assertEquals(Integer.valueOf(1), kill.getTeamSize());
		assertEquals(44 * 60 + 44, kill.getSeconds(), 1e-9);

		assertEquals(Integer.valueOf(2), parse("duo tob 23:00").getTeamSize());
		assertEquals("Theatre of Blood: Hard Mode", parse("solo hmt 50:00").getBoss());
		assertEquals("TzKal-Zuk", parse("inferno 44:59").getBoss());
		assertEquals("Sol Heredit", parse("colo 13:59").getBoss());
	}

	@Test
	public void exactNamesStillWork()
	{
		assertEquals("TzKal-Zuk", parse("TzKal-Zuk 44:05").getBoss());
		assertEquals("Tombs of Amascut: Expert Mode", parse("Tombs_of_Amascut:_Expert_Mode 30:00 team=1").getBoss());
	}

	@Test
	public void options()
	{
		DevKill kill = parse("toa 30:00 solo level=600 total=33:00");
		assertEquals("Tombs of Amascut: Expert Mode", kill.getBoss());
		assertEquals(Integer.valueOf(600), kill.getRaidLevel());
		assertEquals(33 * 60, kill.getTotalSeconds(), 1e-9);
		assertTrue(parse("inferno 55:00 pillars").isPillarsCleared());
	}

	@Test
	public void tripKillsNeedNoTime()
	{
		DevKill kill = parse("awakened levi");
		assertEquals("Leviathan (awakened)", kill.getBoss());
		assertNull(kill.getSeconds());
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingBoss()
	{
		parse("44:44");
	}

	@Test(expected = IllegalArgumentException.class)
	public void junkAfterTheTime()
	{
		parse("tob 44:44 banana");
	}
}
