package com.combatmasteries.tracking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts consumable supplies so the plugin can tell when a trip gained some.
 */
public final class Supplies
{
	private static final Pattern DOSES = Pattern.compile("\\((\\d)\\)$");

	private Supplies()
	{
	}

	/**
	 * @param actions the item's inventory menu actions
	 * @return true for anything eaten or drunk
	 */
	public static boolean isConsumable(String[] actions)
	{
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if ("Eat".equals(action) || "Drink".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Potions count by dose so that combining or decanting them changes nothing.
	 *
	 * @return how many supply units a stack of this item is worth
	 */
	public static int units(String name, int quantity)
	{
		Matcher m = DOSES.matcher(name);
		return m.find() ? Integer.parseInt(m.group(1)) * quantity : quantity;
	}
}
