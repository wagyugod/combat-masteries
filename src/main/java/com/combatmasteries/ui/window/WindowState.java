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
 * search, the Rewards tier and how far each list is scrolled.
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
	private MasteryTier rewardsTier = MasteryTier.ASCENDED;

	private int overviewScroll;
	private int tasksScroll;
	private int bossesScroll;
	private int rewardsScroll;

	public String typeFilterText()
	{
		return typeFilter == null ? "All" : typeFilter.getDisplayName();
	}

	public String contentFilterText()
	{
		return contentFilter == null ? "All" : contentFilter;
	}

	public String completedFilterText()
	{
		return completedFilter == null ? "All" : completedFilter ? "Yes" : "No";
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
