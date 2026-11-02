package com.combatmasteries.tasks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class MasteryTask
{
	/**
	 * Stable identifier. Progress is saved against this, so it must never change once
	 * a task has shipped.
	 */
	private String id;
	private String name;
	private String description;
	private TaskCategory category;
	/**
	 * Which content the task belongs to, e.g. "Inferno" or "Awakened DT2". Used for grouping.
	 */
	private String content;
	/**
	 * The boss as named in its kill count message, e.g. "TzKal-Zuk" or
	 * "Theatre of Blood: Hard Mode". Not used by trip or delve tasks.
	 */
	private String boss;
	private List<TaskCondition> conditions = new ArrayList<>();
	/**
	 * For "1 inventory" tasks: kills needed in one trip, boss name to count.
	 */
	private Map<String, Integer> trip = new LinkedHashMap<>();
	/**
	 * False while the plugin cannot yet detect this task. It still counts towards the total.
	 */
	private boolean tracked = true;

	public boolean isTrip()
	{
		return trip != null && !trip.isEmpty();
	}

	public boolean hasCondition(ConditionType type)
	{
		return conditions.stream().anyMatch(c -> c.getType() == type);
	}

	/**
	 * @return true if a recorded personal best is enough to complete this task
	 */
	public boolean isPersonalBestCreditable()
	{
		return tracked
			&& !isTrip()
			&& hasCondition(ConditionType.UNDER_TIME)
			&& conditions.stream().allMatch(TaskCondition::isPersonalBestCompatible);
	}
}
