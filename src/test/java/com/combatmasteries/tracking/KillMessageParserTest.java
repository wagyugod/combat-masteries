package com.combatmasteries.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Message samples are taken from RuneLite's ChatCommandsPluginTest.
 */
public class KillMessageParserTest
{
	private static final double DELTA = 1e-9;

	@Test
	public void killCount()
	{
		assertEquals("Zulrah", KillMessageParser.parseKillCount("Your Zulrah kill count is: <col=ff0000>4</col>."));
		assertEquals("TzKal-Zuk", KillMessageParser.parseKillCount("Your TzKal-Zuk kill count is: <col=ff0000>2</col>."));
		assertEquals("Kree'arra", KillMessageParser.parseKillCount("Your Kree'arra kill count is: <col=ff0000>4</col>."));
		assertEquals("Nightmare", KillMessageParser.parseKillCount("Your Nightmare kill count is: <col=ff0000>1,130</col>"));
		assertEquals("Corrupted Gauntlet", KillMessageParser.parseKillCount("Your Corrupted Gauntlet completion count is: <col=ff0000>4729</col>."));
		assertEquals("Corrupted Hunllef (Echo)", KillMessageParser.parseKillCount("Your <col=a53fff>Corrupted Hunllef (Echo)</col> kill count is: <col=ff3045>31</col>"));
	}

	@Test
	public void notKillCount()
	{
		assertNull(KillMessageParser.parseKillCount("Fight duration: <col=ff0000>1:36</col> (new personal best)"));
		assertNull(KillMessageParser.parseKillCount("Welcome to Old School RuneScape."));
	}

	@Test
	public void newPersonalBest()
	{
		KillDuration d = KillMessageParser.parseDuration("Fight duration: <col=ff0000>1:36.40</col> (new personal best)");
		assertNotNull(d);
		assertEquals(96.4, d.getSeconds(), DELTA);
		assertEquals(96.4, d.getPersonalBestSeconds(), DELTA);
		assertTrue(d.isNewPersonalBest());
	}

	@Test
	public void personalBestNotBeaten()
	{
		KillDuration d = KillMessageParser.parseDuration("Fight duration: <col=ff0000>0:59.20</col>. Personal best: 0:55.40");
		assertNotNull(d);
		assertEquals(59.2, d.getSeconds(), DELTA);
		assertEquals(55.4, d.getPersonalBestSeconds(), DELTA);
		assertFalse(d.isNewPersonalBest());
	}

	@Test
	public void impreciseTimes()
	{
		KillDuration d = KillMessageParser.parseDuration("Fight duration: <col=ff0000>2:06</col>. Personal best: 1:19.");
		assertNotNull(d);
		assertEquals(126, d.getSeconds(), DELTA);
		assertEquals(79, d.getPersonalBestSeconds(), DELTA);
	}

	@Test
	public void infernoMinutesOverAnHour()
	{
		KillDuration d = KillMessageParser.parseDuration("Duration: <col=ff0000>104:31.20</col> (new personal best)");
		assertNotNull(d);
		assertEquals(104 * 60 + 31.2, d.getSeconds(), DELTA);

		d = KillMessageParser.parseDuration("Duration: <col=ff0000>172:18.40</col>. Personal best: 134:52.20");
		assertNotNull(d);
		assertEquals(172 * 60 + 18.4, d.getSeconds(), DELTA);
		assertEquals(134 * 60 + 52.2, d.getPersonalBestSeconds(), DELTA);
	}

	@Test
	public void gauntletAndNightmareFormats()
	{
		KillDuration d = KillMessageParser.parseDuration("Challenge duration: <col=ff0000>10:24.40</col> (new personal best).");
		assertNotNull(d);
		assertEquals(624.4, d.getSeconds(), DELTA);

		d = KillMessageParser.parseDuration("Subdued in <col=ef1020>6:19.80</col>. Personal best: 5:42.60.");
		assertNotNull(d);
		assertEquals(379.8, d.getSeconds(), DELTA);
		assertEquals(342.6, d.getPersonalBestSeconds(), DELTA);
	}

	@Test
	public void raids()
	{
		KillDuration d = KillMessageParser.parseDuration("<col=ef20ff>Congratulations - your raid is complete!</col><br>Team size: <col=ff0000>3 players</col> Duration:</col> <col=ff0000>41:10</col> Personal best: </col><col=ff0000>40:03</col>");
		assertNotNull(d);
		assertEquals(2470, d.getSeconds(), DELTA);
		assertEquals(2403, d.getPersonalBestSeconds(), DELTA);
		assertEquals("3 players", d.getTeamSize());

		d = KillMessageParser.parseDuration("Theatre of Blood completion time: <col=ff0000>17:00.20</col> (new personal best)");
		assertNotNull(d);
		assertEquals(1020.2, d.getSeconds(), DELTA);
	}

	@Test
	public void tombsOfAmascutUsesRaidTimeNotRoomTime()
	{
		KillDuration d = KillMessageParser.parseDuration("Challenge complete: The Wardens. Duration: <col=ef1020>9:40</col><br>Tombs of Amascut: Expert Mode challenge completion time: <col=ef1020>9:40</col>. Personal best: 8:31");
		assertNotNull(d);
		assertEquals(580, d.getSeconds(), DELTA);
		assertEquals(511, d.getPersonalBestSeconds(), DELTA);
	}

	@Test
	public void ignoresRoomAndTotalTimes()
	{
		assertNull(KillMessageParser.parseDuration("Challenge complete: Ocellus. Duration: <col=ef1020>2:25.80</col>. Total: <col=ef1020>2:25.80</col> (new personal best)"));
		assertNull(KillMessageParser.parseDuration("Tombs of Amascut total completion time: <col=ef1020>0:01</col> (new personal best)"));
		assertNull(KillMessageParser.parseDuration("Your Zulrah kill count is: <col=ff0000>4</col>."));
	}

	@Test
	public void formatSeconds()
	{
		assertEquals("0:45", KillMessageParser.formatSeconds(45));
		assertEquals("0:44.40", KillMessageParser.formatSeconds(44.4));
		assertEquals("1:44:31.20", KillMessageParser.formatSeconds(104 * 60 + 31.2));
	}
}
