package com.combatmasteries.tasks;

import java.util.Arrays;
import java.util.Map;

/**
 * Test helpers for building tasks.
 */
final class Tasks
{
	private Tasks()
	{
	}

	static MasteryTask task(String id, String boss, TaskCategory category, TaskCondition... conditions)
	{
		MasteryTask task = new MasteryTask();
		task.setId(id);
		task.setName(id);
		task.setDescription("");
		task.setCategory(category);
		task.setContent(boss == null ? "Doom of Mokhaiotl" : boss);
		task.setBoss(boss);
		task.setConditions(Arrays.asList(conditions));
		return task;
	}

	static MasteryTask trip(String id, Map<String, Integer> kills)
	{
		MasteryTask task = new MasteryTask();
		task.setId(id);
		task.setName(id);
		task.setDescription("");
		task.setCategory(TaskCategory.CHALLENGE);
		task.setContent("Awakened Bosses");
		task.setTrip(kills);
		return task;
	}
}
