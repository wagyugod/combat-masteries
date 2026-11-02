package com.combatmasteries.ui;

import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskCategory;
import com.combatmasteries.tasks.TaskList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

/**
 * Side panel listing the tasks, the player's tier and progress towards the next one.
 */
public class MasteriesPanel extends PluginPanel
{
	private static final String ALL_CONTENT = "All content";
	private static final String ALL_CATEGORIES = "Speed & Challenge";
	private static final Color COMPLETE_BACKGROUND = new Color(30, 48, 32);
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");

	private enum StatusFilter
	{
		ALL("All tasks"),
		INCOMPLETE("Incomplete"),
		COMPLETED("Completed");

		private final String label;

		StatusFilter(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final JLabel tierLabel = new JLabel();
	private final JLabel pointsLabel = new JLabel();
	private final JLabel nextTierLabel = new JLabel();
	private final ProgressBar progressBar = new ProgressBar();
	private final JComboBox<StatusFilter> statusFilter = new JComboBox<>(StatusFilter.values());
	private final JComboBox<String> contentFilter = new JComboBox<>();
	private final JComboBox<Object> categoryFilter = new JComboBox<>();
	private final JPanel taskListPanel = new JPanel();

	private TaskList taskList;
	private MasteryProgress progress;

	public MasteriesPanel()
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 8));

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("Combat Masteries");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(title);

		tierLabel.setFont(FontManager.getRunescapeBoldFont());
		header.add(tierLabel);

		pointsLabel.setFont(FontManager.getRunescapeSmallFont());
		pointsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(pointsLabel);

		progressBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		progressBar.setForeground(ColorScheme.BRAND_ORANGE);
		progressBar.setAlignmentX(LEFT_ALIGNMENT);
		header.add(progressBar);

		nextTierLabel.setFont(FontManager.getRunescapeSmallFont());
		nextTierLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(nextTierLabel);

		categoryFilter.addItem(ALL_CATEGORIES);
		for (TaskCategory category : TaskCategory.values())
		{
			categoryFilter.addItem(category);
		}

		JPanel filters = new JPanel(new GridLayout(3, 1, 0, 4));
		filters.setBorder(new EmptyBorder(6, 0, 0, 0));
		filters.add(statusFilter);
		filters.add(categoryFilter);
		filters.add(contentFilter);
		filters.setAlignmentX(LEFT_ALIGNMENT);
		header.add(filters);

		for (Component c : header.getComponents())
		{
			((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
		}

		statusFilter.addActionListener(e -> rebuildTasks());
		categoryFilter.addActionListener(e -> rebuildTasks());
		contentFilter.addActionListener(e -> rebuildTasks());

		taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));

		add(header, BorderLayout.NORTH);
		add(taskListPanel, BorderLayout.CENTER);
	}

	/**
	 * Must be called on the Swing thread.
	 *
	 * @param progress the logged-in account's progress, or null when logged out
	 */
	public void update(TaskList taskList, MasteryProgress progress)
	{
		boolean newTasks = this.taskList != taskList;
		this.taskList = taskList;
		this.progress = progress;

		if (newTasks)
		{
			String selected = (String) contentFilter.getSelectedItem();
			contentFilter.removeAllItems();
			contentFilter.addItem(ALL_CONTENT);
			taskList.getTasks().stream().map(MasteryTask::getContent).distinct().forEach(contentFilter::addItem);
			contentFilter.setSelectedItem(selected != null ? selected : ALL_CONTENT);
		}

		rebuildHeader();
		rebuildTasks();
	}

	private void rebuildHeader()
	{
		int total = taskList.size();
		int points = progress == null ? 0 : progress.countCompleted(taskList);
		MasteryTier tier = MasteryTier.forPoints(points, total);

		if (progress == null)
		{
			tierLabel.setText("Log in to see your progress");
			tierLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
		else if (tier == null)
		{
			tierLabel.setText("No tier unlocked");
			tierLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		}
		else
		{
			tierLabel.setText(tier.getDisplayName());
			tierLabel.setForeground(tier.getColor());
		}

		pointsLabel.setText(points + " / " + total + " points");

		List<Integer> markers = new ArrayList<>();
		for (MasteryTier t : MasteryTier.values())
		{
			markers.add(t.threshold(total));
		}
		progressBar.setMaximumValue(Math.max(1, total));
		progressBar.setValue(points);
		progressBar.setPositions(markers);
		progressBar.setCenterLabel(total == 0 ? "" : (points * 100 / total) + "%");

		MasteryTier next = MasteryTier.next(tier);
		if (progress == null || total == 0)
		{
			nextTierLabel.setText(" ");
		}
		else if (next == null)
		{
			nextTierLabel.setText("Every mastery task complete");
		}
		else
		{
			int needed = next.threshold(total) - points;
			nextTierLabel.setText(needed + " more for " + next.getDisplayName());
		}
	}

	private void rebuildTasks()
	{
		if (taskList == null)
		{
			return;
		}

		StatusFilter status = (StatusFilter) statusFilter.getSelectedItem();
		String content = (String) contentFilter.getSelectedItem();
		Object category = categoryFilter.getSelectedItem();

		taskListPanel.removeAll();
		for (MasteryTask task : taskList.getTasks())
		{
			boolean done = progress != null && progress.isCompleted(task.getId());
			if ((status == StatusFilter.COMPLETED && !done) || (status == StatusFilter.INCOMPLETE && done))
			{
				continue;
			}
			if (category instanceof TaskCategory && task.getCategory() != category)
			{
				continue;
			}
			if (content != null && !ALL_CONTENT.equals(content) && !Objects.equals(content, task.getContent()))
			{
				continue;
			}
			taskListPanel.add(buildRow(task, done));
		}

		taskListPanel.revalidate();
		taskListPanel.repaint();
	}

	private JPanel buildRow(MasteryTask task, boolean done)
	{
		JPanel row = new JPanel(new BorderLayout(4, 2));
		row.setBackground(done ? COMPLETE_BACKGROUND : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(6, 8, 6, 8)));

		JLabel name = new JLabel(task.getName());
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(done ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);

		JLabel status = new JLabel(statusText(task, done));
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(done ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(name, BorderLayout.CENTER);
		top.add(status, BorderLayout.EAST);

		JLabel description = new JLabel("<html><body style='width:150px'>"
			+ "<span style='color:#a5a5a5'>" + escapeHtml(task.getContent()) + " &middot; " + task.getCategory() + "</span><br>"
			+ escapeHtml(task.getDescription()) + completionText(task)
			+ "</body></html>");
		description.setFont(FontManager.getRunescapeSmallFont());
		description.setForeground(ColorScheme.TEXT_COLOR);

		row.add(top, BorderLayout.NORTH);
		row.add(description, BorderLayout.CENTER);
		row.setAlignmentX(LEFT_ALIGNMENT);
		return row;
	}

	private String statusText(MasteryTask task, boolean done)
	{
		if (done)
		{
			return "Done";
		}
		if (!task.isTracked())
		{
			return "Tracking soon";
		}
		return "1 pt";
	}

	private String completionText(MasteryTask task)
	{
		MasteryProgress.Completion completion = progress == null ? null : progress.getCompletion(task.getId());
		if (completion == null)
		{
			return "";
		}
		String how = completion.isFromPersonalBest() ? "Credited from your record" : "Completed";
		return "<br><span style='color:#37f046'>" + how + " " + DATE_FORMAT.format(new Date(completion.getTimestamp())) + "</span>";
	}

	private static String escapeHtml(String text)
	{
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
