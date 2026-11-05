package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Collections;
import java.util.List;

/**
 * The Rewards tab (interface 714, scripts 4811-4815 and 4216): tier buttons on the left and the selected
 * tier's rewards on the right, each group of rows beside its icon.
 */
final class RewardsView
{
	private static final int TOP = 81;
	private static final int BUTTONS_X = 4;
	private static final int BUTTONS_W = 125;
	private static final int PANEL_X = 130;
	private static final int PANEL_W = 340 - 18;
	private static final int ICON_W = 36;
	private static final int ICON_H = 32;

	/**
	 * Line colours of the list, as the reference screenshot shows them: the game's near-black lines, and
	 * highlights inside them that take a little of the colour beside them (greener next to the lighter rows
	 * and the icon cells, browner next to the darker rows). Around the table and inside the frame the game
	 * also shows a faint lighter line just outside each dark line and a darker one inside the frame's
	 * highlight on its left and top.
	 */
	private static final int LINE = 0x0c0c0a;
	private static final int FRAME_LIGHT_SIDES = 0x494846;
	private static final int FRAME_LIGHT_ENDS = 0x4a4846;
	private static final int FRAME_SHADE = 0x483c2f;
	private static final int TABLE_GLOW = 0x49473b;
	private static final int TABLE_GLOW_RIGHT = 0x4a473c;
	private static final int TABLE_GLOW_BOTTOM = 0x4a4739;
	private static final int CELL_LIGHT = 0x4f5047;
	private static final int CELL_LIGHT_DARK_ROW = 0x4f4846;
	private static final int ROW_TRANS_DARK = 245;
	private static final int ROW_TRANS_LIGHT = 230;

	/**
	 * Tier button pieces: corners top left, top right, bottom left, bottom right, edges top, bottom, left,
	 * right, then the middle.
	 */
	private static final int[] BUTTON = {1012, 1013, 1014, 2834, 2835, 2836, 2837, 2838, 2839};
	private static final int[] BUTTON_SELECTED = {1004, 1013, 1006, 2834, 1008, 1009, 1010, 2838, 1011};

	private RewardsView()
	{
	}

	static void draw(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		int height = h - 91;
		int y = TOP;
		for (WindowModel.Tier tier : model.getTiers())
		{
			y += drawButton(p, tier, tier.getTier() == state.getRewardsTier(), y);
		}

		// Reward list with a scrollbar. The frame wraps the list (it is the scrolled layer, sized to the
		// content), so it ends just below the last group and scrolls with it.
		List<WindowModel.RewardGroup> groups = model.getRewards().getOrDefault(state.getRewardsTier(), Collections.emptyList());
		int top = TOP - state.getRewardsScroll();
		int contentH = drawGroups(p, groups, PANEL_X + 2, top + 2, PANEL_W - 4, false);
		if (top == TOP)
		{
			p.hline(PANEL_X, TOP - 1, PANEL_W - 1, TABLE_GLOW);
		}
		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(PANEL_X, TOP, PANEL_W, height);
		frame(p, PANEL_X, top, PANEL_W, contentH);
		drawGroups(p, groups, PANEL_X + 2, top + 2, PANEL_W - 4, true);
		g.setClip(clip);
		WindowChrome.scrollbar(p, PANEL_X + PANEL_W, TOP, height, contentH, state.getRewardsScroll());
	}

	/**
	 * @return the height taken, gap included
	 */
	private static int drawButton(WidgetPainter p, WindowModel.Tier tier, boolean selected, int y)
	{
		int[] s = selected ? BUTTON_SELECTED : BUTTON;
		int inner = 24;
		int left = BUTTONS_X;
		int w = BUTTONS_W;
		int rightInset = selected ? 0 : 4;
		int edgeShift = selected ? 2 : 0;
		int edgeW = w - (12 + rightInset);
		int edgeX = left + (w - edgeW) / 2 + edgeShift;

		p.graphic(s[0], left + 4, y, 4, 4, false);
		p.graphic(s[1], left + w - 4 - rightInset, y, 4, 4, false);
		p.graphic(s[2], left + 4, y + 4 + inner, 4, 4, false);
		p.graphic(s[3], left + w - 4 - rightInset, y + 4 + inner, 4, 4, false);
		p.graphic(s[4], edgeX, y, edgeW, 4, false);
		p.graphic(s[5], edgeX, y + 4 + inner, edgeW, 4, false);
		p.graphic(s[6], left + 4, y + 4, 4, inner, false);
		p.graphic(s[7], left + w - 4 - rightInset, y + 4, 4, inner, false);
		p.graphic(s[8], edgeX, y + 4, edgeW, inner, false);
		p.image(TierIcons.small(tier.getTier()), left + 8, y + 4);
		p.text(tier.getName(), GameFont.REGULAR, tier.isUnlocked() ? WindowChrome.BRIGHT_GREEN : WindowChrome.ORANGE, true,
			left + 24, y + 4, w - 24, inner, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		return inner + 8;
	}

	/**
	 * Lays out (and if {@code draw}, draws) the groups, script 4813's way: rows of text beside a cell with
	 * the icon, neighbouring groups sharing a border line.
	 *
	 * @return the height of the list including its margins, which is the height of the frame around it
	 */
	private static int drawGroups(WidgetPainter p, List<WindowModel.RewardGroup> groups, int left, int top, int width, boolean draw)
	{
		int textX = left + ICON_W + 7;
		int textW = width - (ICON_W + 7) - 4;
		int iconX = left + 2;
		int y = top + 2;
		for (WindowModel.RewardGroup group : groups)
		{
			int groupH = 0;
			int rows = group.getLines().size();
			int[] rowHeights = new int[rows];
			for (int i = 0; i < rows; i++)
			{
				String line = group.getLines().get(i);
				rowHeights[i] = 13 * p.lineCount(line, GameFont.REGULAR, textW - 10) + 8;
				if (draw)
				{
					p.fill(textX, y + groupH, textW, rowHeights[i], 0xffffff, rowTrans(i));
					p.text(line, GameFont.REGULAR, WindowChrome.ORANGE, true, textX + 5, y + groupH, textW - 10,
						rowHeights[i], WidgetPainter.LEFT, WidgetPainter.MIDDLE, 15);
				}
				groupH += rowHeights[i];
			}
			if (!draw)
			{
				y += groupH - 1;
				continue;
			}
			textCell(p, textX, y, textW, rowHeights);
			p.fill(iconX, y, ICON_W + 6, groupH, 0xffffff, ROW_TRANS_LIGHT);
			MasteryTier tier = group.getTier();
			p.image(TierIcons.large(tier), left + 5 + (ICON_W - TierIcons.LARGE) / 2, y + (groupH - ICON_H) / 2);
			cell(p, iconX, y, ICON_W + 6, groupH, CELL_LIGHT, CELL_LIGHT, CELL_LIGHT, CELL_LIGHT);
			y += groupH - 1;
		}
		if (draw && !groups.isEmpty())
		{
			// The faint glow just outside the table's dark lines
			int tableTop = top + 2;
			int tableBottom = y;
			int tableRight = textX + textW - 1;
			p.hline(iconX - 1, tableTop - 1, tableRight - iconX + 3, TABLE_GLOW);
			p.fill(iconX - 1, tableTop - 1, 1, tableBottom - tableTop + 3, TABLE_GLOW, 0);
			p.fill(tableRight + 1, tableTop - 1, 1, tableBottom - tableTop + 2, TABLE_GLOW_RIGHT, 0);
			p.hline(iconX - 1, tableBottom + 1, tableRight - iconX + 3, TABLE_GLOW_BOTTOM);
		}
		return y + 1 + 5 - top;
	}

	/**
	 * The game's alternating row shading: the first row of a group darker, the next lighter.
	 */
	private static int rowTrans(int row)
	{
		return row % 2 == 0 ? ROW_TRANS_DARK : ROW_TRANS_LIGHT;
	}

	private static int rowLight(int row)
	{
		return rowTrans(row) == ROW_TRANS_DARK ? CELL_LIGHT_DARK_ROW : CELL_LIGHT;
	}

	/**
	 * A group's text cell, whose highlight at each side takes the shade of the row beside it.
	 */
	private static void textCell(WidgetPainter p, int x, int y, int w, int[] rowHeights)
	{
		int h = 0;
		for (int rowH : rowHeights)
		{
			h += rowH;
		}
		int last = rowHeights.length - 1;
		cell(p, x, y, w, h, rowLight(0), rowLight(last), CELL_LIGHT, CELL_LIGHT);
		int rowY = y;
		for (int i = 0; i < rowHeights.length; i++)
		{
			int from = Math.max(rowY, y + 1);
			int to = Math.min(rowY + rowHeights[i], y + h - 1);
			p.fill(x + 1, from, 1, to - from, rowLight(i), 0);
			p.fill(x + w - 2, from, 1, to - from, rowLight(i), 0);
			rowY += rowHeights[i];
		}
	}

	/**
	 * Script 4216: a dark outline with a lighter one inside it, each side's highlight given separately.
	 */
	private static void cell(WidgetPainter p, int x, int y, int w, int h, int lightTop, int lightBottom,
		int lightLeft, int lightRight)
	{
		p.outline(x, y, w, h, LINE);
		p.hline(x + 1, y + 1, w - 2, lightTop);
		p.hline(x + 1, y + h - 2, w - 2, lightBottom);
		p.fill(x + 1, y + 2, 1, h - 4, lightLeft, 0);
		p.fill(x + w - 2, y + 2, 1, h - 4, lightRight, 0);
	}

	/**
	 * The frame around the list: the cell outline with a dark shade inside its highlight on the left and
	 * top.
	 */
	private static void frame(WidgetPainter p, int x, int y, int w, int h)
	{
		cell(p, x, y, w, h, FRAME_LIGHT_ENDS, FRAME_LIGHT_ENDS, FRAME_LIGHT_SIDES, FRAME_LIGHT_SIDES);
		p.hline(x + 2, y + 2, w - 4, FRAME_SHADE);
		p.fill(x + 2, y + 2, 1, h - 4, FRAME_SHADE, 0);
	}
}
