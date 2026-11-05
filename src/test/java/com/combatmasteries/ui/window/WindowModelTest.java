package com.combatmasteries.ui.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskCategory;
import com.combatmasteries.tasks.TaskList;
import com.google.gson.Gson;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.Test;

public class WindowModelTest
{
	private static TaskList taskList()
	{
		return TaskList.load(new Gson(), TaskList.class.getResourceAsStream("/com/combatmasteries/tasks.json"));
	}

	@Test
	public void formatsTimesLikeTheGame()
	{
		assertEquals("0:59", WindowModel.formatTime(59));
		assertEquals("24:45", WindowModel.formatTime(1485));
		assertEquals("16:33.60", WindowModel.formatTime(993.6));
		assertEquals("1:05:23.40", WindowModel.formatTime(3923.4));
	}

	@Test
	public void countsTiersAndContent()
	{
		TaskList tasks = taskList();
		MasteryProgress progress = new MasteryProgress();
		List<MasteryTask> all = tasks.getTasks();
		for (int i = 0; i < 20; i++)
		{
			progress.complete(all.get(i).getId(), 0, false);
		}
		WindowModel model = WindowModel.build(tasks, progress, "B I G F O X");

		assertEquals(20, model.getPoints());
		assertEquals(3, model.getTiers().size());
		WindowModel.Tier ascended = model.getTiers().get(0);
		assertEquals("Ascended", ascended.getName());
		assertEquals(MasteryTier.ASCENDED.threshold(tasks.size()), ascended.getThreshold());
		assertTrue(ascended.isUnlocked());
		assertFalse(model.getTiers().get(1).isUnlocked());
		assertEquals(model.getTiers().get(1), model.nextTier());
		int next = model.getTiers().get(1).getThreshold() - 20;
		assertEquals("Total Points: 20 - Next unlock in " + next + " points", model.pointsText());

		assertEquals(WindowModel.CONTENT_ORDER.size(), model.getContents().size());
		int total = model.getContents().stream().mapToInt(WindowModel.Content::getTasks).sum();
		int done = model.getContents().stream().mapToInt(WindowModel.Content::getCompleted).sum();
		assertEquals(tasks.size(), total);
		assertEquals(20, done);
	}

	@Test
	public void personalBestsInOrder()
	{
		MasteryProgress progress = new MasteryProgress();
		progress.recordPersonalBest(MasteryProgress.personalBestKey("Theatre of Blood", 3), 1092);
		WindowModel model = WindowModel.build(taskList(), progress, "x");
		List<WindowModel.PersonalBest> pbs = model.getPersonalBests();
		assertEquals(9, pbs.size());
		assertEquals("Inferno", pbs.get(0).getLabel());
		assertEquals("-", pbs.get(0).getValue());
		assertEquals("ToB (Trio)", pbs.get(3).getLabel());
		assertEquals("18:12", pbs.get(3).getValue());
		assertEquals("ToA Expert (Solo)", pbs.get(8).getLabel());
	}

	@Test
	public void allTiersUnlocked()
	{
		TaskList tasks = taskList();
		MasteryProgress progress = new MasteryProgress();
		tasks.getTasks().forEach(t -> progress.complete(t.getId(), 0, false));
		WindowModel model = WindowModel.build(tasks, progress, "x");
		assertNull(model.nextTier());
		assertEquals("Total Points: " + tasks.size() + " - All tiers unlocked", model.pointsText());
		assertEquals(1, WindowRenderer.barFill(model, WindowView.OVERVIEW), 0);
	}

	@Test
	public void filtersTasks()
	{
		WindowModel model = WindowModel.build(taskList(), new MasteryProgress(), "x");
		WindowState state = new WindowState();
		assertEquals(model.getTasks().size(), state.filter(model.getTasks()).size());

		state.setContentFilter("Awakened DT2");
		assertEquals(3, state.filter(model.getTasks()).size());
		state.setCompletedFilter(true);
		assertTrue(state.filter(model.getTasks()).isEmpty());
		state.setCompletedFilter(null);
		state.setContentFilter(null);
		state.setTypeFilter(TaskCategory.SPEED);
		assertTrue(state.filter(model.getTasks()).stream().allMatch(t -> t.getCategory() == TaskCategory.SPEED));

		state.setSearch("theatre");
		assertEquals(2, state.search(model.getContents()).size());
	}

	@Test
	public void rendersEveryViewWithoutSprites()
	{
		WindowModel model = WindowModel.build(taskList(), new MasteryProgress(), "x");
		for (WindowView view : WindowView.values())
		{
			WindowState state = new WindowState();
			state.setView(view);
			BufferedImage img = new BufferedImage(WindowChrome.WIDTH, 314, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			WindowRenderer.render(g, (id, frame) -> null, model, state, WindowChrome.WIDTH, 314);
			g.dispose();
		}
	}
}
