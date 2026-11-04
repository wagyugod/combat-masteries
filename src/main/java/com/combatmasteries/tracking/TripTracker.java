package com.combatmasteries.tracking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;

/**
 * Counts boss kills made on one inventory, for "1 inventory" tasks.
 * <p>
 * The plugin calls {@link #restock(String)} whenever something resupplies the player:
 * opening a bank, drinking from a pool, praying at an altar, gaining food or potions,
 * dying, or receiving heal other, energy transfer or vengeance other. Teleporting is
 * allowed.
 */
public class TripTracker
{
	private final Map<String, Integer> kills = new HashMap<>();
	@Getter
	private String lastRestockReason = "none yet";

	public void onKill(String boss)
	{
		kills.merge(key(boss), 1, Integer::sum);
	}

	public void restock(String reason)
	{
		kills.clear();
		lastRestockReason = reason;
	}

	/**
	 * @return true if this trip includes at least the given kills (boss to count)
	 */
	public boolean satisfies(Map<String, Integer> required)
	{
		for (Map.Entry<String, Integer> entry : required.entrySet())
		{
			if (kills.getOrDefault(key(entry.getKey()), 0) < entry.getValue())
			{
				return false;
			}
		}
		return !required.isEmpty();
	}

	/**
	 * @return true if this trip includes a kill of any of these bosses
	 */
	public boolean hasAnyKill(Iterable<String> bosses)
	{
		for (String boss : bosses)
		{
			if (kills.getOrDefault(key(boss), 0) > 0)
			{
				return true;
			}
		}
		return false;
	}

	public Map<String, Integer> getKills()
	{
		return Collections.unmodifiableMap(kills);
	}

	private static String key(String boss)
	{
		return boss.toLowerCase(Locale.ROOT);
	}
}
