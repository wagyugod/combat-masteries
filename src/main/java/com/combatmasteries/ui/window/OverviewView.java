package com.combatmasteries.ui.window;

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
	 * Personal best values. Script 4809 asks for 0xdfdfdf; the current game shows them a touch lighter.
	 */
	private static final int VALUE_COLOUR = 0xe4e4e4;
	/**
	 * The light edge outside the tier boxes, and its fainter ends.
	 */
	private static final int HIGHLIGHT = 0x96806b;
	private static final int HIGHLIGHT_FADED = 0x7b705b;

	private OverviewView()
	{
	}

	static void draw(WidgetPainter p, WindowModel model, int w, int h)
	{
		int rightX = w - 6 - RIGHT_W;
		int contentH = h - 104;

		p.text("Mastery Tiers", GameFont.BOLD, WindowChrome.ORANGE, true, LEFT_X, HEADER_Y + 1, LEFT_W, 22,
			WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		p.text("Personal Bests - " + model.getPlayerName(), GameFont.BOLD, WindowChrome.ORANGE, true, rightX, HEADER_Y + 1,
			RIGHT_W, 22, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);

		drawTiers(p, model.getTiers(), LEFT_X, CONTENT_Y, contentH);
		drawPersonalBests(p, model.getPersonalBests(), rightX, CONTENT_Y, RIGHT_W, contentH);
	}

	/**
	 * Script 4806 lays the six difficulty boxes out two to a row, each 90 wide and a third of the column
	 * tall. With three tiers each gets a row of its own, the width of both columns.
	 */
	private static void drawTiers(WidgetPainter p, List<WindowModel.Tier> tiers, int left, int top, int height)
	{
		int boxW = 90 * 2 + 5;
		int boxH = height / 3 - 3;
		int y = 0;
		for (int i = 0; i < tiers.size(); i++)
		{
			WindowModel.Tier tier = tiers.get(i);
			int x = left + 4;
			int by = top + y;
			WindowChrome.stoneBox(p, x, by, boxW, boxH);
			p.outline(x, by, boxW, boxH, 0x000000, 200);
			boxHighlight(p, x, by, boxW, boxH);
			p.text(tier.getName(), GameFont.REGULAR, WindowChrome.LIGHT_ORANGE, true, x, by + 5, boxW, 12,
				WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
			p.image(TierIcons.small(tier.getTier()), x + boxW / 2 - TierIcons.SMALL / 2, by + 19);
			WindowChrome.segmentedBar(p, tier.getProgress(), tier.getThreshold(), x + 9, by + 41, boxW - 18, 2);
			y += boxH + 4;
		}
	}

	/**
	 * The light edge the current game shows just outside a tier box: along the top, fading at its ends,
	 * and down the right side by the top and bottom corners.
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
		p.fill(x + w, y + h - 11, 1, 4, HIGHLIGHT, 208);
	}

	/**
	 * Rows as script 4809 draws the Combat Profile: alternating light bands, label left, value right. The
	 * script frames them with a dark line; the current game instead shows a faint light edge, drawn over
	 * the rows.
	 */
	private static void drawPersonalBests(WidgetPainter p, List<WindowModel.PersonalBest> rows, int left, int top, int w, int h)
	{
		int rowW = w - 4;
		int rowH = h / Math.max(7, rows.size());
		// Rows run down to the frame's bottom edge; the last takes whatever the division leaves over
		int bottom = top + h - 4;
		int y = top + 1;
		for (int i = 0; i < rows.size(); i++)
		{
			WindowModel.PersonalBest row = rows.get(i);
			if (i == rows.size() - 1 && rows.size() > 7)
			{
				rowH = bottom - y;
			}
			p.fill(left + 1, y, rowW, rowH, 0xffffff, i % 2 == 0 ? 230 : 245);
			p.text(row.getLabel(), GameFont.REGULAR, WindowChrome.LIGHT_ORANGE, left + 1 + 5, y, rowW, rowH, WidgetPainter.LEFT);
			p.text(row.getValue(), GameFont.REGULAR, VALUE_COLOUR, left + 1, y, rowW - 5, rowH, WidgetPainter.RIGHT);
			y += rowH;
		}
		p.outline(left, top, w - 2, h - 4, 0xffffff, 245);
	}
}
