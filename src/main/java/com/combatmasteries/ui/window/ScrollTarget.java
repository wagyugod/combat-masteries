package com.combatmasteries.ui.window;

/**
 * The lists in the window that scroll, each with its own offset in {@link WindowState}.
 */
public enum ScrollTarget
{
	OVERVIEW,
	TASKS,
	BOSSES,
	REWARDS,
	/**
	 * The open filter dropdown's option list.
	 */
	FILTER;

	static ScrollTarget of(WindowView view)
	{
		switch (view)
		{
			case TASKS:
				return TASKS;
			case BOSSES:
				return BOSSES;
			case REWARDS:
				return REWARDS;
			default:
				return OVERVIEW;
		}
	}
}
