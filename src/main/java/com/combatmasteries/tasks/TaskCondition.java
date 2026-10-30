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
	 * Time limit for {@link ConditionType#MAX_TIME}, in seconds.
	 */
	private double seconds;

	/**
	 * @return true if this condition depends only on the kill time, so it can be judged
	 * from a personal best without the attempt having been watched
	 */
	public boolean isTimeOnly()
	{
		return type == ConditionType.MAX_TIME;
	}
}
