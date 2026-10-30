package com.combatmasteries.tasks;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasteryTask
{
	/**
	 * Stable identifier. Progress is saved against this, so it must never change once
	 * a task has shipped.
	 */
	private String id;
	private String name;
	private String description;
	/**
	 * The boss as named in its kill count message, e.g. "Zulrah" or "TzKal-Zuk".
	 */
	private String boss;
	private List<TaskCondition> conditions = new ArrayList<>();
	/**
	 * How many qualifying kills in a row are needed. Any kill or death at this boss that
	 * does not qualify resets the streak.
	 */
	private int consecutive = 1;

	/**
	 * @return true if this task can be credited from a personal best alone
	 */
	public boolean isRetroCreditable()
	{
		return consecutive <= 1
			&& !conditions.isEmpty()
			&& conditions.stream().allMatch(TaskCondition::isTimeOnly);
	}
}
