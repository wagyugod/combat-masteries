package com.combatmasteries.tasks;

import java.awt.Color;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tiers unlock at a percentage of all tasks completed. Every task is worth one point.
 * Immortal is 90%, the same share as the gilded collection log staff.
 */
@Getter
@RequiredArgsConstructor
public enum MasteryTier
{
	ASCENDED("Ascended Grandmaster", 25, new Color(230, 50, 40)),
	EXALTED("Exalted Grandmaster", 50, new Color(70, 100, 255)),
	IMMORTAL("Immortal Grandmaster", 90, new Color(240, 245, 255));

	private final String displayName;
	private final int percent;
	/**
	 * Colour used for this tier in the side panel and chat.
	 */
	private final Color color;

	/**
	 * @return tasks needed for this tier, rounded down (90% of 92 tasks is 82.8, so 82),
	 * and never less than one
	 */
	public int threshold(int totalTasks)
	{
		return Math.max(1, totalTasks * percent / 100);
	}

	/**
	 * @return the highest tier reached, or null if none
	 */
	@Nullable
	public static MasteryTier forPoints(int points, int totalTasks)
	{
		if (totalTasks <= 0)
		{
			return null;
		}

		MasteryTier reached = null;
		for (MasteryTier tier : values())
		{
			if (points >= tier.threshold(totalTasks))
			{
				reached = tier;
			}
		}
		return reached;
	}

	/**
	 * @return the next tier above the given one, or null at the top
	 */
	@Nullable
	public static MasteryTier next(@Nullable MasteryTier tier)
	{
		if (tier == null)
		{
			return values()[0];
		}
		int next = tier.ordinal() + 1;
		return next < values().length ? values()[next] : null;
	}
}
