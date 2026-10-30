package com.combatmasteries.tracking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Parses the game's kill count and kill duration chat messages.
 * <p>
 * The patterns are adapted from RuneLite's ChatCommandsPlugin (BSD-2-Clause), which
 * is the reference implementation for these message formats.
 */
public final class KillMessageParser
{
	private static final Pattern KILLCOUNT_PATTERN = Pattern.compile("Your (?<pre>completion count for |subdued |completed )?(?:<col=[0-9a-f]{6}>)?(?<boss>.+?)(?:</col>)? (?<post>(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?)is: ?(?:<col=[0-9a-f]{6}>|@.+?@)(?<kc>[0-9,]+)</col>");
	private static final String TEAM_SIZES = "(?<teamsize>\\d+(?:\\+|-\\d+)? players?|Solo)";
	private static final String TIME = "[0-9:]+(?:\\.[0-9]+)?";
	private static final String DURATION_PREFIX = "(?i)(?:(?:Fight |Lap |Challenge |Corrupted challenge )?duration:|Subdued in|(?<!total )completion time:|The Fractured Archive: Total completion time:) (?:<col=[0-9a-f]{6}>|@.+?@)";
	private static final Pattern RAIDS_PB_PATTERN = Pattern.compile("<col=ef20ff>Congratulations - your raid is complete!</col><br>Team size: <col=ff0000>" + TEAM_SIZES + "</col> Duration:</col> <col=ff0000>(?<time>" + TIME + ")</col> \\(new personal best\\)</col>");
	private static final Pattern RAIDS_DURATION_PATTERN = Pattern.compile("<col=ef20ff>Congratulations - your raid is complete!</col><br>Team size: <col=ff0000>" + TEAM_SIZES + "</col> Duration:</col> <col=ff0000>(?<time>" + TIME + ")</col> Personal best: </col><col=ff0000>(?<pb>" + TIME + ")</col>");
	private static final Pattern KILL_DURATION_PATTERN = Pattern.compile(DURATION_PREFIX + "(?<time>" + TIME + ")</col>\\. Personal best: (?:<col=ff0000>|@.+?@)?(?<pb>" + TIME + ")");
	private static final Pattern NEW_PB_PATTERN = Pattern.compile(DURATION_PREFIX + "(?<time>" + TIME + ")</col> \\(new personal best\\)");

	private KillMessageParser()
	{
	}

	/**
	 * @return the boss name from a kill count message, or null if this is not one
	 */
	@Nullable
	public static String parseKillCount(String message)
	{
		Matcher m = KILLCOUNT_PATTERN.matcher(message);
		if (!m.find())
		{
			return null;
		}

		String pre = m.group("pre");
		String post = m.group("post");
		if ((pre == null || pre.isEmpty()) && (post == null || post.isEmpty()))
		{
			// Not a kill count, e.g. "Your X is: 5"
			return null;
		}
		return m.group("boss");
	}

	/**
	 * @return the duration from a kill duration message, or null if this is not one
	 */
	@Nullable
	public static KillDuration parseDuration(String message)
	{
		Matcher m = NEW_PB_PATTERN.matcher(message);
		if (m.find())
		{
			double time = timeStringToSeconds(m.group("time"));
			return new KillDuration(time, time, true, null);
		}

		m = KILL_DURATION_PATTERN.matcher(message);
		if (m.find())
		{
			return new KillDuration(timeStringToSeconds(m.group("time")), timeStringToSeconds(m.group("pb")), false, null);
		}

		m = RAIDS_PB_PATTERN.matcher(message);
		if (m.find())
		{
			double time = timeStringToSeconds(m.group("time"));
			return new KillDuration(time, time, true, m.group("teamsize"));
		}

		m = RAIDS_DURATION_PATTERN.matcher(message);
		if (m.find())
		{
			return new KillDuration(timeStringToSeconds(m.group("time")), timeStringToSeconds(m.group("pb")), false, m.group("teamsize"));
		}

		return null;
	}

	/**
	 * Converts "m:ss", "m:ss.cc" or "h:mm:ss(.cc)" into seconds. Minutes may exceed 59
	 * (the Inferno reports "104:31.20").
	 */
	public static double timeStringToSeconds(String timeString)
	{
		String[] s = timeString.split(":");
		if (s.length == 2)
		{
			return Integer.parseInt(s[0]) * 60 + Double.parseDouble(s[1]);
		}
		else if (s.length == 3)
		{
			return Integer.parseInt(s[0]) * 3600 + Integer.parseInt(s[1]) * 60 + Double.parseDouble(s[2]);
		}
		return Double.parseDouble(timeString);
	}

	/**
	 * Formats seconds the way the game does: "m:ss" or "m:ss.cc" when precise.
	 */
	public static String formatSeconds(double seconds)
	{
		int hours = (int) (Math.floor(seconds) / 3600);
		int minutes = (int) (Math.floor(seconds / 60) % 60);
		double secs = seconds % 60;
		String prefix = hours > 0 ? String.format("%d:%02d:", hours, minutes) : String.format("%d:", minutes);
		return prefix + (Math.floor(secs) == secs ? String.format("%02d", (int) secs) : String.format("%05.2f", secs));
	}
}
