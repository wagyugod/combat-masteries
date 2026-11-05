package com.combatmasteries.ui.window;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The Tasks tab's filter dropdowns, top to bottom.
 */
@Getter
@RequiredArgsConstructor
public enum TaskFilter
{
	TYPE("Type"),
	CONTENT("Content"),
	COMPLETED("Completed");

	/**
	 * The label above the dropdown.
	 */
	private final String label;
}
