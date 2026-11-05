package com.combatmasteries.ui.window;

/**
 * The parts of the Combat Achievements window every tab shares, drawn as the game's interface scripts
 * build them: the steel frame with title, menu and close buttons (scripts 228 and 4769), the points bar
 * (4782), separators, stone boxes (5845), segmented progress bars (4807), scrollbars (31) and filter
 * dropdowns (4821).
 */
public final class WindowChrome
{
	public static final int WIDTH = 480;

	// Colours used across the window
	public static final int ORANGE = 0xff981f;
	public static final int LIGHT_ORANGE = 0xff9933;
	public static final int WHITE = 0xffffff;
	public static final int GREY_TEXT = 0xdfdfdf;
	public static final int GREEN = 0x0dc10d;
	public static final int BRIGHT_GREEN = 0x00ff00;
	public static final int YELLOW = 0xffff00;
	public static final int INCOMPLETE = 0x9f9f9f;
	public static final int BAR_EMPTY = 0x9e9e9e;
	public static final int LINE_DARK = 0x0e0e0c;
	public static final int LINE_LIGHT = 0x474745;

	// Sprites
	static final int STONE = 297;
	static final int CORNER_TOP_LEFT = 310;
	static final int CORNER_TOP_RIGHT = 311;
	static final int CORNER_BOTTOM_LEFT = 312;
	static final int CORNER_BOTTOM_RIGHT = 313;
	static final int EDGE_LEFT = 172;
	static final int EDGE_RIGHT = 315;
	static final int EDGE_TOP = 314;
	static final int EDGE_BOTTOM = 173;
	static final int DIVIDER_HORIZONTAL = 2546;
	static final int DIVIDER_VERTICAL = 2561;
	static final int CLOSE = 535;
	static final int MENU = 2289;
	static final int BAR_FILL = 3391;
	static final int BAR_BACK = 3392;
	static final int BAR_TICK = 2388;
	static final int SCROLL_TRACK = 792;
	static final int SCROLL_THUMB = 790;
	static final int SCROLL_THUMB_TOP = 789;
	static final int SCROLL_THUMB_BOTTOM = 791;
	static final int SCROLL_UP = 773;
	static final int SCROLL_DOWN = 788;
	/**
	 * Stone box corners top left, top right, bottom left, bottom right, then edges left, top, right, bottom.
	 */
	static final int BOX_FIRST = 913;

	private WindowChrome()
	{
	}

	/**
	 * The steel border, stone background, title and the divider under it, the close button and the menu
	 * button.
	 */
	public static void frame(WidgetPainter p, int w, int h, String title)
	{
		p.graphic(STONE, 1, 1, w - 2, h - 2, true);
		p.text(title, GameFont.BOLD, ORANGE, true, 6, 6, w - 12, 24, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		p.sprite(CORNER_TOP_LEFT, 0, 0);
		p.sprite(CORNER_TOP_RIGHT, w - 25, 0);
		p.sprite(CORNER_BOTTOM_LEFT, 0, h - 30);
		p.sprite(CORNER_BOTTOM_RIGHT, w - 25, h - 30);
		p.graphic(EDGE_LEFT, -15, 30, 36, h - 60, true);
		p.graphic(EDGE_RIGHT, w - 21, 30, 36, h - 60, true);
		p.graphic(EDGE_TOP, 25, -15, w - 50, 36, true);
		p.graphic(EDGE_BOTTOM, 25, h - 21, w - 50, 36, true);
		p.graphic(DIVIDER_HORIZONTAL, 5, 14, w - 10, 26, true);
		p.graphic(CLOSE, w - 29, 6, 26, 23, false);
	}

	/**
	 * The menu button in the top left; drawn after the tab's content like the game does.
	 */
	public static void menuButton(WidgetPainter p)
	{
		p.sprite(MENU, 7, 7);
	}

	/**
	 * Steel divider across the window under the points bar (Tasks and Bosses).
	 */
	public static void dividerUnderBar(WidgetPainter p, int w)
	{
		p.graphic(DIVIDER_HORIZONTAL, 5, 61, w - 10, 36, true);
	}

	/**
	 * The points bar under the title.
	 *
	 * @param fill  0 to 1
	 * @param ticks tier thresholds as a share of the bar (0 to 1) to mark, as on the Rewards tab; may be null
	 */
	public static void pointsBar(WidgetPainter p, int windowWidth, String text, double fill, double[] ticks)
	{
		int w = windowWidth - 18;
		int x = (windowWidth - w) / 2;
		int y = 39;
		int inner = w - 4;
		int filled = (int) Math.min(inner, Math.round(inner * Math.max(0, fill)));
		p.outline(x + 1, y + 1, w - 2, 29, LINE_LIGHT);
		p.graphic(BAR_BACK, x + 2, y + 2, inner, 27, true);
		p.graphic(BAR_FILL, x + 2, y + 2, filled, 27, true);
		if (ticks != null)
		{
			for (double tick : ticks)
			{
				int pos = (int) (tick * inner);
				if (pos <= 2)
				{
					pos = 0;
				}
				else if (pos >= inner)
				{
					pos = inner + 3;
				}
				else
				{
					pos += 2;
				}
				p.sprite(BAR_TICK, x + pos - 4, y);
			}
		}
		p.text(text, GameFont.REGULAR, WHITE, true, x, y, w, 33, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		p.outline(x, y, w, 31, LINE_DARK);
	}

	/**
	 * A raised stone box: tiled stone with bevelled corners and edges.
	 */
	public static void stoneBox(WidgetPainter p, int x, int y, int w, int h)
	{
		p.graphic(STONE, x, y, w, h, true);
		p.graphic(BOX_FIRST, x, y, 9, 9, true);
		p.graphic(BOX_FIRST + 1, x + w - 9, y, 9, 9, true);
		p.graphic(BOX_FIRST + 2, x, y + h - 9, 9, 9, true);
		p.graphic(BOX_FIRST + 3, x + w - 9, y + h - 9, 9, 9, true);
		p.graphic(BOX_FIRST + 4, x, y + 9, 9, h - 18, true);
		p.graphic(BOX_FIRST + 5, x + 9, y, w - 18, 9, true);
		p.graphic(BOX_FIRST + 6, x + w - 9, y + 9, 9, h - 18, true);
		p.graphic(BOX_FIRST + 7, x + 9, y + h - 9, w - 18, 9, true);
	}

	/**
	 * The "done/total" count over a bar of four segments, as in the tier boxes and boss cards.
	 *
	 * @param textOffset how far below {@code y} the count sits
	 */
	public static void segmentedBar(WidgetPainter p, int done, int total, int x, int y, int w, int textOffset)
	{
		int segments = 4;
		int segment = w / segments;
		int colour = done == total ? GREEN : done > 0 ? YELLOW : BAR_EMPTY;
		int remaining = 1 + done * (w - 1) / Math.max(1, total);

		p.text(done + "/" + total, GameFont.REGULAR, colour, true, x, y + textOffset, w, 12,
			WidgetPainter.CENTRE, WidgetPainter.TOP, 0);
		int barY = y + 17;
		p.fill(x, barY, w, 7, 0x000000, 150);
		for (int i = 0; i < segments; i++)
		{
			p.outline(x + i * segment, barY, segment + 1, 7, 0x000000);
		}
		for (int i = 0; i < segments && remaining > 0; i++)
		{
			int width;
			if (remaining > segment)
			{
				width = segment - 1;
				remaining -= segment;
			}
			else
			{
				width = remaining - 1;
				remaining = 0;
			}
			p.fill(x + i * segment + 1, y + 18, width, 5, colour, 0);
		}
	}

	/**
	 * A scrollbar: arrows, stretched track and a thumb sized to the visible share.
	 */
	public static void scrollbar(WidgetPainter p, int x, int y, int h, int contentHeight, int scroll)
	{
		p.graphic(SCROLL_TRACK, x, y + 16, 16, h - 32, false);
		int track = h - 32;
		int content = Math.max(contentHeight, h);
		int thumb = Math.max(10, track * h / content);
		int maxScroll = content - h;
		int thumbY = y + 16 + (maxScroll <= 0 ? 0 : (track - thumb) * scroll / maxScroll);
		p.graphic(SCROLL_THUMB, x, thumbY, 16, thumb, false);
		p.graphic(SCROLL_THUMB_TOP, x, thumbY, 16, 5, false);
		p.graphic(SCROLL_THUMB_BOTTOM, x, thumbY + thumb - 5, 16, 5, false);
		p.sprite(SCROLL_UP, x, y);
		p.sprite(SCROLL_DOWN, x, y + h - 16);
	}

	/**
	 * A filter dropdown (label above, value box with a down arrow), in a 110x39 box.
	 */
	public static void dropdown(WidgetPainter p, int x, int y, String label, String value)
	{
		int w = 110;
		p.graphic(STONE, x, y + 19, w, 20, true);
		p.outline(x, y + 19, w, 20, LINE_DARK);
		p.outline(x + 1, y + 20, w - 2, 18, LINE_LIGHT);
		p.sprite(SCROLL_DOWN, x + w - 18, y + 21);
		p.text(value, GameFont.SMALL, ORANGE, true, x + 2, y + 21, w - 20, 16, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		p.text(label + ":", GameFont.REGULAR, ORANGE, true, x + 2, y, w - 3, 18, WidgetPainter.LEFT, WidgetPainter.MIDDLE, 0);
	}
}
