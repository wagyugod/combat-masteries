package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Bosses tab (interface 716, scripts 4791-4800): a search box and a three-column grid of stone cards,
 * one per content, with the boss level under the name as Combat Achievements shows it.
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
	 * The green of a completed card's name and count, and the orange of its "Level:" label; a shade off the
	 * shared colours, as the reference Bosses tab shows them.
	 */
	private static final int CARD_GREEN = 0x05c408;
	private static final int LEVEL_ORANGE = 0xff9d34;

	/**
	 * The names the Combat Achievements Bosses tab uses for each content. Long names break
	 * after the colon onto a second line, as the game does ("Chambers of Xeric:" /
	 * "Challenge Mode"); one-line names sit centred in the same two-line space.
	 */
	private static final Map<String, String> CARD_NAMES = new HashMap<>();

	static
	{
		CARD_NAMES.put("Inferno", "TzKal-Zuk");
		CARD_NAMES.put("Colosseum", "Fortis Colosseum");
		CARD_NAMES.put("Theatre of Blood: Hard Mode", "Theatre of Blood:<br>Hard Mode");
		CARD_NAMES.put("Chambers of Xeric: CM", "Chambers of Xeric:<br>Challenge Mode");
		CARD_NAMES.put("Tombs of Amascut", "Tombs of Amascut:<br>Expert Mode");
		CARD_NAMES.put("Yama Contracts", "Yama");
	}

	/**
	 * Combat level shown under each card's name, as the Combat Achievements Bosses tab does.
	 * Doom shows its deep-delve level; the awakened card shows two of the bosses' levels.
	 */
	private static final Map<String, String> CARD_LEVELS = new HashMap<>();

	static
	{
		CARD_LEVELS.put("Inferno", "1400");
		CARD_LEVELS.put("Colosseum", "1563");
		CARD_LEVELS.put("Theatre of Blood", "1520");
		CARD_LEVELS.put("Theatre of Blood: Hard Mode", "1520");
		CARD_LEVELS.put("Chambers of Xeric: CM", "1043");
		CARD_LEVELS.put("Tombs of Amascut", "544");
		CARD_LEVELS.put("Doom of Mokhaiotl", "908");
		CARD_LEVELS.put("Awakened Bosses", "1157 / 1136");
		CARD_LEVELS.put("Yama Contracts", "1238");
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
		p.hotspot(Hotspot.Kind.SEARCH, null, sx, sy, 373, 30);
		p.text(state.getSearch() + "*", GameFont.REGULAR, WindowChrome.ORANGE, 90, 35 + 9, w - 111, 22, WidgetPainter.LEFT);

		List<WindowModel.Content> contents = sortedByCardName(state.search(model.getContents()));
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

	/**
	 * Cards are listed alphabetically by the name shown on them, ignoring a leading "The".
	 */
	static List<WindowModel.Content> sortedByCardName(List<WindowModel.Content> contents)
	{
		List<WindowModel.Content> sorted = new ArrayList<>(contents);
		sorted.sort(Comparator.comparing(c -> sortKey(c.getName())));
		return sorted;
	}

	static String sortKey(String content)
	{
		String name = CARD_NAMES.getOrDefault(content, content).replace("<br>", " ").toLowerCase(Locale.ROOT);
		return name.startsWith("the ") ? name.substring(4) : name;
	}

	private static void drawCard(WidgetPainter p, WindowModel.Content content, int x, int y)
	{
		boolean done = content.getTasks() > 0 && content.getCompleted() == content.getTasks();
		WindowChrome.stoneBox(p, x, y, CARD_W, CARD_H);
		p.outline(x, y, CARD_W, CARD_H, 0x000000, 50);
		String name = CARD_NAMES.getOrDefault(content.getName(), content.getName());
		// A one-line name has its cap-top 15px below the card's top; two lines centre on the same spot
		p.text(name, GameFont.BOLD, done ? CARD_GREEN : WindowChrome.LIGHT_ORANGE, true,
			x + 3, y + 6, CARD_W - 6, 30, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 13);
		String level = CARD_LEVELS.getOrDefault(content.getName(), "-");
		p.text("<col=" + Integer.toHexString(LEVEL_ORANGE) + ">Level:</col><col=ffffff> " + level + "</col>", GameFont.SMALL, WindowChrome.WHITE, true,
			x, y + 32, CARD_W, 24, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		// Every card shows its count and bar, even a content with no tasks yet ("0/0", all dark)
		WindowChrome.segmentedBar(p, content.getCompleted(), content.getTasks(), x + 8, y + 53, CARD_W - 16, 0, CARD_GREEN);
	}
}
