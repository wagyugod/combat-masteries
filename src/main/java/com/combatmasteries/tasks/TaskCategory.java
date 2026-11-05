package com.combatmasteries.tasks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Top-end PvMers split into speed-runners and challenge-runners.
 */
@Getter
@RequiredArgsConstructor
public enum TaskCategory
{
	SPEED("Speed"),
	CHALLENGE("Challenge");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
