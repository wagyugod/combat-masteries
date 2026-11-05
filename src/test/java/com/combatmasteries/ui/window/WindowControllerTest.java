package com.combatmasteries.ui.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskCategory;
import com.combatmasteries.tasks.TaskList;
import com.google.gson.Gson;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.Before;
import org.junit.Test;

public class WindowControllerTest
{
	private static final int W = WindowChrome.WIDTH;
	private static final int H = 340;

	private WindowModel model;
	private WindowController controller;
	private long now;

	@Before
	public void setUp()
	{
		TaskList tasks = TaskList.load(new Gson(), TaskList.class.getResourceAsStream("/com/combatmasteries/tasks.json"));
		model = WindowModel.build(tasks, new MasteryProgress(), "Player");
		controller = new WindowController();
	}

	private void render()
	{
		BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		controller.render(g, (id, frame) -> null, model, W, H, now);
		g.dispose();
	}

	private Hotspot find(Hotspot.Kind kind, Object value)
	{
		Hotspot hotspot = controller.getLayout().find(kind, value);
		assertNotNull(kind + " " + value + " was not drawn", hotspot);
		return hotspot;
	}

	/**
	 * Clicks the middle of a hotspot, then draws the next frame.
	 */
	private boolean click(Hotspot.Kind kind, Object value)
	{
		Rectangle r = find(kind, value).getBounds();
		boolean close = controller.press((int) r.getCenterX(), (int) r.getCenterY(), now);
		controller.release();
		render();
		return close;
	}

	private WindowState state()
	{
		return controller.getState();
	}

	@Test
	public void closeButtonClosesTheWindow()
	{
		controller.show(WindowView.OVERVIEW);
		render();
		assertTrue(click(Hotspot.Kind.CLOSE, null));
	}

	@Test
	public void menuSwitchesTabs()
	{
		controller.show(WindowView.OVERVIEW);
		render();
		assertNull(controller.getLayout().find(Hotspot.Kind.MENU_ITEM, null));

		click(Hotspot.Kind.MENU, null);
		assertTrue(state().isMenuOpen());
		// The current tab is not offered
		assertNull(controller.getLayout().find(Hotspot.Kind.MENU_ITEM, WindowView.OVERVIEW));

		click(Hotspot.Kind.MENU_ITEM, WindowView.REWARDS);
		assertEquals(WindowView.REWARDS, state().getView());
		assertFalse(state().isMenuOpen());
	}

	@Test
	public void clickingAwayClosesTheMenu()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.MENU, null);
		assertTrue(state().isMenuOpen());

		// The close button is under the menu's overlay: the click only closes the menu
		Rectangle close = find(Hotspot.Kind.CLOSE, null).getBounds();
		assertFalse(controller.press(close.x + 2, close.y + 2, now));
		assertFalse(state().isMenuOpen());
		assertEquals(WindowView.TASKS, state().getView());
	}

	@Test
	public void filterListPicksAnOption()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.FILTER, TaskFilter.TYPE);
		assertEquals(TaskFilter.TYPE, state().getOpenFilter());

		click(Hotspot.Kind.FILTER_OPTION, "Challenge");
		assertNull(state().getOpenFilter());
		assertEquals(TaskCategory.CHALLENGE, state().getTypeFilter());
		for (WindowModel.Task task : state().filter(model.getTasks()))
		{
			assertEquals(TaskCategory.CHALLENGE, task.getCategory());
		}
	}

	@Test
	public void filterListClosesOnASecondClickOrElsewhere()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.FILTER, TaskFilter.COMPLETED);
		click(Hotspot.Kind.FILTER, TaskFilter.COMPLETED);
		assertNull(state().getOpenFilter());

		click(Hotspot.Kind.FILTER, TaskFilter.CONTENT);
		assertEquals(TaskFilter.CONTENT, state().getOpenFilter());
		controller.press(W - 60, H - 20, now);
		assertNull(state().getOpenFilter());
		assertNull(state().getContentFilter());
	}

	@Test
	public void longFilterListScrollsInsideTheWindow()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.FILTER, TaskFilter.CONTENT);
		WindowLayout.Scroll list = controller.getLayout().scroll(ScrollTarget.FILTER);
		assertNotNull(list);
		for (Hotspot hotspot : controller.getLayout().hotspots())
		{
			assertTrue(hotspot.getBounds().y + hotspot.getBounds().height <= H);
		}

		controller.wheel(100);
		assertEquals(list.max(), state().getFilterScroll());
		// The task list behind it does not move
		assertEquals(0, state().getTasksScroll());
	}

	@Test
	public void taskRowsExpandAndCollapse()
	{
		controller.show(WindowView.TASKS);
		render();
		String id = model.getTasks().get(1).getId();
		click(Hotspot.Kind.TASK, id);
		assertEquals(id, state().getExpandedTask());

		Rectangle expanded = find(Hotspot.Kind.TASK, id).getBounds();
		assertTrue(expanded.height > TasksView.ROW_H);

		click(Hotspot.Kind.TASK, id);
		assertNull(state().getExpandedTask());
	}

	@Test
	public void rewardsTierButtonsPickATier()
	{
		controller.show(WindowView.REWARDS);
		render();
		click(Hotspot.Kind.TIER, MasteryTier.IMMORTAL);
		assertEquals(MasteryTier.IMMORTAL, state().getRewardsTier());
		click(Hotspot.Kind.TIER, MasteryTier.EXALTED);
		assertEquals(MasteryTier.EXALTED, state().getRewardsTier());
	}

	@Test
	public void wheelScrollsTheTabsListWithinRange()
	{
		controller.show(WindowView.TASKS);
		render();
		controller.wheel(2);
		assertEquals(2 * WindowController.WHEEL_STEP, state().getTasksScroll());
		controller.wheel(-5);
		assertEquals(0, state().getTasksScroll());

		WindowLayout.Scroll tasks = controller.getLayout().scroll(ScrollTarget.TASKS);
		assertNotNull(tasks);
		controller.wheel(10_000);
		assertEquals(tasks.max(), state().getTasksScroll());
	}

	@Test
	public void wheelIsIgnoredWhileTheMenuIsOpen()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.MENU, null);
		controller.wheel(3);
		assertEquals(0, state().getTasksScroll());
	}

	@Test
	public void overviewScrollsARowANotch()
	{
		controller.show(WindowView.OVERVIEW);
		render();
		WindowLayout.Scroll bests = controller.getLayout().scroll(ScrollTarget.OVERVIEW);
		assertNotNull(bests);
		controller.wheel(1);
		assertEquals(Math.min(bests.getStep(), bests.max()), state().getOverviewScroll());
		controller.wheel(100);
		assertEquals(bests.max(), state().getOverviewScroll());
	}

	@Test
	public void heldArrowScrollsEveryCycle()
	{
		controller.show(WindowView.TASKS);
		render();
		Rectangle bar = controller.getLayout().scroll(ScrollTarget.TASKS).getBar();
		controller.press(bar.x + 8, bar.y + bar.height - 8, now);
		assertEquals(WindowController.ARROW_STEP, state().getTasksScroll());

		now += 5 * WindowController.CYCLE_MILLIS;
		render();
		assertEquals(6 * WindowController.ARROW_STEP, state().getTasksScroll());

		controller.release();
		now += 5 * WindowController.CYCLE_MILLIS;
		render();
		assertEquals(6 * WindowController.ARROW_STEP, state().getTasksScroll());

		controller.press(bar.x + 8, bar.y + 8, now);
		assertEquals(5 * WindowController.ARROW_STEP, state().getTasksScroll());
		controller.release();
	}

	@Test
	public void draggingTheThumbScrolls()
	{
		controller.show(WindowView.TASKS);
		render();
		WindowLayout.Scroll tasks = controller.getLayout().scroll(ScrollTarget.TASKS);
		Rectangle bar = tasks.getBar();
		int thumb = ScrollMath.thumbHeight(bar.height, tasks.getContent());

		// Pick the thumb up by its middle and drag it to the bottom
		controller.press(bar.x + 8, bar.y + ScrollMath.ARROW + thumb / 2, now);
		assertTrue(controller.isDragging());
		assertEquals(0, state().getTasksScroll());
		controller.drag(bar.x + 8, bar.y + bar.height + 100);
		assertEquals(tasks.max(), state().getTasksScroll());
		controller.release();
		assertFalse(controller.isDragging());

		// Pressing the track brings the thumb's middle there
		render();
		int middle = bar.y + ScrollMath.ARROW + ScrollMath.track(bar.height) / 2;
		controller.press(bar.x + 8, middle, now);
		controller.release();
		int top = ScrollMath.thumbTop(bar.height, tasks.getContent(), state().getTasksScroll());
		assertTrue(Math.abs(bar.y + ScrollMath.ARROW + top + thumb / 2 - middle) <= 1);
	}

	@Test
	public void shorterListPullsTheScrollBack()
	{
		controller.show(WindowView.TASKS);
		render();
		controller.wheel(10_000);
		assertTrue(state().getTasksScroll() > 0);

		state().setContentFilter("Inferno");
		render();
		WindowLayout.Scroll tasks = controller.getLayout().scroll(ScrollTarget.TASKS);
		assertEquals(tasks.max(), state().getTasksScroll());
	}

	@Test
	public void typingSearchesBossesOnly()
	{
		controller.show(WindowView.OVERVIEW);
		render();
		assertFalse(controller.isSearching());
		assertFalse(controller.type('a'));
		assertFalse(controller.backspace());

		controller.show(WindowView.BOSSES);
		render();
		// The chatbox keeps typing until the search box is clicked
		assertFalse(controller.isSearching());
		assertFalse(controller.type('T'));
		click(Hotspot.Kind.SEARCH, null);
		assertTrue(controller.type('T'));
		assertTrue(controller.type('o'));
		assertTrue(controller.type('m'));
		assertFalse(controller.type('\n'));
		assertEquals("Tom", state().getSearch());
		assertTrue(controller.backspace());
		assertEquals("To", state().getSearch());

		for (int i = 0; i < 100; i++)
		{
			controller.type('x');
		}
		assertEquals(WindowController.MAX_SEARCH, state().getSearch().length());

		// A click elsewhere, or Escape, gives typing back to the chatbox
		controller.press(1, 1, 0);
		assertFalse(controller.isSearching());
		click(Hotspot.Kind.SEARCH, null);
		assertFalse(controller.escape());
		assertFalse(controller.isSearching());
	}

	@Test
	public void escapeClosesWhatIsOpenFirst()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.FILTER, TaskFilter.TYPE);
		assertFalse(controller.escape());
		assertNull(state().getOpenFilter());
		render();
		click(Hotspot.Kind.MENU, null);
		assertFalse(controller.escape());
		assertFalse(state().isMenuOpen());
		assertTrue(controller.escape());
	}

	@Test
	public void showResetsWhatWasOpen()
	{
		controller.show(WindowView.TASKS);
		render();
		click(Hotspot.Kind.FILTER, TaskFilter.TYPE);
		controller.move(10, 10);
		controller.show(WindowView.BOSSES);
		assertFalse(controller.isSearching());
		assertEquals(WindowView.BOSSES, state().getView());
		assertNull(state().getOpenFilter());
		assertEquals(-1, state().getMouseX());
	}
}
