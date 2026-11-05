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
	 * The tier buttons, drawn the way the current game draws them (the cache's button sprites are flat): a
	 * dark border with rounded corners, a light highlight inside it and a one pixel bevel inside that. The
	 * selected button is 4px wider and darker, with a deeper bevel and a strip of the unselected shade on
	 * its right. The game also darkens the strip behind each button very slightly and lights a faint line
	 * just outside it; the selected button's right edge glows brighter.
	 */
	private static final int BUTTON_X = 8;
	private static final int BUTTON_W = 117;
	private static final int BUTTON_SELECTED_W = 121;
	private static final int BUTTON_H = 32;
	private static final int BUTTON_TEXT_W = 101;
	private static final int BUTTON_BORDER = 0x2c2922;
	private static final int BUTTON_BORDER_CORNER = 0x2d2a22;
	private static final int BUTTON_LIGHT = 0x746652;
	private static final int BUTTON_LIGHT_CORNER = 0x736552;
	/**
	 * The anti-aliased pixels just outside each rounded corner: the border colour, mostly transparent.
	 */
	private static final int BUTTON_CORNER_TRANS = 220;
	/**
	 * The fill, then the bevel lines inside the highlight: top, left, bottom and right.
	 */
	private static final int[] BUTTON_SHADES = {0x554c41, 0x544c41, 0x4f473e, 0x4f473e, 0x544c41};
	private static final int[] BUTTON_SELECTED_SHADES = {0x483e33, 0x473d32, 0x40362d, 0x40362d, 0x4f473e};
	private static final int SELECTED_STRIP = 0x554c41;
	private static final int SELECTED_STRIP_END = 0x50493f;
	private static final int SELECTED_BOTTOM_END = 0x3e352c;
	/**
	 * Strength (out of 256) of the light line around a button, at its ends and along its length, and of
	 * the selected button's brighter right edge from its top down: first row, next two, the fourth, the
	 * length, and the last two.
	 */
	private static final int GLOW_END = 20;
	private static final int GLOW_SIDE = 32;
	private static final int GLOW_TOP = 34;
	private static final int[] SELECTED_GLOW = {60, 72, 85, 93, 82};
	private static final int BUTTON_SHADOW_TRANS = 254;
	/**
	 * Strength of the faint light line just outside the reward list's frame, on its left and below it.
	 */
	private static final int FRAME_GLOW = 13;

	private RewardsView()
	{
	}

	static void draw(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		int height = h - 91;

		// Reward list with a scrollbar. The frame wraps the list (it is the scrolled layer, sized to the
		// content) but, as in the game, a short list still sits in a box that ends 4px above the bottom of
		// the scrollbar.
		List<WindowModel.RewardGroup> groups = model.getRewards().getOrDefault(state.getRewardsTier(), Collections.emptyList());
		int top = TOP - state.getRewardsScroll();
		int contentH = drawGroups(p, groups, PANEL_X + 2, top + 2, PANEL_W - 4, false);
		int frameH = Math.max(contentH, height - 4);
		frameGlow(p, top, frameH, height);

		List<WindowModel.Tier> tiers = model.getTiers();
		for (int i = 0; i < tiers.size(); i++)
		{
			buttonGlow(p, TOP + i * BUTTON_H, tiers.get(i).getTier() == state.getRewardsTier());
		}
		for (int i = 0; i < tiers.size(); i++)
		{
			WindowModel.Tier tier = tiers.get(i);
			drawButton(p, tier, tier.getTier() == state.getRewardsTier(), TOP + i * BUTTON_H, i > 0, i < tiers.size() - 1);
		}

		if (top == TOP)
		{
			p.hline(PANEL_X, TOP - 1, PANEL_W - 1, TABLE_GLOW);
		}
		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(PANEL_X, TOP, PANEL_W, height);
		frame(p, PANEL_X, top, PANEL_W, frameH);
		drawGroups(p, groups, PANEL_X + 2, top + 2, PANEL_W - 4, true);
		g.setClip(clip);
		WindowChrome.scrollbar(p, PANEL_X + PANEL_W, TOP, height, contentH, state.getRewardsScroll());
	}

	/**
	 * The faint light line just outside the list frame's left side and below it, kept to the viewport.
	 */
	private static void frameGlow(WidgetPainter p, int top, int frameH, int height)
	{
		int from = top == TOP ? TOP - 1 : TOP;
		int below = top + frameH;
		int to = Math.min(below, TOP + height - 1);
		light(p, PANEL_X - 1, from, 1, to - from + 1, FRAME_GLOW);
		if (below < TOP + height)
		{
			light(p, PANEL_X, below, PANEL_W - 1, 1, FRAME_GLOW);
		}
	}

	/**
	 * The shade behind a tier button and the light line around it, drawn before any button so that a
	 * neighbouring button covers the part of the line that runs under it.
	 */
	private static void buttonGlow(WidgetPainter p, int y, boolean selected)
	{
		int w = selected ? BUTTON_SELECTED_W : BUTTON_W;
		int h = BUTTON_H;
		int left = BUTTON_X - 1;
		int right = BUTTON_X + w;
		p.fill(left, y, BUTTON_SELECTED_W + 1, h, 0x000000, BUTTON_SHADOW_TRANS);

		light(p, BUTTON_X + 1, y - 1, 2, 1, GLOW_END);
		light(p, BUTTON_X + 3, y - 1, w - 6, 1, GLOW_TOP);
		light(p, right - 3, y - 1, 2, 1, GLOW_END);
		sideGlow(p, left, y, h);
		if (!selected)
		{
			sideGlow(p, right, y, h);
			return;
		}
		light(p, right, y, 1, 1, SELECTED_GLOW[0]);
		light(p, right, y + 1, 1, 2, SELECTED_GLOW[1]);
		light(p, right, y + 3, 1, 1, SELECTED_GLOW[2]);
		light(p, right, y + 4, 1, h - 7, SELECTED_GLOW[3]);
		light(p, right, y + h - 3, 1, 2, SELECTED_GLOW[4]);
	}

	private static void sideGlow(WidgetPainter p, int x, int y, int h)
	{
		light(p, x, y + 1, 1, 2, GLOW_END);
		light(p, x, y + 3, 1, h - 6, GLOW_SIDE);
		light(p, x, y + h - 3, 1, 2, GLOW_END);
	}

	/**
	 * Lights a rectangle with the buttons' highlight colour at the given strength out of 256.
	 */
	private static void light(WidgetPainter p, int x, int y, int w, int h, int strength)
	{
		p.fill(x, y, w, h, BUTTON_LIGHT, 256 - strength);
	}

	/**
	 * @param joinedTop    whether another button sits right above, which the border row shares a shade with
	 * @param joinedBottom whether another button sits right below
	 */
	private static void drawButton(WidgetPainter p, WindowModel.Tier tier, boolean selected, int y, boolean joinedTop,
		boolean joinedBottom)
	{
		int x = BUTTON_X;
		int w = selected ? BUTTON_SELECTED_W : BUTTON_W;
		int h = BUTTON_H;
		int r = x + w - 1;
		int b = y + h - 1;
		int[] c = selected ? BUTTON_SELECTED_SHADES : BUTTON_SHADES;

		// Fill and the bevel inside the highlight
		p.fill(x + 3, y + 3, w - 6, h - 6, c[0], 0);
		p.hline(x + 3, y + 2, w - 6, c[1]);
		p.fill(x + 2, y + 3, 1, h - 6, c[2], 0);
		p.hline(x + 3, b - 2, w - 6, c[3]);
		p.fill(r - 2, y + 3, 1, h - 7, c[4], 0);
		p.fill(r - 2, b - 3, 1, 1, c[3], 0);
		if (selected)
		{
			p.fill(r - 3, y + 3, 1, h - 6, SELECTED_STRIP, 0);
			p.fill(r - 3, y + 2, 1, 1, SELECTED_STRIP_END, 0);
			p.fill(r - 3, b - 2, 1, 1, SELECTED_STRIP_END, 0);
			p.fill(r - 4, b - 2, 1, 1, SELECTED_BOTTOM_END, 0);
		}

		// Highlight
		p.hline(x + 3, y + 1, w - 6, BUTTON_LIGHT);
		p.hline(x + 3, b - 1, w - 6, BUTTON_LIGHT);
		p.fill(x + 1, y + 3, 1, h - 6, BUTTON_LIGHT, 0);
		p.fill(r - 1, y + 3, 1, h - 6, BUTTON_LIGHT, 0);

		// Border
		p.hline(x + 2, y, w - 4, joinedTop ? BUTTON_BORDER_CORNER : BUTTON_BORDER);
		p.hline(x + 2, b, w - 4, joinedBottom ? BUTTON_BORDER_CORNER : BUTTON_BORDER);
		p.fill(x, y + 3, 1, h - 6, BUTTON_BORDER, 0);
		p.fill(r, y + 3, 1, h - 6, BUTTON_BORDER, 0);

		// The four rounded corners, mirrored from the top left one
		for (int corner = 0; corner < 4; corner++)
		{
			boolean flipX = corner % 2 == 1;
			boolean flipY = corner >= 2;
			int cx = flipX ? r : x;
			int cy = flipY ? b : y;
			int dx = flipX ? -1 : 1;
			int dy = flipY ? -1 : 1;
			px(p, cx + 2 * dx, cy, BUTTON_BORDER_CORNER);
			px(p, cx + dx, cy + dy, BUTTON_BORDER_CORNER);
			px(p, cx, cy + 2 * dy, BUTTON_BORDER_CORNER);
			px(p, cx + 2 * dx, cy + dy, BUTTON_LIGHT_CORNER);
			px(p, cx + dx, cy + 2 * dy, BUTTON_LIGHT_CORNER);
			px(p, cx + 2 * dx, cy + 2 * dy, BUTTON_LIGHT_CORNER);
			p.fill(cx + dx, cy, 1, 1, BUTTON_BORDER, BUTTON_CORNER_TRANS);
			p.fill(cx, cy + dy, 1, 1, BUTTON_BORDER, BUTTON_CORNER_TRANS);
		}

		p.image(TierIcons.small(tier.getTier()), x + 4, y + 4);
		p.text(tier.getName(), GameFont.REGULAR, tier.isUnlocked() ? WindowChrome.BRIGHT_GREEN : WindowChrome.ORANGE, true,
			x + 20, y + 4, BUTTON_TEXT_W, h - 8, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
	}

	private static void px(WidgetPainter p, int x, int y, int rgb)
	{
		p.fill(x, y, 1, 1, rgb, 0);
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
			p.image(TierIcons.large(tier), left + 5 + (ICON_W - TierIcons.LARGE_WIDTH) / 2, y + (groupH - ICON_H) / 2);
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
