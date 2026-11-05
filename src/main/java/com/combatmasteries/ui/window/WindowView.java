package com.combatmasteries.ui.window;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The four tabs of the window, as in the Combat Achievements menu.
 */
@Getter
@RequiredArgsConstructor
public enum WindowView
{
	OVERVIEW("Overview"),
	TASKS("Tasks"),
	BOSSES("Bosses"),
	REWARDS("Rewards");

	private final String label;

	public String title()
	{
		return "Combat Masteries - " + label;
	}
}
