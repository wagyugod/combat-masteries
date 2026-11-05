package com.combatmasteries.tasks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCondition
{
	private ConditionType type;
	/**
	 * Time limit for {@link ConditionType#UNDER_TIME}, in seconds.
	 */
	private double seconds;
	/**
	 * Number for {@link ConditionType#TEAM_SIZE}, {@link ConditionType#MIN_RAID_LEVEL} and
	 * {@link ConditionType#MIN_DELVE_LEVEL}.
	 */
	private int value;

	public static TaskCondition underTime(double seconds)
	{
		return new TaskCondition(ConditionType.UNDER_TIME, seconds, 0);
	}

	public static TaskCondition of(ConditionType type, int value)
	{
		return new TaskCondition(type, 0, value);
	}

	/**
	 * @return true if this condition can be judged from a personal best, which is recorded
	 * per boss and team size
	 */
	public boolean isPersonalBestCompatible()
	{
		return type == ConditionType.UNDER_TIME || type == ConditionType.TEAM_SIZE;
	}
}
