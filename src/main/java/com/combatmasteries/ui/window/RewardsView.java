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
		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(PANEL_X, TOP, PANEL_W, height);
		cell(p, PANEL_X, top, PANEL_W, contentH);
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
		int y = top + 2;
		for (WindowModel.RewardGroup group : groups)
		{
			int groupH = 0;
			for (int i = 0; i < group.getLines().size(); i++)
			{
				String line = group.getLines().get(i);
				int rowH = 13 * p.lineCount(line, GameFont.REGULAR, textW - 10) + 8;
				if (draw)
				{
					p.fill(textX, y + groupH, textW, rowH, 0xffffff, i % 2 == 0 ? 245 : 230);
					p.text(line, GameFont.REGULAR, WindowChrome.ORANGE, true, textX + 5, y + groupH, textW - 10, rowH,
						WidgetPainter.LEFT, WidgetPainter.MIDDLE, 15);
				}
				groupH += rowH;
			}
			if (!draw)
			{
				y += groupH - 1;
				continue;
			}
			cell(p, textX, y, textW, groupH);
			int iconX = left + 2;
			p.fill(iconX, y, ICON_W + 6, groupH, 0xffffff, 230);
			MasteryTier tier = group.getTier();
			p.image(TierIcons.large(tier), left + 5 + (ICON_W - TierIcons.LARGE) / 2, y + (groupH - ICON_H) / 2);
			cell(p, iconX, y, ICON_W + 6, groupH);
			y += groupH - 1;
		}
		return y + 1 + 5 - top;
	}

	/**
	 * Script 4216: a dark outline with a lighter one inside it.
	 */
	private static void cell(WidgetPainter p, int x, int y, int w, int h)
	{
		p.outline(x, y, w, h, WindowChrome.LINE_DARK);
		p.outline(x + 1, y + 1, w - 2, h - 2, WindowChrome.LINE_LIGHT);
	}
}
