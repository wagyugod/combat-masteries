package com.combatmasteries.tasks;

import static org.junit.Assert.assertFalse;
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
			assertTrue(task.getId(), taskList.getBosses().containsKey(task.getBoss()));
			assertTrue(task.getId(), task.getConsecutive() >= 1);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void duplicateIdsAreRejected()
	{
		MasteryTask task = new MasteryTask("a", "A", "", "Zulrah",
			Collections.singletonList(new TaskCondition(ConditionType.NO_PRAYER, 0)), 1);
		new TaskList(Arrays.asList(task, task),
			Collections.singletonList(new BossDefinition("Zulrah", Collections.singletonList("Zulrah"), Collections.emptyList())));
	}

	@Test(expected = IllegalStateException.class)
	public void unknownBossIsRejected()
	{
		MasteryTask task = new MasteryTask("a", "A", "", "Nobody",
			Collections.singletonList(new TaskCondition(ConditionType.NO_PRAYER, 0)), 1);
		new TaskList(Collections.singletonList(task), Collections.emptyList());
	}
}
