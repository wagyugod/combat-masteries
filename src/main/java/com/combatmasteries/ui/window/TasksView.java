package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The Tasks tab (interface 715, scripts 4816, 4821 and 5244): filters on the left, the task list on the
 * right with an expand button, the content icon where Combat Achievements has its tier sword, the name and
 * a "Content:" line.
 */
final class TasksView
{
	static final int LIST_X = 130;
	static final int LIST_Y = 82;
	static final int LIST_W = 344 - 16;
	static final int ROW_H = 40;

	private static final int EXPAND = 2312;
	private static final int COLLAPSE = 2314;

	private TasksView()
	{
	}

	static void draw(WidgetPainter p, WindowModel model, WindowState state, int w, int h)
	{
		WindowChrome.dividerUnderBar(p, w);
		p.graphic(WindowChrome.DIVIDER_VERTICAL, 109, 81, 36, h - 86, true);

		// Filters
		int panelX = 6;
		int panelW = 118;
		p.text("Filters", GameFont.BOLD, WindowChrome.ORANGE, true, panelX, LIST_Y + 1, panelW, 20,
			WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		int dx = panelX + (panelW - 110) / 2;
		WindowChrome.dropdown(p, dx, LIST_Y + 26, "Type", state.typeFilterText());
		WindowChrome.dropdown(p, dx, LIST_Y + 77, "Content", state.contentFilterText());
		WindowChrome.dropdown(p, dx, LIST_Y + 128, "Completed", state.completedFilterText());

		// List
		List<WindowModel.Task> tasks = state.filter(model.getTasks());
		int listH = h - 88;
		Graphics2D g = p.graphics();
		Shape clip = g.getClip();
		g.clipRect(LIST_X, LIST_Y, LIST_W, listH);
		int y = LIST_Y - state.getTasksScroll();
		int contentH = 0;
		for (int i = 0; i < tasks.size(); i++)
		{
			WindowModel.Task task = tasks.get(i);
			int rowH = drawRow(p, task, i, y, task.getId().equals(state.getExpandedTask()));
			y += rowH;
			contentH += rowH;
		}
		g.setClip(clip);
		if (tasks.isEmpty())
		{
			p.text("There are no tasks available to you<br>that match your filters.", GameFont.REGULAR, WindowChrome.ORANGE, true,
				LIST_X, LIST_Y, LIST_W, listH, WidgetPainter.CENTRE, WidgetPainter.MIDDLE, 0);
		}

		WindowChrome.scrollbar(p, LIST_X + LIST_W, LIST_Y, listH, contentH, state.getTasksScroll());
	}

	private static int drawRow(WidgetPainter p, WindowModel.Task task, int index, int y, boolean expanded)
	{
		int x = LIST_X;
		String description = "Description: <col=ffffff>" + task.getDescription() + "</col>";
		int descLines = expanded ? p.lineCount(description, GameFont.SMALL, LIST_W - 10) : 0;
		int rowH = expanded ? ROW_H + 10 * descLines + 25 : ROW_H;

		p.fill(x, y, LIST_W, rowH, 0xffffff, index % 2 == 1 ? 230 : 245);
		p.hotspot(Hotspot.Kind.TASK, task.getId(), x, y, LIST_W, rowH);
		p.graphic(expanded ? COLLAPSE : EXPAND, x + 5, y + (ROW_H - 18) / 2, 18, 18, false);
		BufferedImage icon = ContentIcons.get(task.getContent());
		p.image(icon, x + 28, y + (ROW_H - ContentIcons.SIZE) / 2);
		p.text(task.getName(), GameFont.REGULAR, task.isCompleted() ? WindowChrome.GREEN : WindowChrome.INCOMPLETE, true,
			x + 55, y + ROW_H / 2 - 16, LIST_W - 55, 16, WidgetPainter.LEFT, WidgetPainter.MIDDLE, 0);
		p.text("Content: <col=ffffff>" + task.getContent() + "</col>", GameFont.SMALL, WindowChrome.ORANGE, true,
			x + 55, y + ROW_H / 2 + 2, LIST_W - 55, 11, WidgetPainter.LEFT, WidgetPainter.MIDDLE, 0);
		if (expanded)
		{
			p.text("Type: <col=ffffff>" + task.getCategory().getDisplayName() + "</col> <col=dfdfdf>(+1 point)</col>",
				GameFont.SMALL, WindowChrome.ORANGE, true, x + 5, y + ROW_H, LIST_W - 55, 11, WidgetPainter.LEFT, WidgetPainter.MIDDLE, 0);
			p.text(description, GameFont.SMALL, WindowChrome.ORANGE, true, x + 5, y + ROW_H + 19, LIST_W - 10, 10 * descLines,
				WidgetPainter.LEFT, WidgetPainter.MIDDLE, 0);
		}
		return rowH;
	}
}
