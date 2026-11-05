package com.combatmasteries.ui.window;

import java.awt.Graphics2D;

/**
 * Draws the whole Combat Masteries window, a copy of the Combat Achievements window, into a graphics
 * context with its top left at (0, 0). Pure Java2D: the in-game overlay and the offline preview draw the
 * same pixels.
 */
public final class WindowRenderer
{
	private WindowRenderer()
	{
	}

	/**
	 * @param w window width, normally {@link WindowChrome#WIDTH}
	 * @param h window height; the game stretches the window to the viewport, 314 at least
	 */
	public static void render(Graphics2D g, SpriteSource sprites, WindowModel model, WindowState state, int w, int h)
	{
		WidgetPainter p = new WidgetPainter(g, sprites);
		WindowView view = state.getView();
		WindowChrome.frame(p, w, h, view.title());
		if (view != WindowView.BOSSES)
		{
			// The Bosses tab has its search box there instead
			WindowChrome.pointsBar(p, w, model.pointsText(), barFill(model, view), view == WindowView.REWARDS ? ticks(model) : null);
		}
		switch (view)
		{
			case OVERVIEW:
				OverviewView.draw(p, model, w, h);
				break;
			case TASKS:
				TasksView.draw(p, model, state, w, h);
				break;
			case BOSSES:
				BossesView.draw(p, model, state, w, h);
				break;
			case REWARDS:
				RewardsView.draw(p, model, state, w, h);
				break;
		}
		WindowChrome.menuButton(p);
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
