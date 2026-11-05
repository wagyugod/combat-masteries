package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;
import net.runelite.api.gameval.SpriteID;

/**
 * What opens over the window: the tab menu under the top-left button (script 4768, in the steel border of
 * script 249) and a Tasks filter's option list (script 4826).
 */
final class WindowMenus
{
	/**
	 * The tab menu: 81x104 at (0, 29), four 69x23 rows from (6, 6).
	 */
	static final int MENU_X = 0;
	static final int MENU_Y = 29;
	static final int MENU_W = 81;
	static final int MENU_H = 104;
	static final int MENU_ROW_X = 6;
	static final int MENU_ROW_Y = 6;
	static final int MENU_ROW_W = MENU_W - 12;
	static final int MENU_ROW_H = 23;
	/**
	 * The menu's background, stretched to fill it.
	 */
	private static final int MENU_BACKGROUND = SpriteID.TRADEBACKING_LIGHT;
	/**
	 * The current tab's row: a faint white band (transparency 235) and grey text.
	 */
	private static final int MENU_CURRENT_TRANS = 235;
	private static final int MENU_CURRENT_TEXT = 0xc8c8c8;
	/**
	 * The menu button's open (and hovered) sprite.
	 */
	static final int MENU_OPEN = SpriteID.MenuButtons._1;

	/**
	 * A filter's option list: rows 14 tall in the small font, under the value box and as wide as it, inside
	 * the box's dark and light lines.
	 */
	static final int OPTION_H = 14;
	private static final int OPTION_HOVER_TRANS = 200;
	/**
	 * Space kept between the bottom of a list and the bottom of the window.
	 */
	private static final int LIST_MARGIN = 8;

	private WindowMenus()
	{
	}

	static void menuButton(WidgetPainter p, WindowState state)
	{
		if (state.isMenuOpen() || state.isHovered(7, 7, 21, 21))
		{
			p.sprite(MENU_OPEN, 7, 7);
		}
	}

	static void menu(WidgetPainter p, WindowState state, int w, int h)
	{
		// Like the game's menu overlay, a click anywhere else closes the menu
		p.hotspot(Hotspot.Kind.MENU_DISMISS, null, 0, 0, w, h);

		p.graphic(MENU_BACKGROUND, MENU_X, MENU_Y, MENU_W, MENU_H, false);
		steelBorder(p, MENU_X, MENU_Y, MENU_W, MENU_H);
		WindowView[] views = WindowView.values();
		for (int i = 0; i < views.length; i++)
		{
			WindowView view = views[i];
			int x = MENU_X + MENU_ROW_X;
			int y = MENU_Y + MENU_ROW_Y + i * MENU_ROW_H;
			boolean current = view == state.getView();
			int colour = WindowChrome.ORANGE;
			if (current)
			{
				p.fill(x, y, MENU_ROW_W, MENU_ROW_H, WindowChrome.WHITE, MENU_CURRENT_TRANS);
				colour = MENU_CURRENT_TEXT;
			}
			else
			{
				if (state.isHovered(x, y, MENU_ROW_W, MENU_ROW_H))
				{
					colour = WindowChrome.WHITE;
				}
				p.hotspot(Hotspot.Kind.MENU_ITEM, view, x, y, MENU_ROW_W, MENU_ROW_H);
			}
			p.text(view.getLabel(), GameFont.REGULAR, colour, true, x, y, MENU_ROW_W, MENU_ROW_H,
				WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		}
	}

	/**
	 * The open filter's option list, under its value box. A list too long for the window scrolls.
	 */
	static void filterList(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		TaskFilter filter = state.getOpenFilter();
		Hotspot box = filter == null ? null : p.layout().find(Hotspot.Kind.FILTER, filter);
		p.hotspot(Hotspot.Kind.FILTER_DISMISS, null, 0, 0, w, h);
		if (box == null)
		{
			return;
		}

		List<String> options = WindowState.filterOptions(filter, model);
		int x = box.getBounds().x;
		int y = box.getBounds().y + box.getBounds().height;
		int listW = box.getBounds().width;
		int room = h - LIST_MARGIN - y - 4;
		int visible = Math.max(1, Math.min(options.size(), room / OPTION_H));
		int viewport = visible * OPTION_H;
		int content = options.size() * OPTION_H;
		boolean scrolls = visible < options.size();
		int rowW = listW - 4 - (scrolls ? ScrollMath.ARROW : 0);
		int scroll = ScrollMath.clamp(state.getFilterScroll(), ScrollMath.max(content, viewport));

		p.graphic(WindowChrome.STONE, x, y, listW, viewport + 4, true);
		p.outline(x, y, listW, viewport + 4, WindowChrome.LINE_DARK);
		p.outline(x + 1, y + 1, listW - 2, viewport + 2, WindowChrome.LINE_LIGHT);

		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(x + 2, y + 2, rowW, viewport);
		for (int i = 0; i < options.size(); i++)
		{
			int ry = y + 2 + i * OPTION_H - scroll;
			if (ry + OPTION_H <= y + 2 || ry >= y + 2 + viewport)
			{
				continue;
			}
			String option = options.get(i);
			int colour = WindowChrome.ORANGE;
			if (state.isHovered(x + 2, ry, rowW, OPTION_H) && state.isHovered(x + 2, y + 2, rowW, viewport))
			{
				p.fill(x + 2, ry, rowW, OPTION_H, WindowChrome.WHITE, OPTION_HOVER_TRANS);
				colour = WindowChrome.WHITE;
			}
			p.text(truncate(p, option, rowW - 8), GameFont.SMALL, colour, true, x + 2, ry, rowW, OPTION_H,
				WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
			p.hotspot(Hotspot.Kind.FILTER_OPTION, option, x + 2, ry, rowW, OPTION_H);
		}
		g.setClip(clip);

		p.scrollTarget(ScrollTarget.FILTER);
		if (scrolls)
		{
			WindowChrome.scrollbar(p, x + listW - 2 - ScrollMath.ARROW, y + 2, viewport, content, scroll);
		}
		else
		{
			p.recordScroll(viewport, content, OPTION_H);
		}
	}

	/**
	 * Cuts an option that is too wide for its row and ends it with "...", as script 4783 does.
	 */
	static String truncate(WidgetPainter p, String text, int width)
	{
		if (p.width(text, GameFont.SMALL) <= width)
		{
			return text;
		}
		for (int end = text.length() - 1; end > 0; end--)
		{
			String cut = text.substring(0, end).trim() + "...";
			if (p.width(cut, GameFont.SMALL) <= width)
			{
				return cut;
			}
		}
		return "...";
	}

	/**
	 * The steel border of script 249 around a box: the window frame's corners and edges.
	 */
	private static void steelBorder(WidgetPainter p, int x, int y, int w, int h)
	{
		p.sprite(WindowChrome.CORNER_TOP_LEFT, x, y);
		p.sprite(WindowChrome.CORNER_TOP_RIGHT, x + w - 25, y);
		p.sprite(WindowChrome.CORNER_BOTTOM_LEFT, x, y + h - 30);
		p.sprite(WindowChrome.CORNER_BOTTOM_RIGHT, x + w - 25, y + h - 30);
		p.graphic(WindowChrome.EDGE_LEFT, x - 15, y + 30, 36, h - 60, true);
		p.graphic(WindowChrome.EDGE_RIGHT, x + w - 21, y + 30, 36, h - 60, true);
		p.graphic(WindowChrome.EDGE_TOP, x + 25, y - 15, w - 50, 36, true);
		p.graphic(WindowChrome.EDGE_BOTTOM, x + 25, y + h - 21, w - 50, 36, true);
	}
}
