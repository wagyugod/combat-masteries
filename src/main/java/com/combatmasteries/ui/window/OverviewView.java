package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;

/**
 * The Overview tab (interface 717, scripts 4805-4809): tier boxes on the left, where Combat Achievements
 * has its difficulty tiers, and personal bests on the right, in the Combat Profile's rows.
 */
final class OverviewView
{
	private static final int LEFT_X = 6;
	private static final int LEFT_W = 190;
	private static final int RIGHT_W = 277;
	private static final int HEADER_Y = 75;
	private static final int CONTENT_Y = 98;
	/**
	 * Combat Profile row height (script 4809).
	 */
	private static final int ROW_H = 31;
	/**
	 * Tier box grid (script 4806): two columns of 90 wide boxes, 5 apart.
	 */
	private static final int TIER_COLUMNS = 2;
	private static final int TIER_BOX_W = 90;
	private static final int TIER_GAP = 5;
	/**
	 * Personal best values. Script 4809 asks for 0xdfdfdf; the current game shows them a touch lighter.
	 */
	private static final int VALUE_COLOUR = 0xe4e4e4;
	/**
	 * The light edge outside the tier boxes, and its fainter ends.
	 */
	private static final int HIGHLIGHT = 0x96806b;
	private static final int HIGHLIGHT_FADED = 0x7b705b;
	/**
	 * The right side of that edge: just below and above the bright corner pieces, and the faint
	 * lightening between them (a translucent fill that lifts the stone by one shade).
	 */
	private static final int SIDE_LIGHT = 0x4a463e;
	private static final int SIDE_LIGHT_TOP = 0x4a483e;
	private static final int SIDE_LIGHT_BOTTOM = 0x4a453a;
	private static final int SIDE_FAINT = 0x8c6e6e;

	private OverviewView()
	{
	}

	static void draw(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		int rightX = w - 6 - RIGHT_W;
		int contentH = h - 104;

		p.text("Mastery Tiers", GameFont.BOLD, WindowChrome.ORANGE, true, LEFT_X, HEADER_Y + 1, LEFT_W, 22,
			WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		p.text("Personal Bests - " + model.getPlayerName(), GameFont.BOLD, WindowChrome.ORANGE, true, rightX, HEADER_Y + 1,
			RIGHT_W, 22, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);

		drawTiers(p, model.getTiers(), LEFT_X, CONTENT_Y, contentH);
		drawPersonalBests(p, model.getPersonalBests(), state.getOverviewScroll(), rightX, CONTENT_Y, RIGHT_W, contentH);
	}

	/**
	 * Script 4806 lays the difficulty boxes out two to a row, each 90 wide and 5 apart, and a third of the
	 * column tall. The mastery tiers fill that grid in the same order (Ascended and Exalted on the first
	 * row, Immortal on the second), leaving the remaining cells empty.
	 */
	private static void drawTiers(WidgetPainter p, List<WindowModel.Tier> tiers, int left, int top, int height)
	{
		int boxW = TIER_BOX_W;
		int boxH = height / 3 - 3;
		for (int i = 0; i < tiers.size(); i++)
		{
			WindowModel.Tier tier = tiers.get(i);
			int x = left + 4 + (i % TIER_COLUMNS) * (boxW + TIER_GAP);
			int by = top + (i / TIER_COLUMNS) * (boxH + 4);
			WindowChrome.stoneBox(p, x, by, boxW, boxH);
			boxHighlight(p, x, by, boxW, boxH);
			p.text(tier.getName(), GameFont.REGULAR, WindowChrome.LIGHT_ORANGE, true, x, by + 5, boxW, 12,
				WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
			p.image(TierIcons.small(tier.getTier()), x + boxW / 2 - TierIcons.SMALL / 2, by + 19);
			WindowChrome.segmentedBar(p, tier.getProgress(), tier.getThreshold(), x + 9, by + 41, boxW - 18, 2);
		}
	}

	/**
	 * The light edge the current game shows just outside a tier box: along the top, fading at its ends,
	 * and down the right side, brighter by the top and bottom corners and faint in between.
	 */
	private static void boxHighlight(WidgetPainter p, int x, int y, int w, int h)
	{
		p.fill(x + 3, y - 1, w - 6, 1, HIGHLIGHT, 208);
		p.fill(x + 1, y - 1, 2, 1, HIGHLIGHT_FADED, 208);
		p.fill(x + w - 3, y - 1, 2, 1, HIGHLIGHT_FADED, 208);
		p.fill(x + w - 1, y - 1, 1, 1, HIGHLIGHT, 248);
		p.fill(x + w, y, 1, 1, HIGHLIGHT, 248);
		p.fill(x + w, y + 1, 1, 2, HIGHLIGHT_FADED, 208);
		p.fill(x + w, y + 3, 1, 1, HIGHLIGHT, 208);
		p.fill(x + w, y + 4, 1, 1, SIDE_LIGHT_TOP, 0);
		p.fill(x + w, y + 5, 1, 4, SIDE_LIGHT, 0);
		p.fill(x + w, y + 9, 1, h - 20, SIDE_FAINT, 248);
		p.fill(x + w, y + h - 11, 1, 4, HIGHLIGHT, 208);
		p.fill(x + w, y + h - 7, 1, 4, SIDE_LIGHT, 0);
		p.fill(x + w, y + h - 3, 1, 2, SIDE_LIGHT_BOTTOM, 0);
	}

	/**
	 * Rows as script 4809 draws the Combat Profile: 31 tall, alternating light bands, label left, value
	 * right, running the full width of the frame. The script frames them with a dark line; the current
	 * game instead shows a faint light edge, drawn over the rows. The frame shows seven rows, the Combat
	 * Profile's count (the seventh runs under the frame's bottom line), and like the Combat Profile it has
	 * no scrollbar: further personal bests are reached by scrolling the list a row at a time.
	 */
	private static void drawPersonalBests(WidgetPainter p, List<WindowModel.PersonalBest> rows, int scroll, int left, int top, int w, int h)
	{
		int frameH = h - 4;
		int listY = top + 1;
		// The last row runs under the frame's bottom line
		int listH = frameH - 1;
		int rowW = w - 4;
		int visible = (listH + ROW_H - 1) / ROW_H;
		int offset = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible) * ROW_H));
		p.recordScroll(visible * ROW_H, rows.size() * ROW_H, ROW_H);

		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(left + 1, listY, rowW, listH);
		for (int i = 0; i < rows.size(); i++)
		{
			int y = listY + i * ROW_H - offset;
			if (y + ROW_H <= listY || y >= listY + listH)
			{
				continue;
			}
			WindowModel.PersonalBest row = rows.get(i);
			p.fill(left + 1, y, rowW, ROW_H, 0xffffff, i % 2 == 0 ? 230 : 245);
			p.text(row.getLabel(), GameFont.REGULAR, WindowChrome.LIGHT_ORANGE, left + 1 + 5, y, rowW, ROW_H, WidgetPainter.LEFT);
			p.text(row.getValue(), GameFont.REGULAR, VALUE_COLOUR, left + 1, y, rowW - 5, ROW_H, WidgetPainter.RIGHT);
		}
		g.setClip(clip);
		p.outline(left, top, w - 2, frameH, 0xffffff, 245);
	}
}
