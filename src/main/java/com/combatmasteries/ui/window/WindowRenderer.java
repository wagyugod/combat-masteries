package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.util.List;

/**
 * Draws the whole Combat Masteries window, a copy of the Combat Achievements window, into a graphics
 * context with its top left at (0, 0). Pure Java2D: the in-game overlay and the offline preview draw the
 * same pixels.
 */
public final class WindowRenderer
{
	/**
	 * The Rewards tab's tier buttons, as {@link RewardsView} draws them: a column of 32px tall buttons from
	 * (8, 81), the selected one 121 wide.
	 */
	static final int TIER_BUTTON_X = 8;
	static final int TIER_BUTTON_Y = 81;
	static final int TIER_BUTTON_W = 121;
	static final int TIER_BUTTON_H = 32;

	private WindowRenderer()
	{
	}

	/**
	 * @param w window width, normally {@link WindowChrome#WIDTH}
	 * @param h window height; the game stretches the window to the viewport, 314 at least
	 */
	public static void render(Graphics2D g, SpriteSource sprites, WindowModel model, WindowState state, int w, int h)
	{
		render(g, sprites, model, state, w, h, new WindowLayout());
	}

	/**
	 * @param layout filled in with where the clickable parts and scrolling lists were drawn
	 */
	public static void render(Graphics2D g, SpriteSource sprites, WindowModel model, WindowState state, int w, int h,
		WindowLayout layout)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			draw(new WidgetPainter(g2, new SpriteOverrides(sprites), layout), model, state, w, h);
		}
		finally
		{
			g2.dispose();
		}
	}

	private static void draw(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		WindowView view = state.getView();
		p.scrollTarget(ScrollTarget.of(view));
		WindowChrome.frame(p, w, h, view.title());
		if (view == WindowView.OVERVIEW && !state.isCapturing())
		{
			int x = w - WindowChrome.SCREENSHOT_RIGHT - WindowChrome.SCREENSHOT_W;
			WindowChrome.screenshotButton(p, w, !state.isMenuOpen()
				&& state.isHovered(x, WindowChrome.SCREENSHOT_Y, WindowChrome.SCREENSHOT_W, WindowChrome.SCREENSHOT_H));
		}
		if (view != WindowView.BOSSES)
		{
			// The Bosses tab has its search box there instead
			WindowChrome.pointsBar(p, w, model.pointsText(), barFill(model, view), view == WindowView.REWARDS ? ticks(model) : null);
		}
		switch (view)
		{
			case OVERVIEW:
				OverviewView.draw(p, model, state, w, h);
				break;
			case TASKS:
				TasksView.draw(p, model, state, w, h);
				break;
			case BOSSES:
				BossesView.draw(p, model, state, w, h);
				break;
			case REWARDS:
				RewardsView.draw(p, model, state, w, h);
				tierButtons(p, model.getTiers());
				break;
		}
		WindowChrome.menuButton(p);
		WindowMenus.menuButton(p, state);
		if (state.isMenuOpen())
		{
			WindowMenus.menu(p, state, w, h);
		}
		else if (view == WindowView.TASKS && state.getOpenFilter() != null)
		{
			WindowMenus.filterList(p, model, state, w, h);
		}
	}

	private static void tierButtons(WidgetPainter p, List<WindowModel.Tier> tiers)
	{
		for (int i = 0; i < tiers.size(); i++)
		{
			p.hotspot(Hotspot.Kind.TIER, tiers.get(i).getTier(), TIER_BUTTON_X, TIER_BUTTON_Y + i * TIER_BUTTON_H,
				TIER_BUTTON_W, TIER_BUTTON_H);
		}
	}

	/**
	 * Like the game: the Rewards tab fills the bar by total points against the top tier; the other tabs
	 * by progress from the last unlocked tier to the next.
	 */
	static double barFill(WindowModel model, WindowView view)
	{
		if (view == WindowView.REWARDS)
		{
			return (double) model.getPoints() / model.maxThreshold();
		}
		WindowModel.Tier next = model.nextTier();
		if (next == null)
		{
			return 1;
		}
		int previous = model.previousThreshold();
		return (double) (model.getPoints() - previous) / Math.max(1, next.getThreshold() - previous);
	}

	private static double[] ticks(WindowModel model)
	{
		double[] ticks = new double[model.getTiers().size()];
		for (int i = 0; i < ticks.length; i++)
		{
			ticks[i] = (double) model.getTiers().get(i).getThreshold() / model.maxThreshold();
		}
		return ticks;
	}
}
