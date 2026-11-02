package com.combatmasteries.tasks;

import static com.combatmasteries.tasks.Tasks.task;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class TaskListTest
{
	@Test
	public void bundledTaskListLoads()
	{
		TaskList taskList = TaskList.load(new Gson(), TaskList.class.getResourceAsStream("/com/combatmasteries/tasks.json"));
		assertFalse(taskList.getTasks().isEmpty());
		for (MasteryTask task : taskList.getTasks())
		{
			assertNotNull(task.getId(), task.getCategory());
			assertNotNull(task.getId(), task.getContent());
		}
		assertTrue(taskList.usesTotalTime("Tombs of Amascut: Expert Mode"));
		assertFalse(taskList.usesTotalTime("Theatre of Blood"));
		// Defaults survive JSON loading
		assertTrue(taskList.getTasks().get(0).isTracked());
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateIdsAreRejected()
	{
		MasteryTask a = task("a", "Zulrah", TaskCategory.SPEED, TaskCondition.underTime(60));
		new TaskList(Arrays.asList(a, a), Collections.emptyList());
	}

	@Test(expected = IllegalStateException.class)
	public void pillarTaskNeedsARegion()
	{
		MasteryTask a = task("a", "TzKal-Zuk", TaskCategory.CHALLENGE, TaskCondition.of(ConditionType.INFERNO_NO_PILLARS, 0));
		new TaskList(Collections.singletonList(a), Collections.emptyList());
	}
}
