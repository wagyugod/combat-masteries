package com.combatmasteries.tracking;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Reads the developer-mode ::cmkill command, e.g. "::cmkill Solo ToB 44:44",
 * "::cmkill inferno 44:59 pillars" or "::cmkill toa 30:00 solo level=600 total=33:00".
 * Accepts common short names and solo/duo/trio, so testers don't need the exact kill-message
 * names.
 */
@Value
public class DevKill
{
	private static final Pattern TIME = Pattern.compile("^\\d+:\\d{2}(:\\d{2})?(\\.\\d+)?$");
	private static final Map<String, Integer> TEAM_WORDS = ImmutableMap.<String, Integer>builder()
		.put("solo", 1).put("duo", 2).put("trio", 3).put("4s", 4).put("4man", 4).put("5s", 5).put("5man", 5)
		.build();
	private static final Map<String, String> ALIASES = ImmutableMap.<String, String>builder()
		.put("inferno", "TzKal-Zuk").put("zuk", "TzKal-Zuk")
		.put("colo", "Sol Heredit").put("colosseum", "Sol Heredit").put("sol", "Sol Heredit")
		.put("tob", "Theatre of Blood").put("theatre of blood", "Theatre of Blood")
		.put("hmt", "Theatre of Blood: Hard Mode").put("hm tob", "Theatre of Blood: Hard Mode")
		.put("toa", "Tombs of Amascut: Expert Mode").put("expert toa", "Tombs of Amascut: Expert Mode")
		.put("cm", "Chambers of Xeric Challenge Mode").put("cox cm", "Chambers of Xeric Challenge Mode")
		.put("awakened levi", "Leviathan (awakened)").put("awakened leviathan", "Leviathan (awakened)")
		.put("awakened vard", "Vardorvis (awakened)").put("awakened vardorvis", "Vardorvis (awakened)")
		.put("awakened duke", "Duke Sucellus (awakened)").put("awakened sucellus", "Duke Sucellus (awakened)")
		.put("awakened whisperer", "Whisperer (awakened)").put("awakened whisp", "Whisperer (awakened)")
		.build();

	String boss;
	/**
	 * Kill time in seconds, or null when none was given (fine for one-inventory tasks).
	 */
	@Nullable
	Double seconds;
	@Nullable
	Integer teamSize;
	@Nullable
	Integer raidLevel;
	@Nullable
	Double totalSeconds;
	boolean pillarsCleared;

	/**
	 * @throws IllegalArgumentException with a message to show the tester
	 */
	public static DevKill parse(String[] args)
	{
		List<String> bossWords = new ArrayList<>();
		Double seconds = null;
		Integer team = null;
		Integer level = null;
		Double total = null;
		boolean pillars = false;

		for (String arg : args)
		{
			String a = arg.toLowerCase(Locale.ROOT);
			if (seconds == null && TIME.matcher(a).matches())
			{
				seconds = KillMessageParser.timeStringToSeconds(a);
			}
			else if (TEAM_WORDS.containsKey(a))
			{
				team = TEAM_WORDS.get(a);
			}
			else if (a.startsWith("team="))
			{
				team = number(a.substring(5), arg);
			}
			else if (a.startsWith("level="))
			{
				level = number(a.substring(6), arg);
			}
			else if (a.startsWith("total="))
			{
				String t = a.substring(6);
				if (!TIME.matcher(t).matches())
				{
					throw new IllegalArgumentException("\"" + arg + "\" isn't a time like total=33:00");
				}
				total = KillMessageParser.timeStringToSeconds(t);
			}
			else if (a.equals("pillars"))
			{
				pillars = true;
			}
			else if (seconds == null)
			{
				bossWords.add(arg.replace('_', ' '));
			}
			else
			{
				throw new IllegalArgumentException("Didn't understand \"" + arg + "\"");
			}
		}

		if (bossWords.isEmpty())
		{
			throw new IllegalArgumentException("Which boss? e.g. ::cmkill Solo ToB 44:44");
		}
		String typed = String.join(" ", bossWords);
		String boss = ALIASES.getOrDefault(typed.toLowerCase(Locale.ROOT), typed);
		return new DevKill(boss, seconds, team, level, total, pillars);
	}

	private static int number(String value, String arg)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("\"" + arg + "\" needs a number");
		}
	}
}
