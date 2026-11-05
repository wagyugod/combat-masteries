package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import lombok.Data;

/**
 * What the player has chosen in the window: the tab, the Tasks filters, the expanded task, the Bosses
 * search, the Rewards tier and how far each list is scrolled; and the open menu or filter list and the
 * mouse position, for drawing them.
 */
@Data
public class WindowState
{
	private WindowView view = WindowView.OVERVIEW;

	/**
	 * Null for All.
	 */
	@Nullable
	private TaskCategory typeFilter;
	/**
	 * Null for All.
	 */
	@Nullable
	private String contentFilter;
	/**
	 * Null for All, true for Yes, false for No.
	 */
	@Nullable
	private Boolean completedFilter;
	@Nullable
	private String expandedTask;

	private String search = "";
	/**
	 * True after the Bosses search box was clicked, until a click elsewhere, Escape or a tab change.
	 */
	private boolean searchFocused;
	private MasteryTier rewardsTier = MasteryTier.ASCENDED;

	private int overviewScroll;
	private int tasksScroll;
	private int bossesScroll;
	private int rewardsScroll;

	/**
	 * Whether the tab menu (the button in the top left) is open.
	 */
	private boolean menuOpen;
	/**
	 * The filter whose option list is open, or null.
	 */
	@Nullable
	private TaskFilter openFilter;
	private int filterScroll;

	/**
	 * Where the mouse is, in window coordinates, for hover highlights; -1 when it is outside the window.
	 */
	private int mouseX = -1;
	private int mouseY = -1;

	public boolean isHovered(int x, int y, int w, int h)
	{
		return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
	}

	public int getScroll(ScrollTarget target)
	{
		switch (target)
		{
			case TASKS:
				return tasksScroll;
			case BOSSES:
				return bossesScroll;
			case REWARDS:
				return rewardsScroll;
			case FILTER:
				return filterScroll;
			default:
				return overviewScroll;
		}
	}

	public void setScroll(ScrollTarget target, int scroll)
	{
		switch (target)
		{
			case TASKS:
				tasksScroll = scroll;
				break;
			case BOSSES:
				bossesScroll = scroll;
				break;
			case REWARDS:
				rewardsScroll = scroll;
				break;
			case FILTER:
				filterScroll = scroll;
				break;
			default:
				overviewScroll = scroll;
				break;
		}
	}

	public String filterText(TaskFilter filter)
	{
		switch (filter)
		{
			case TYPE:
				return typeFilterText();
			case CONTENT:
				return contentFilterText();
			default:
				return completedFilterText();
		}
	}

	/**
	 * @return the choices a filter's list offers, "All" first; the Content filter lists the content that has
	 * tasks, in the Bosses tab's order
	 */
	public static List<String> filterOptions(TaskFilter filter, WindowModel model)
	{
		List<String> options = new ArrayList<>();
		options.add(ALL);
		switch (filter)
		{
			case TYPE:
				for (TaskCategory category : TaskCategory.values())
				{
					options.add(category.getDisplayName());
				}
				break;
			case CONTENT:
				for (WindowModel.Content content : model.getContents())
				{
					if (content.getTasks() > 0)
					{
						options.add(content.getName());
					}
				}
				break;
			case COMPLETED:
				options.add(YES);
				options.add(NO);
				break;
		}
		return options;
	}

	/**
	 * Applies a choice from a filter's list.
	 */
	public void selectFilter(TaskFilter filter, String option)
	{
		boolean all = ALL.equals(option);
		switch (filter)
		{
			case TYPE:
				typeFilter = null;
				for (TaskCategory category : TaskCategory.values())
				{
					if (category.getDisplayName().equals(option))
					{
						typeFilter = category;
					}
				}
				break;
			case CONTENT:
				contentFilter = all ? null : option;
				break;
			case COMPLETED:
				completedFilter = all ? null : YES.equals(option);
				break;
		}
	}

	private static final String ALL = "All";
	private static final String YES = "Yes";
	private static final String NO = "No";

	public String typeFilterText()
	{
		return typeFilter == null ? ALL : typeFilter.getDisplayName();
	}

	public String contentFilterText()
	{
		return contentFilter == null ? ALL : contentFilter;
	}

	public String completedFilterText()
	{
		return completedFilter == null ? ALL : completedFilter ? YES : NO;
	}

	/**
	 * Switches to the Tasks tab showing every task: all filters back to All, the list at its top.
	 */
	public void showAllTasks()
	{
		view = WindowView.TASKS;
		typeFilter = null;
		contentFilter = null;
		completedFilter = null;
		openFilter = null;
		tasksScroll = 0;
	}

	/**
	 * @return the tasks that pass the Tasks tab's filters, in task list order
	 */
	public List<WindowModel.Task> filter(List<WindowModel.Task> tasks)
	{
		List<WindowModel.Task> shown = new ArrayList<>();
		for (WindowModel.Task task : tasks)
		{
			if (typeFilter != null && task.getCategory() != typeFilter)
			{
				continue;
			}
			if (contentFilter != null && !contentFilter.equals(task.getContent()))
			{
				continue;
			}
			if (completedFilter != null && task.isCompleted() != completedFilter)
			{
				continue;
			}
			shown.add(task);
		}
		return shown;
	}

	/**
	 * @return the content that matches the Bosses tab's search
	 */
	public List<WindowModel.Content> search(List<WindowModel.Content> contents)
	{
		String needle = search.toLowerCase(Locale.ROOT);
		List<WindowModel.Content> shown = new ArrayList<>();
		for (WindowModel.Content content : contents)
		{
			if (needle.isEmpty() || content.getName().toLowerCase(Locale.ROOT).contains(needle))
			{
				shown.add(content);
			}
		}
		return shown;
	}
}
