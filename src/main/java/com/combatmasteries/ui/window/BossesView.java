package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Bosses tab (interface 716, scripts 4791-4800): a search box and a three-column grid of stone cards,
 * one per content, with "Tasks: N" where Combat Achievements shows the boss's level.
 */
final class BossesView
{
	static final int LIST_X = 5;
	static final int LIST_Y = 82;
	static final int CARD_W = 145;
	static final int CARD_H = 88;
	static final int GAP = 5;
	static final int COLUMNS = 3;

	private static final int SEARCH_LEFT = 1123;
	private static final int SEARCH_MIDDLE = 1124;
	private static final int SEARCH_RIGHT = 1125;

	/**
	 * Shorter names for the cards whose full name does not fit on one line, as no Combat Achievements boss
	 * name wraps.
	 */
	private static final Map<String, String> SHORT_NAMES = new HashMap<>();

	static
	{
		SHORT_NAMES.put("Theatre of Blood: Hard Mode", "ToB: Hard Mode");
		SHORT_NAMES.put("Chambers of Xeric: CM", "CoX: Challenge Mode");
	}

	private BossesView()
	{
	}

	static void draw(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		WindowChrome.dividerUnderBar(p, w);

		// Search bar: label, then a 373x30 box
		p.text("Search:", GameFont.REGULAR, WindowChrome.ORANGE, 0, 35, 85, 40, WidgetPainter.CENTRE);
		int sx = 85;
		int sy = 40;
		p.graphic(SEARCH_MIDDLE, sx + 4, sy, 365, 30, false);
		p.graphic(SEARCH_LEFT, sx, sy, 4, 30, false);
		p.graphic(SEARCH_RIGHT, sx + 369, sy, 4, 30, false);
		p.text(state.getSearch() + "*", GameFont.REGULAR, WindowChrome.ORANGE, 90, 35 + 9, w - 111, 22, WidgetPainter.LEFT);

		List<WindowModel.Content> contents = state.search(model.getContents());
		int listW = w - 10;
		int listH = h - 88;
		int areaX = LIST_X;
		int areaY = LIST_Y + 2;
		int areaH = listH - 4;

		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(areaX, areaY, listW - 16, areaH);
		int rowY = 0;
		for (int i = 0; i < contents.size(); i++)
		{
			if (i > 0 && i % COLUMNS == 0)
			{
				rowY += CARD_H + GAP;
			}
			int x = areaX + 5 + (i % COLUMNS) * (CARD_W + GAP);
			int y = areaY + rowY - state.getBossesScroll();
			drawCard(p, contents.get(i), x, y);
		}
		g.setClip(clip);
		int contentH = contents.isEmpty() ? 0 : rowY + CARD_H + GAP;
		if (contents.isEmpty())
		{
			p.text("There are no bosses available to you<br>that match your search.", GameFont.REGULAR, WindowChrome.ORANGE, true,
				areaX, areaY, listW - 16, areaH, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		}

		WindowChrome.scrollbar(p, LIST_X + listW - 16 - 1, LIST_Y, listH, contentH, state.getBossesScroll());
	}

	private static void drawCard(WidgetPainter p, WindowModel.Content content, int x, int y)
	{
		boolean done = content.getTasks() > 0 && content.getCompleted() == content.getTasks();
		WindowChrome.stoneBox(p, x, y, CARD_W, CARD_H);
		p.outline(x, y, CARD_W, CARD_H, 0x000000, 50);
		String name = content.getName();
		if (p.width(name, GameFont.BOLD) > CARD_W - 6)
		{
			name = SHORT_NAMES.getOrDefault(name, name);
		}
		p.text(name, GameFont.BOLD, done ? WindowChrome.GREEN : WindowChrome.LIGHT_ORANGE, true,
			x + 3, y + 9, CARD_W - 6, 24, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		p.text("<col=ff9933>Tasks:</col><col=ffffff> " + content.getTasks() + "</col>", GameFont.SMALL, WindowChrome.WHITE, true,
			x, y + 32, CARD_W, 24, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		if (content.getTasks() > 0)
		{
			WindowChrome.segmentedBar(p, content.getCompleted(), content.getTasks(), x + 8, y + 53, CARD_W - 16, 0);
		}
	}
}
