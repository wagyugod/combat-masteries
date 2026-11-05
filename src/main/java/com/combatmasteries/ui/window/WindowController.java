package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Draws the window and turns mouse and keyboard input into changes to its {@link WindowState}, the way the
 * Combat Achievements window responds: the menu button opens a tab menu, filter boxes open option lists,
 * task rows expand, tier buttons pick a tier, and lists scroll with the wheel, the arrows (held), the
 * track and the thumb.
 * <p>
 * Coordinates are window pixels. Clicks are hit-tested against what the last {@link #render} drew. Input
 * arrives on the AWT thread while the client thread draws, so every method is synchronized.
 */
public class WindowController
{
	/**
	 * Pixels per mouse wheel notch (script 36).
	 */
	public static final int WHEEL_STEP = 45;
	/**
	 * Pixels per client cycle while a scroll arrow is held (script 33), and the length of a cycle.
	 */
	static final int ARROW_STEP = 4;
	static final long CYCLE_MILLIS = 20;
	static final int MAX_SEARCH = 30;

	private final WindowState state = new WindowState();
	private WindowLayout layout = new WindowLayout();

	/**
	 * The scrollbar whose thumb is being dragged, and where on the thumb it was picked up.
	 */
	@Nullable
	private WindowLayout.Scroll dragging;
	private int grab;
	/**
	 * The list whose scroll arrow is held, which way, and when it last moved.
	 */
	@Nullable
	private ScrollTarget held;
	private int heldDirection;
	private long lastStep;

	public synchronized WindowState getState()
	{
		return state;
	}

	public synchronized WindowLayout getLayout()
	{
		return layout;
	}

	/**
	 * Shows a tab with nothing open over it.
	 */
	public synchronized void show(WindowView view)
	{
		state.setView(view);
		state.setMenuOpen(false);
		state.setOpenFilter(null);
		state.setSearchFocused(false);
		state.setMouseX(-1);
		state.setMouseY(-1);
		release();
	}

	/**
	 * Draws the window at (0, 0), first moving any held scroll arrow on by the cycles since the last frame.
	 */
	public synchronized void render(Graphics2D g, SpriteSource sprites, WindowModel model, int w, int h, long now)
	{
		stepHeldArrow(now);
		draw(g, sprites, model, w, h);
		if (clampScrolls())
		{
			// A list got shorter (a filter, a collapsed row): draw it again where it can scroll to
			draw(g, sprites, model, w, h);
		}
	}

	private void draw(Graphics2D g, SpriteSource sprites, WindowModel model, int w, int h)
	{
		WindowLayout drawn = new WindowLayout();
		WindowRenderer.render(g, sprites, model, state, w, h, drawn);
		layout = drawn;
	}

	/**
	 * Keeps each list that was drawn within its range.
	 *
	 * @return whether any offset changed
	 */
	synchronized boolean clampScrolls()
	{
		boolean changed = false;
		for (ScrollTarget target : ScrollTarget.values())
		{
			WindowLayout.Scroll scroll = layout.scroll(target);
			if (scroll != null)
			{
				int value = state.getScroll(target);
				int clamped = ScrollMath.clamp(value, scroll.max());
				if (clamped != value)
				{
					state.setScroll(target, clamped);
					changed = true;
				}
			}
		}
		return changed;
	}

	/**
	 * A mouse button went down at (x, y).
	 *
	 * @return true if the close button was pressed
	 */
	public synchronized boolean press(int x, int y, long now)
	{
		move(x, y);
		Hotspot hit = layout.hit(x, y);
		// Any click other than on the search box gives typing back to the chatbox
		state.setSearchFocused(hit != null && hit.getKind() == Hotspot.Kind.SEARCH);
		if (hit == null)
		{
			return false;
		}

		switch (hit.getKind())
		{
			case CLOSE:
				return true;
			case MENU:
				state.setMenuOpen(!state.isMenuOpen());
				state.setOpenFilter(null);
				break;
			case MENU_ITEM:
				state.setView((WindowView) hit.getValue());
				state.setMenuOpen(false);
				state.setOpenFilter(null);
				break;
			case MENU_DISMISS:
				state.setMenuOpen(false);
				break;
			case FILTER:
				TaskFilter filter = (TaskFilter) hit.getValue();
				state.setOpenFilter(state.getOpenFilter() == filter ? null : filter);
				state.setFilterScroll(0);
				break;
			case FILTER_OPTION:
				if (state.getOpenFilter() != null)
				{
					state.selectFilter(state.getOpenFilter(), (String) hit.getValue());
					state.setTasksScroll(0);
				}
				state.setOpenFilter(null);
				break;
			case FILTER_DISMISS:
				state.setOpenFilter(null);
				break;
			case TASK:
				String id = (String) hit.getValue();
				state.setExpandedTask(Objects.equals(id, state.getExpandedTask()) ? null : id);
				break;
			case TIER:
				MasteryTier tier = (MasteryTier) hit.getValue();
				if (tier != state.getRewardsTier())
				{
					state.setRewardsTier(tier);
					state.setRewardsScroll(0);
				}
				break;
			case SCROLLBAR:
				pressScrollbar((WindowLayout.Scroll) hit.getValue(), y, now);
				break;
			case SEARCH:
				break;
		}
		return false;
	}

	private void pressScrollbar(WindowLayout.Scroll scroll, int y, long now)
	{
		Rectangle bar = scroll.getBar();
		if (bar == null)
		{
			return;
		}

		int from = y - bar.y;
		if (from < ScrollMath.ARROW || from >= bar.height - ScrollMath.ARROW)
		{
			held = scroll.getTarget();
			heldDirection = from < ScrollMath.ARROW ? -1 : 1;
			lastStep = now;
			scrollBy(held, heldDirection * ARROW_STEP);
			return;
		}

		// Pressing the thumb picks it up where it was pressed; pressing the track picks it up by its middle
		// there (script 34)
		int onTrack = from - ScrollMath.ARROW;
		int current = state.getScroll(scroll.getTarget());
		int thumbTop = ScrollMath.thumbTop(bar.height, scroll.getContent(), current);
		int thumb = ScrollMath.thumbHeight(bar.height, scroll.getContent());
		grab = onTrack >= thumbTop && onTrack < thumbTop + thumb ? onTrack - thumbTop : thumb / 2;
		dragging = scroll;
		dragTo(y);
	}

	/**
	 * The mouse moved with a button down.
	 */
	public synchronized void drag(int x, int y)
	{
		move(x, y);
		dragTo(y);
	}

	private void dragTo(int y)
	{
		if (dragging == null || dragging.getBar() == null)
		{
			return;
		}
		Rectangle bar = dragging.getBar();
		int top = y - bar.y - ScrollMath.ARROW - grab;
		state.setScroll(dragging.getTarget(), ScrollMath.scrollForThumb(bar.height, dragging.getContent(), top));
	}

	/**
	 * Whether a scrollbar's thumb is being dragged, so drags outside the window still belong to it.
	 */
	public synchronized boolean isDragging()
	{
		return dragging != null;
	}

	/**
	 * The mouse button came up: lets go of the thumb and the arrows.
	 */
	public synchronized void release()
	{
		dragging = null;
		held = null;
	}

	public synchronized void move(int x, int y)
	{
		state.setMouseX(x);
		state.setMouseY(y);
	}

	/**
	 * The mouse left the window.
	 */
	public synchronized void exit()
	{
		move(-1, -1);
	}

	/**
	 * Scrolls the list under the open filter, or the tab's list, by whole wheel notches.
	 */
	public synchronized void wheel(int notches)
	{
		if (state.isMenuOpen())
		{
			return;
		}
		ScrollTarget target = state.getOpenFilter() != null ? ScrollTarget.FILTER : ScrollTarget.of(state.getView());
		WindowLayout.Scroll scroll = layout.scroll(target);
		if (scroll != null)
		{
			scrollBy(target, notches * scroll.getStep());
		}
	}

	private void scrollBy(ScrollTarget target, int pixels)
	{
		WindowLayout.Scroll scroll = layout.scroll(target);
		if (scroll != null)
		{
			state.setScroll(target, ScrollMath.clamp(state.getScroll(target) + pixels, scroll.max()));
		}
	}

	private void stepHeldArrow(long now)
	{
		if (held == null)
		{
			return;
		}
		long cycles = (now - lastStep) / CYCLE_MILLIS;
		if (cycles > 0)
		{
			lastStep += cycles * CYCLE_MILLIS;
			scrollBy(held, (int) Math.min(Integer.MAX_VALUE / ARROW_STEP, cycles) * heldDirection * ARROW_STEP);
		}
	}

	/**
	 * Whether typed characters go to the Bosses search: only after its box was clicked, so the chatbox
	 * keeps working otherwise.
	 */
	public synchronized boolean isSearching()
	{
		return state.getView() == WindowView.BOSSES && state.isSearchFocused();
	}

	/**
	 * Types a character into the Bosses search.
	 *
	 * @return whether the window took it
	 */
	public synchronized boolean type(char c)
	{
		if (!isSearching() || Character.isISOControl(c) || c == 0xffff)
		{
			return false;
		}
		if (state.getSearch().length() < MAX_SEARCH)
		{
			state.setSearch(state.getSearch() + c);
			state.setBossesScroll(0);
		}
		return true;
	}

	/**
	 * Deletes the last character of the Bosses search.
	 *
	 * @return whether the window took the key
	 */
	public synchronized boolean backspace()
	{
		if (!isSearching())
		{
			return false;
		}
		String search = state.getSearch();
		if (!search.isEmpty())
		{
			state.setSearch(search.substring(0, search.length() - 1));
			state.setBossesScroll(0);
		}
		return true;
	}

	/**
	 * Escape closes the menu or filter list if one is open.
	 *
	 * @return true if nothing was open, so the window itself should close
	 */
	public synchronized boolean escape()
	{
		if (state.isMenuOpen() || state.getOpenFilter() != null || state.isSearchFocused())
		{
			state.setMenuOpen(false);
			state.setOpenFilter(null);
			state.setSearchFocused(false);
			return false;
		}
		return true;
	}
}
