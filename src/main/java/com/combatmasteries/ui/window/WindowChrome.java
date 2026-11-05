package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.runelite.api.gameval.SpriteID;

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
	/**
	 * The points bar's frame. Script 4782 in the cache dump uses the line colours above, but the current game
	 * (the reference screenshots) draws a light outer and a dark inner line.
	 */
	static final int BAR_OUTER_LINE = 0x746652;
	/**
	 * A faint brown line just inside the frame's left and bottom edges, as the current game shows.
	 */
	private static final int INNER_SHADE = 0x4c3514;
	private static final int INNER_SHADE_TRANS = 230;

	/**
	 * The points bar's gradients, top to bottom, as the current game draws them (the reference screenshots).
	 * They are close to sprites 3391 (fill) and 3392 (track) but not the same: each pair of rows differs a
	 * little, and the first filled and last empty columns are lighter.
	 */
	private static final int BAR_HEIGHT = 27;
	private static final int[] BAR_FILL_ROWS = {
		0x0e4d21, 0x125e29, 0x13622c, 0x167532, 0x187d37, 0x1c8e3e, 0x1b8b3d, 0x1e9d44, 0x1fa346, 0x20a948,
		0x22b84f, 0x24c153, 0x26cb57, 0x28d05a, 0x26cb57, 0x24c153, 0x22b84f, 0x20a948, 0x1fa346, 0x1e9d44,
		0x1a883c, 0x1b8b3d, 0x187d37, 0x167532, 0x13622c, 0x125e29, 0x0e4d21,
	};
	private static final int[] BAR_FILL_FIRST_COLUMN = {
		0x0e5121, 0x0f6729, 0x116f2e, 0x138335, 0x168b3b, 0x189b43, 0x1b9c42, 0x1bab49, 0x1eb74d, 0x1fc150,
		0x20cb57, 0x22d15a, 0x24d35b, 0x27d35b, 0x24d35b, 0x22cc58, 0x20c353, 0x1eb84f, 0x1cac49, 0x1ba446,
		0x189b42, 0x178d3e, 0x138b3b, 0x127f35, 0x0f6f2e, 0x0f6429, 0x0e5121,
	};
	private static final int[] BAR_TRACK_ROWS = {
		0x2d2c2e, 0x343434, 0x333333, 0x3d3d3d, 0x3b3b3b, 0x454545, 0x434343, 0x4d4d4d, 0x4b4b4b, 0x575757,
		0x555555, 0x616161, 0x5f5f5f, 0x636363, 0x5e5e5e, 0x5f5f5f, 0x535353, 0x555555, 0x494949, 0x4b4b4b,
		0x414141, 0x434343, 0x393939, 0x3b3b3b, 0x313131, 0x333333, 0x2d2c2e,
	};
	private static final int[] BAR_TRACK_LAST_COLUMN = {
		0x2d2d2f, 0x353537, 0x333435, 0x3e3f41, 0x3d3d3f, 0x48494a, 0x464749, 0x515254, 0x505052, 0x5e5e5f,
		0x5c5c5e, 0x646464, 0x636464, 0x646464, 0x636464, 0x606060, 0x5c5c5e, 0x565656, 0x505052, 0x4c4c4c,
		0x464749, 0x434444, 0x3d3d3f, 0x3b3b3c, 0x333435, 0x333333, 0x2d2d2f,
	};
	/**
	 * The dark line inside the bar's frame: along the fill, along the track, and its two end columns top to
	 * bottom (the frame's 29 inner rows).
	 */
	private static final int BAR_INNER_LINE_FILL = 0x272920;
	private static final int BAR_INNER_LINE_FILL_FIRST = 0x242a20;
	private static final int BAR_INNER_LINE_TRACK = 0x2c2a22;
	private static final int[] BAR_INNER_LINE_LEFT = {
		0x1d2920, 0x232920, 0x262920, 0x262922, 0x252822, 0x252822, 0x252822, 0x242821, 0x242821, 0x232721,
		0x232721, 0x232721, 0x222721, 0x242721, 0x242721, 0x222721, 0x222721, 0x232721, 0x232721, 0x232721,
		0x242821, 0x242821, 0x252822, 0x252822, 0x252822, 0x262920, 0x262920, 0x232920, 0x1d2920,
	};
	private static final int[] BAR_INNER_LINE_RIGHT = {
		0x2b2921, 0x2c2a22, 0x2c2a22, 0x2c2a22, 0x2c2a22, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921,
		0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921,
		0x2c2921, 0x2c2921, 0x2c2921, 0x2c2921, 0x2c2a22, 0x2c2a22, 0x2c2a22, 0x2c2a22, 0x2b2921,
	};

	/**
	 * The Rewards tab's points bar (the one with tier markers) keeps the dark frame of script 4782 and has no
	 * shadow around it: a near-black outer line with a dark brown line inside it, reddish down the left end.
	 * The colours are the reference screenshot's.
	 */
	private static final int REWARDS_BAR_OUTER_LINE = 0x0c0c0b;
	private static final int REWARDS_BAR_TOP_FILL = 0x2f2b28;
	private static final int REWARDS_BAR_TOP_FILL_FIRST = 0x2f2e28;
	private static final int REWARDS_BAR_TOP_TRACK = 0x333026;
	private static final int REWARDS_BAR_TOP_JOIN = 0x332d27;
	private static final int REWARDS_BAR_BOTTOM_FILL = 0x3a2b28;
	private static final int REWARDS_BAR_BOTTOM_FILL_FIRST = 0x372e28;
	private static final int REWARDS_BAR_BOTTOM_TRACK = 0x353026;
	private static final int REWARDS_BAR_BOTTOM_JOIN_FILL = 0x382d27;
	private static final int REWARDS_BAR_BOTTOM_JOIN_TRACK = 0x372d27;
	/**
	 * The faint light line just outside the Rewards bar's frame, lighter still above the tier markers.
	 */
	private static final int REWARDS_BAR_GLOW = 0x4a4135;
	private static final int REWARDS_BAR_GLOW_TICK = 0x50473b;
	private static final int[] REWARDS_BAR_LEFT = {
		0x3b312a, 0x372c27, 0x3a2827, 0x392526, 0x392325, 0x392023, 0x381f23, 0x381c22, 0x381b21, 0x371921,
		0x371720, 0x37151f, 0x36131e, 0x36111d, 0x36111d, 0x36111d, 0x36131e, 0x37151f, 0x371720, 0x371921,
		0x381b21, 0x381c22, 0x381f23, 0x392023, 0x392325, 0x392526, 0x3a2827, 0x372c27, 0x3b312a,
	};
	private static final int[] REWARDS_BAR_RIGHT = {
		0x333026, 0x352f25, 0x342f25, 0x342e24, 0x332e24, 0x312d22, 0x312d22, 0x2f2b20, 0x2f2b20, 0x2e291f,
		0x2e291f, 0x2c271d, 0x2c271d, 0x2a261b, 0x2b261c, 0x2a261b, 0x2c271d, 0x2c271d, 0x2e291f, 0x2e291f,
		0x2f2b20, 0x2f2b20, 0x312d22, 0x312d22, 0x332e24, 0x342e24, 0x342f25, 0x352f25, 0x353026,
	};
	/**
	 * Rows of a tier marker that, where the marker runs into the bar's right edge, darken the frame's last
	 * column: its two ends, but not the stem between them.
	 */
	private static final int TICK_STEM_FIRST_ROW = 6;
	private static final int TICK_STEM_LAST_ROW = 23;
	private static final int TICK_HEIGHT = 31;

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
	static final int BAR_TICK = 2388;
	private static final int BAR_TICK_WIDTH = 10;
	/**
	 * The classic arrows. The game's scripts ask for 773 and 788, which the reference client (RuneLite's 2005
	 * interface style) swaps for these; the dropdowns use the same down arrow.
	 */
	static final int SCROLL_UP = 801;
	static final int SCROLLBAR_WIDTH = 16;
	static final int SCROLL_DOWN = 802;
	private static final int SCROLL_TRACK_COLOUR = 0x272520;
	private static final int SCROLL_THUMB_COLOUR = 0x4d4437;
	private static final int SCROLL_LIGHT = 0x756756;
	private static final int SCROLL_DARK = 0x342f28;
	/**
	 * Stone box corners top left, top right, bottom left, bottom right, then edges left, top, right, bottom.
	 */
	static final int BOX_FIRST = 913;
	/**
	 * A hovered stone box (script 2978): darker stone and lit bevels, in the same order.
	 */
	static final int STONE_HOVERED = SpriteID.TRADEBACKING_DARK;
	static final int BOX_HOVERED_FIRST = SpriteID.V2StoneButtonIn.A_TOP_LEFT;
	/**
	 * A small stone button with a metal rim, as plugins add to the game's title bars (and as the Combat
	 * Achievements window's "Masteries" button): stone, then corners top left, top right, bottom left,
	 * bottom right, then edges left, top, right, bottom; and the darker stone and lit rim while hovered.
	 */
	static final int[] BUTTON = {
		SpriteID.TRADEBACKING,
		SpriteID.V2StoneButtonOut.A_TOP_LEFT, SpriteID.V2StoneButtonOut.A_TOP_RIGHT,
		SpriteID.V2StoneButtonOut.A_BOTTOM_LEFT, SpriteID.V2StoneButtonOut.A_BOTTOM_RIGHT,
		SpriteID.V2StoneButtonOut.A_MAP_EDGE_LEFT, SpriteID.V2StoneButtonOut.A_MAP_EDGE_TOP,
		SpriteID.V2StoneButtonOut.A_MAP_EDGE_RIGHT, SpriteID.V2StoneButtonOut.A_MAP_EDGE_BOTTOM,
	};
	static final int[] BUTTON_HOVERED = {
		SpriteID.TRADEBACKING_DARK,
		SpriteID.V2StoneButtonIn.A_TOP_LEFT, SpriteID.V2StoneButtonIn.A_TOP_RIGHT,
		SpriteID.V2StoneButtonIn.A_BOTTOM_LEFT, SpriteID.V2StoneButtonIn.A_BOTTOM_RIGHT,
		SpriteID.V2StoneButtonIn.A_LEFT, SpriteID.V2StoneButtonIn.A_TOP,
		SpriteID.V2StoneButtonIn.A_RIGHT, SpriteID.V2StoneButtonIn.A_BOTTOM,
	};
	/**
	 * The screenshot button: as tall as the close button's sprite (21, from y 7) and 4 left of it, the gap
	 * the "Masteries" button leaves in the Combat Achievements title bar.
	 */
	static final int SCREENSHOT_W = 25;
	static final int SCREENSHOT_H = 21;
	static final int SCREENSHOT_Y = 7;
	static final int SCREENSHOT_RIGHT = 32;
	/**
	 * The label colours of the "Masteries" button, used to tint the camera at rest and while hovered.
	 */
	private static final int BUTTON_TEXT = 0xd6d6d6;

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
		// The faint shade the game shows along the stone's inner left and bottom edges
		p.fill(6, 30, 1, h - 37, INNER_SHADE, INNER_SHADE_TRANS);
		p.fill(6, h - 7, w - 12, 1, INNER_SHADE, INNER_SHADE_TRANS);
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
		p.hotspot(Hotspot.Kind.CLOSE, null, w - 29, 6, 26, 23);
	}

	/**
	 * The screenshot button in the title bar, just left of the close button: RuneLite's camera icon on a
	 * small stone button that lights up while hovered.
	 */
	public static void screenshotButton(WidgetPainter p, int w, boolean hovered)
	{
		int x = w - SCREENSHOT_RIGHT - SCREENSHOT_W;
		stoneButton(p, x, SCREENSHOT_Y, SCREENSHOT_W, SCREENSHOT_H, hovered);
		BufferedImage icon = hovered ? Camera.HOVERED : Camera.IDLE;
		if (icon != null)
		{
			p.image(icon, x + (SCREENSHOT_W - icon.getWidth()) / 2, SCREENSHOT_Y + (SCREENSHOT_H - icon.getHeight() + 1) / 2);
		}
		p.hotspot(Hotspot.Kind.SCREENSHOT, null, x, SCREENSHOT_Y, SCREENSHOT_W, SCREENSHOT_H);
	}

	/**
	 * A small stone button with a metal rim; the stone stays inside the rim's rounded corners, as in the
	 * game's own title bar buttons (script 134).
	 */
	static void stoneButton(WidgetPainter p, int x, int y, int w, int h, boolean hovered)
	{
		int[] s = hovered ? BUTTON_HOVERED : BUTTON;
		p.graphic(s[0], x + 1, y + 1, w - 2, h - 2, true);
		p.graphic(s[1], x, y, 9, 9, true);
		p.graphic(s[2], x + w - 9, y, 9, 9, true);
		p.graphic(s[3], x, y + h - 9, 9, 9, true);
		p.graphic(s[4], x + w - 9, y + h - 9, 9, 9, true);
		p.graphic(s[5], x, y + 9, 9, h - 18, true);
		p.graphic(s[6], x + 9, y, w - 18, 9, true);
		p.graphic(s[7], x + w - 9, y + 9, 9, h - 18, true);
		p.graphic(s[8], x + 9, y + h - 9, w - 18, 9, true);
	}

	/**
	 * RuneLite's screenshot camera (from its Screenshot plugin, BSD-2-Clause), white while hovered and
	 * tinted like the button labels at rest. Loaded on first use.
	 */
	private static final class Camera
	{
		@Nullable
		static final BufferedImage HOVERED = load();
		@Nullable
		static final BufferedImage IDLE = tint(HOVERED, BUTTON_TEXT);

		@Nullable
		private static BufferedImage load()
		{
			try (InputStream in = WindowChrome.class.getResourceAsStream("/com/combatmasteries/screenshot.png"))
			{
				return in == null ? null : ImageIO.read(in);
			}
			catch (IOException e)
			{
				return null;
			}
		}

		@Nullable
		private static BufferedImage tint(@Nullable BufferedImage img, int rgb)
		{
			if (img == null)
			{
				return null;
			}
			BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < img.getHeight(); y++)
			{
				for (int x = 0; x < img.getWidth(); x++)
				{
					out.setRGB(x, y, img.getRGB(x, y) & 0xff000000 | rgb);
				}
			}
			return out;
		}
	}

	/**
	 * The menu button in the top left; drawn after the tab's content like the game does.
	 */
	public static void menuButton(WidgetPainter p)
	{
		p.sprite(MENU, 7, 7);
		p.hotspot(Hotspot.Kind.MENU, null, 7, 7, 21, 21);
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
	 * @param ticks tier thresholds as a share of the bar (0 to 1) to mark, as on the Rewards tab, whose bar
	 *              also has its own darker frame; null for the other tabs' bar
	 */
	public static void pointsBar(WidgetPainter p, int windowWidth, String text, double fill, double[] ticks)
	{
		int w = windowWidth - 18;
		int x = (windowWidth - w) / 2;
		int y = 39;
		int inner = w - 4;
		int filled = (int) Math.min(inner, Math.round(inner * Math.max(0, fill)));
		boolean rewards = ticks != null;
		if (rewards)
		{
			rewardsBarInnerLine(p, x + 1, y + 1, w - 2, 29, filled);
		}
		else
		{
			barShadow(p, x, y, w, 31);
			barInnerLine(p, x + 1, y + 1, w - 2, 29, filled);
		}
		for (int i = 0; i < BAR_HEIGHT; i++)
		{
			int row = y + 2 + i;
			p.hline(x + 2, row, filled, BAR_FILL_ROWS[i]);
			p.hline(x + 2 + filled, row, inner - filled, BAR_TRACK_ROWS[i]);
			if (filled > 0)
			{
				p.hline(x + 2, row, 1, BAR_FILL_FIRST_COLUMN[i]);
			}
			if (filled < inner)
			{
				p.hline(x + 1 + inner, row, 1, BAR_TRACK_LAST_COLUMN[i]);
			}
		}
		if (rewards)
		{
			p.outline(x - 1, y - 1, w + 2, 33, REWARDS_BAR_GLOW);
			p.outline(x, y, w, 31, REWARDS_BAR_OUTER_LINE);
			for (double tick : ticks)
			{
				barTick(p, x, y, w, inner, tick);
			}
		}
		p.text(text, GameFont.REGULAR, WHITE, true, x, y, w, 33, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		if (!rewards)
		{
			p.outline(x, y, w, 31, BAR_OUTER_LINE);
		}
	}

	/**
	 * One tier marker (script 4782): placed on the threshold, or at either end when it is within 2px of it,
	 * over the bar's top and bottom lines. The marker on the bar's last pixel is cut off by the frame's right
	 * column, which the game shows dark beside the marker's ends.
	 */
	private static void barTick(WidgetPainter p, int x, int y, int w, int inner, double tick)
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
		int left = x + pos - 4;
		int right = x + w - 1;
		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		// The marker stays between the frame's top and bottom lines and left of its right column
		g.clipRect(x, y + 1, w - 1, TICK_HEIGHT - 2);
		p.sprite(BAR_TICK, left, y);
		g.setClip(clip);
		g.clipRect(x, y - 1, w - 1, TICK_HEIGHT + 2);
		// The game shows the marker's caps on the frame lines as two dark runs either side of the middle,
		// a little wider than the marker, with the glow above lighter over it
		p.hline(left - 1, y, 5, 0x000000);
		p.hline(left + 7, y, 5, 0x000000);
		p.hline(left - 1, y + TICK_HEIGHT - 1, 4, 0x000000);
		p.hline(left + 8, y + TICK_HEIGHT - 1, 4, 0x000000);
		p.hline(left, y - 1, BAR_TICK_WIDTH + 1, REWARDS_BAR_GLOW_TICK);
		g.setClip(clip);
		if (left + BAR_TICK_WIDTH > right)
		{
			for (int row = 1; row < TICK_HEIGHT - 1; row++)
			{
				if (row < TICK_STEM_FIRST_ROW || row > TICK_STEM_LAST_ROW)
				{
					p.hline(right, y + row, 1, 0x000000);
				}
			}
		}
	}

	/**
	 * The Rewards bar's line inside its frame: brown along the fill, greyer along the track, with a short
	 * blend where they meet and reddish end columns.
	 */
	private static void rewardsBarInnerLine(WidgetPainter p, int x, int y, int w, int h, int filled)
	{
		int bottom = y + h - 1;
		p.hline(x, y, w, REWARDS_BAR_TOP_TRACK);
		p.hline(x, bottom, w, REWARDS_BAR_BOTTOM_TRACK);
		if (filled > 0)
		{
			p.hline(x + 1, y, filled, REWARDS_BAR_TOP_FILL);
			p.hline(x + 1, bottom, filled, REWARDS_BAR_BOTTOM_FILL);
			p.hline(x + 1, y, 1, REWARDS_BAR_TOP_FILL_FIRST);
			p.hline(x + 1, bottom, 1, REWARDS_BAR_BOTTOM_FILL_FIRST);
			if (filled < w - 2)
			{
				p.hline(x + filled, y, 2, REWARDS_BAR_TOP_JOIN);
				p.hline(x + filled, bottom, 1, REWARDS_BAR_BOTTOM_JOIN_FILL);
				p.hline(x + filled + 1, bottom, 1, REWARDS_BAR_BOTTOM_JOIN_TRACK);
			}
		}
		for (int i = 0; i < h; i++)
		{
			p.hline(x, y + i, 1, REWARDS_BAR_LEFT[i]);
			p.hline(x + w - 1, y + i, 1, REWARDS_BAR_RIGHT[i]);
		}
	}

	/**
	 * The dark line just inside the bar's frame: a shade greener where it runs along the filled part, with
	 * its own left and right columns.
	 */
	private static void barInnerLine(WidgetPainter p, int x, int y, int w, int h, int filled)
	{
		for (int edge : new int[]{y, y + h - 1})
		{
			p.hline(x, edge, filled + 1, BAR_INNER_LINE_FILL);
			p.hline(x + 1 + filled, edge, w - 1 - filled, BAR_INNER_LINE_TRACK);
			if (filled > 0)
			{
				p.hline(x + 1, edge, 1, BAR_INNER_LINE_FILL_FIRST);
			}
		}
		for (int i = 0; i < h; i++)
		{
			p.hline(x, y + i, 1, BAR_INNER_LINE_LEFT[i]);
			p.hline(x + w - 1, y + i, 1, BAR_INNER_LINE_RIGHT[i]);
		}
	}

	/**
	 * The soft shadow around the bar's frame: dark above and at the sides, fading at the top corners and
	 * barely there underneath.
	 */
	private static void barShadow(WidgetPainter p, int x, int y, int w, int h)
	{
		p.fill(x + 1, y - 1, w - 2, 1, 0x000000, 230);
		p.fill(x - 1, y + 1, 1, h - 2, 0x000000, 230);
		p.fill(x + w, y + 1, 1, h - 2, 0x000000, 230);
		p.fill(x - 1, y - 1, 2, 1, 0x000000, 243);
		p.fill(x + w - 1, y - 1, 2, 1, 0x000000, 243);
		p.fill(x - 1, y, 1, 1, 0x000000, 243);
		p.fill(x + w, y, 1, 1, 0x000000, 243);
		p.fill(x - 1, y + h - 1, 1, 1, 0x000000, 243);
		p.fill(x + w, y + h - 1, 1, 1, 0x000000, 243);
		p.fill(x + 1, y + h, w, 1, 0x000000, 254);
	}

	/**
	 * A raised stone box: tiled stone with bevelled corners and edges.
	 */
	public static void stoneBox(WidgetPainter p, int x, int y, int w, int h)
	{
		stoneBox(p, x, y, w, h, false);
	}

	/**
	 * A raised stone box, lit up while {@code hovered} the way the game's clickable stone boxes are
	 * (scripts 5845 and 2978).
	 */
	public static void stoneBox(WidgetPainter p, int x, int y, int w, int h, boolean hovered)
	{
		int box = hovered ? BOX_HOVERED_FIRST : BOX_FIRST;
		p.graphic(hovered ? STONE_HOVERED : STONE, x, y, w, h, true);
		p.graphic(box, x, y, 9, 9, true);
		p.graphic(box + 1, x + w - 9, y, 9, 9, true);
		p.graphic(box + 2, x, y + h - 9, 9, 9, true);
		p.graphic(box + 3, x + w - 9, y + h - 9, 9, 9, true);
		p.graphic(box + 4, x, y + 9, 9, h - 18, true);
		p.graphic(box + 5, x + 9, y, w - 18, 9, true);
		p.graphic(box + 6, x + w - 9, y + 9, 9, h - 18, true);
		p.graphic(box + 7, x + 9, y + h - 9, w - 18, 9, true);
	}

	/**
	 * The "done/total" count over a bar of four segments, as in the tier boxes and boss cards.
	 *
	 * @param textOffset how far below {@code y} the count sits
	 */
	public static void segmentedBar(WidgetPainter p, int done, int total, int x, int y, int w, int textOffset)
	{
		segmentedBar(p, done, total, x, y, w, textOffset, GREEN);
	}

	/**
	 * The "done/total" count over a bar of four segments, drawing a complete count in {@code doneTextColour}
	 * (the bar itself stays {@link #GREEN}). An empty total shows "0/0" in grey over an all-dark bar.
	 */
	public static void segmentedBar(WidgetPainter p, int done, int total, int x, int y, int w, int textOffset,
		int doneTextColour)
	{
		int segments = 4;
		int segment = w / segments;
		boolean complete = total > 0 && done >= total;
		int colour = complete ? GREEN : done > 0 ? YELLOW : BAR_EMPTY;
		int remaining = total <= 0 ? 0 : 1 + Math.min(done, total) * (w - 1) / total;

		p.text(done + "/" + total, GameFont.REGULAR, complete ? doneTextColour : colour, true, x, y + textOffset, w, 12,
			WidgetPainter.CENTRE, WidgetPainter.TOP, 0);
		int barY = y + 17;
		p.fill(x, barY, w, 7, 0x000000, 150);
		for (int i = 0; i < segments; i++)
		{
			p.outline(x + i * segment, barY, segment + 1, 7, 0x000000);
		}
		int filled = remaining - 1;
		if (filled > 0)
		{
			// The fill's glow on the stone: a faint tinted row above the filled part, and down the right
			// side once the bar is full
			p.fill(x + 1, barY - 1, filled, 1, glow(colour), BAR_GLOW_TRANS);
			if (complete)
			{
				p.fill(x + segments * segment + 1, barY + 1, 1, 5, glow(colour), BAR_GLOW_TRANS);
			}
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
			segmentFill(p, x + i * segment + 1, barY + 1, width, colour);
		}
	}

	/**
	 * How strongly a bar's glow tints the stone around it.
	 */
	private static final int BAR_GLOW_TRANS = 230;

	/**
	 * The colour that, blended at {@link #BAR_GLOW_TRANS} over the stone, adds a little of the bar colour to it.
	 */
	private static int glow(int rgb)
	{
		int r = Math.min(255, 0x48 + ((rgb >> 16) & 0xff) * 6 / 10);
		int g = Math.min(255, 0x3e + ((rgb >> 8) & 0xff) * 6 / 10);
		int b = Math.min(255, 0x33 + (rgb & 0xff) * 6 / 10);
		return r << 16 | g << 8 | b;
	}

	/**
	 * One segment's fill, five rows tall: the body colour with a lighter top and bottom row and lighter end
	 * columns, as the current game shades it.
	 */
	private static void segmentFill(WidgetPainter p, int x, int y, int w, int rgb)
	{
		if (w <= 0)
		{
			return;
		}
		p.fill(x, y + 1, w, 3, rgb, 0);
		p.hline(x, y, w, lighten(rgb, 0, 4, 0));
		p.hline(x, y + 4, w, lighten(rgb, 3, 4, 3));
		for (int cx : w > 1 ? new int[]{x, x + w - 1} : new int[]{x})
		{
			p.fill(cx, y + 1, 1, 3, lighten(rgb, 0, 4, 0), 0);
			p.hline(cx, y, 1, lighten(rgb, 0, 6, 0));
			p.hline(cx, y + 4, 1, lighten(rgb, 4, 6, 4));
		}
	}

	/**
	 * Adds to each channel that is not zero, so a saturated colour such as yellow keeps its exact value.
	 */
	private static int lighten(int rgb, int dr, int dg, int db)
	{
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;
		r = r == 0 ? 0 : Math.min(255, r + dr);
		g = g == 0 ? 0 : Math.min(255, g + dg);
		b = b == 0 ? 0 : Math.min(255, b + db);
		return r << 16 | g << 8 | b;
	}

	/**
	 * A scrollbar in the classic style the reference screenshots show (RuneLite's 2005 interface style): the
	 * old arrow sprites, a flat dark track and a bevelled thumb sized to the visible share, drawn as the
	 * pre-2007 client drew it.
	 */
	public static void scrollbar(WidgetPainter p, int x, int y, int h, int contentHeight, int scroll)
	{
		int track = ScrollMath.track(h);
		p.fill(x, y + 16, 16, track, SCROLL_TRACK_COLOUR, 0);
		int thumb = ScrollMath.thumbHeight(h, contentHeight);
		int top = y + 16 + ScrollMath.thumbTop(h, contentHeight, scroll);
		p.fill(x, top, 16, thumb, SCROLL_THUMB_COLOUR, 0);
		p.fill(x, top, 2, thumb, SCROLL_LIGHT, 0);
		p.fill(x, top, 16, 2, SCROLL_LIGHT, 0);
		p.fill(x + 15, top, 1, thumb, SCROLL_DARK, 0);
		p.fill(x + 14, top + 1, 1, thumb - 1, SCROLL_DARK, 0);
		p.fill(x, top + thumb - 1, 16, 1, SCROLL_DARK, 0);
		p.fill(x + 1, top + thumb - 2, 15, 1, SCROLL_DARK, 0);
		p.sprite(SCROLL_UP, x, y);
		p.sprite(SCROLL_DOWN, x, y + h - 16);
		p.recordScrollbar(x, y, h, contentHeight);
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
		for (TaskFilter filter : TaskFilter.values())
		{
			if (filter.getLabel().equals(label))
			{
				p.hotspot(Hotspot.Kind.FILTER, filter, x, y + 19, w, 20);
			}
		}
	}
}
