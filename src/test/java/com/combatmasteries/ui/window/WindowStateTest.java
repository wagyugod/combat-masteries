package com.combatmasteries.ui.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.TaskCategory;
import com.combatmasteries.tasks.TaskList;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class WindowStateTest
{
	private static WindowModel model()
	{
		TaskList tasks = TaskList.load(new Gson(), TaskList.class.getResourceAsStream("/com/combatmasteries/tasks.json"));
		return WindowModel.build(tasks, new MasteryProgress(), "Player");
	}

	@Test
	public void filterOptionsStartWithAll()
	{
		WindowModel model = model();
		assertEquals(Arrays.asList("All", "Speed", "Challenge"), WindowState.filterOptions(TaskFilter.TYPE, model));
		assertEquals(Arrays.asList("All", "Yes", "No"), WindowState.filterOptions(TaskFilter.COMPLETED, model));

		List<String> content = WindowState.filterOptions(TaskFilter.CONTENT, model);
		assertEquals("All", content.get(0));
		assertTrue(content.contains("Inferno"));
	}

	@Test
	public void selectingAnOptionSetsTheFilter()
	{
		WindowState state = new WindowState();
		state.selectFilter(TaskFilter.TYPE, "Challenge");
		state.selectFilter(TaskFilter.CONTENT, "Inferno");
		state.selectFilter(TaskFilter.COMPLETED, "No");
		assertEquals(TaskCategory.CHALLENGE, state.getTypeFilter());
		assertEquals("Inferno", state.getContentFilter());
		assertEquals(Boolean.FALSE, state.getCompletedFilter());
		assertEquals("Challenge", state.filterText(TaskFilter.TYPE));
		assertEquals("Inferno", state.filterText(TaskFilter.CONTENT));
		assertEquals("No", state.filterText(TaskFilter.COMPLETED));

		for (TaskFilter filter : TaskFilter.values())
		{
			state.selectFilter(filter, "All");
			assertEquals("All", state.filterText(filter));
		}
		assertNull(state.getTypeFilter());
		assertNull(state.getContentFilter());
		assertNull(state.getCompletedFilter());
	}

	@Test
	public void filtersTheTaskList()
	{
		WindowModel model = model();
		WindowState state = new WindowState();
		state.selectFilter(TaskFilter.CONTENT, "Inferno");
		state.selectFilter(TaskFilter.TYPE, "Speed");
		List<WindowModel.Task> shown = state.filter(model.getTasks());
		assertFalse(shown.isEmpty());
		for (WindowModel.Task task : shown)
		{
			assertEquals("Inferno", task.getContent());
			assertEquals(TaskCategory.SPEED, task.getCategory());
		}
		state.selectFilter(TaskFilter.COMPLETED, "Yes");
		assertTrue(state.filter(model.getTasks()).isEmpty());
	}

	@Test
	public void eachListHasItsOwnScroll()
	{
		WindowState state = new WindowState();
		int value = 10;
		for (ScrollTarget target : ScrollTarget.values())
		{
			state.setScroll(target, value);
			assertEquals(value, state.getScroll(target));
			value += 10;
		}
		assertEquals(10, state.getOverviewScroll());
		assertEquals(20, state.getTasksScroll());
		assertEquals(30, state.getBossesScroll());
		assertEquals(40, state.getRewardsScroll());
		assertEquals(50, state.getFilterScroll());
	}
}
