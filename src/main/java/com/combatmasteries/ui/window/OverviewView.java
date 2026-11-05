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
			p.text(tier.getName(), GameFont.REGULAR, WindowChrome.LIGHT_ORANGE, true, x, by + 5, boxW, 12,
				WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
			p.image(TierIcons.small(tier.getTier()), x + boxW / 2 - TierIcons.SMALL / 2, by + 19);
			WindowChrome.segmentedBar(p, tier.getProgress(), tier.getThreshold(), x + 9, by + 41, boxW - 18, 2);
			y += boxH + 4;
		}
	}

	/**
	 * Rows as script 4809 draws the Combat Profile: alternating light bands, label left, value right.
	 */
	private static void drawPersonalBests(WidgetPainter p, List<WindowModel.PersonalBest> rows, int left, int top, int w, int h)
	{
		p.outline(left, top, w - 2, h - 4, 0x000000, 150);
		int rowW = w - 4;
		int rowH = h / Math.max(7, rows.size());
		int y = top + 1;
		for (int i = 0; i < rows.size(); i++)
		{
			WindowModel.PersonalBest row = rows.get(i);
			p.fill(left + 1, y, rowW, rowH, 0xffffff, i % 2 == 0 ? 230 : 245);
			p.text(row.getLabel(), GameFont.REGULAR, WindowChrome.LIGHT_ORANGE, left + 1 + 5, y, rowW, rowH, WidgetPainter.LEFT);
			p.text(row.getValue(), GameFont.REGULAR, WindowChrome.GREY_TEXT, left + 1, y, rowW - 5, rowH, WidgetPainter.RIGHT);
			y += rowH;
		}
	}
}
