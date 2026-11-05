package com.combatmasteries.ui;

import com.combatmasteries.tasks.MasteryProgress;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.MasteryTier;
import com.combatmasteries.tasks.TaskCategory;
import com.combatmasteries.tasks.TaskList;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ImageUtil;

/**
 * Side panel styled after the in-game Combat Achievements interface: an Overview tab with
 * each tier's progress and reward, and a Tasks tab with filters and the task list.
 */
public class MasteriesPanel extends PluginPanel
{
	// Colours of the game's stone interfaces
	static final Color STONE = new Color(62, 53, 41);
	static final Color STONE_LIGHT = new Color(73, 63, 50);
	static final Color STONE_DARK = new Color(45, 39, 31);
	static final Color EDGE = new Color(24, 20, 16);
	static final Color ORANGE = new Color(255, 152, 31);
	static final Color YELLOW = new Color(255, 255, 0);
	static final Color GREEN = new Color(13, 193, 13);
	static final Color TEXT = new Color(235, 230, 215);
	static final Color MUTED = new Color(170, 160, 140);
	static final Color BAR_EMPTY = new Color(96, 16, 16);
	static final Color BAR_FILL = new Color(36, 150, 36);

	private static final String OVERVIEW = "Overview";
	private static final String TASKS = "Tasks";
	private static final String ALL = "All";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy");

	private enum Status
	{
		ALL("All"),
		INCOMPLETE("Incomplete"),
		COMPLETED("Completed");

		private final String label;

		Status(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final CardLayout cards = new CardLayout();
	private final JPanel cardPanel = new JPanel(cards);
	private final JButton overviewTab = tabButton(OVERVIEW);
	private final JButton tasksTab = tabButton(TASKS);

	private final JShadowedLabel totalLabel = shadowed("", TEXT, false);
	private final JPanel tierRows = new JPanel();
	private final Map<MasteryTier, BufferedImage> tierIcons = new EnumMap<>(MasteryTier.class);

	private final JComboBox<Object> typeFilter = new JComboBox<>();
	private final JComboBox<String> contentFilter = new JComboBox<>();
	private final JComboBox<Status> statusFilter = new JComboBox<>(Status.values());
	private final JShadowedLabel shownLabel = shadowed("", MUTED, false);
	private final JPanel taskRows = new JPanel();

	private TaskList taskList;
	private MasteryProgress progress;

	public MasteriesPanel()
	{
		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(STONE_DARK);
		setLayout(new BorderLayout(0, 6));

		for (MasteryTier tier : MasteryTier.values())
		{
			tierIcons.put(tier, ImageUtil.loadImageResource(MasteriesPanel.class, "/com/combatmasteries/tier_" + tier.name().toLowerCase() + ".png"));
		}

		JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setOpaque(false);
		JShadowedLabel title = shadowed("Combat Masteries", ORANGE, true);
		title.setHorizontalAlignment(JLabel.CENTER);
		header.add(title, BorderLayout.NORTH);

		JPanel tabs = new JPanel(new GridLayout(1, 2, 4, 0));
		tabs.setOpaque(false);
		tabs.add(overviewTab);
		tabs.add(tasksTab);
		header.add(tabs, BorderLayout.SOUTH);
		overviewTab.addActionListener(e -> showCard(OVERVIEW));
		tasksTab.addActionListener(e -> showCard(TASKS));

		cardPanel.setOpaque(false);
		cardPanel.add(buildOverview(), OVERVIEW);
		cardPanel.add(buildTasks(), TASKS);

		add(header, BorderLayout.NORTH);
		add(cardPanel, BorderLayout.CENTER);
		showCard(OVERVIEW);
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
			contentFilter.addItem(ALL);
			taskList.getTasks().stream().map(MasteryTask::getContent).distinct().forEach(contentFilter::addItem);
			contentFilter.setSelectedItem(selected != null ? selected : ALL);
		}

		rebuildOverview();
		rebuildTasks();
	}

	private JPanel buildOverview()
	{
		JPanel overview = new JPanel();
		overview.setOpaque(false);
		overview.setLayout(new BoxLayout(overview, BoxLayout.Y_AXIS));

		JPanel summary = stonePanel(new BorderLayout());
		summary.add(totalLabel, BorderLayout.CENTER);
		summary.setAlignmentX(LEFT_ALIGNMENT);
		overview.add(summary);
		overview.add(spacer());

		tierRows.setOpaque(false);
		tierRows.setLayout(new BoxLayout(tierRows, BoxLayout.Y_AXIS));
		tierRows.setAlignmentX(LEFT_ALIGNMENT);
		overview.add(tierRows);

		JPanel reward = stonePanel(new BorderLayout());
		JLabel rewardText = new JLabel("<html><body style='width:160px'>Each tier makes the eyes of your Tzkal Slayer Helmet glow."
			+ " Only you can see it, and it shows while you wear the helmet.</body></html>");
		rewardText.setFont(FontManager.getRunescapeSmallFont());
		rewardText.setForeground(MUTED);
		reward.add(rewardText, BorderLayout.CENTER);
		reward.setAlignmentX(LEFT_ALIGNMENT);
		overview.add(spacer());
		overview.add(reward);
		return overview;
	}

	private JPanel buildTasks()
	{
		JPanel tasks = new JPanel(new BorderLayout(0, 6));
		tasks.setOpaque(false);

		typeFilter.addItem(ALL);
		for (TaskCategory category : TaskCategory.values())
		{
			typeFilter.addItem(category);
		}

		JPanel filters = stonePanel(new GridLayout(3, 2, 4, 4));
		filters.add(shadowed("Type", ORANGE, false));
		filters.add(typeFilter);
		filters.add(shadowed("Content", ORANGE, false));
		filters.add(contentFilter);
		filters.add(shadowed("Completed", ORANGE, false));
		filters.add(statusFilter);
		for (JComboBox<?> box : new JComboBox<?>[]{typeFilter, contentFilter, statusFilter})
		{
			box.setFont(FontManager.getRunescapeSmallFont());
			box.addActionListener(e -> rebuildTasks());
		}

		JPanel top = new JPanel(new BorderLayout(0, 4));
		top.setOpaque(false);
		top.add(filters, BorderLayout.NORTH);
		top.add(shownLabel, BorderLayout.SOUTH);

		taskRows.setOpaque(false);
		taskRows.setLayout(new BoxLayout(taskRows, BoxLayout.Y_AXIS));

		tasks.add(top, BorderLayout.NORTH);
		tasks.add(taskRows, BorderLayout.CENTER);
		return tasks;
	}

	private void showCard(String name)
	{
		cards.show(cardPanel, name);
		styleTab(overviewTab, OVERVIEW.equals(name));
		styleTab(tasksTab, TASKS.equals(name));
	}

	private void rebuildOverview()
	{
		if (taskList == null)
		{
			return;
		}

		int total = taskList.size();
		int points = progress == null ? 0 : progress.countCompleted(taskList);
		MasteryTier reached = MasteryTier.forPoints(points, total);
		totalLabel.setText(progress == null
			? "Log in to see your progress"
			: "Total points: " + points + " / " + total);

		tierRows.removeAll();
		for (MasteryTier tier : MasteryTier.values())
		{
			int needed = tier.threshold(total);
			boolean unlocked = progress != null && reached != null && reached.ordinal() >= tier.ordinal();

			JPanel row = stonePanel(new BorderLayout(8, 0));
			row.setAlignmentX(LEFT_ALIGNMENT);

			JLabel icon = new JLabel(new ImageIcon(tierIcons.get(tier)));
			row.add(icon, BorderLayout.WEST);

			JPanel right = new JPanel(new GridLayout(3, 1, 0, 2));
			right.setOpaque(false);
			right.add(shadowed(tier.getDisplayName(), ORANGE, true));
			right.add(new ProgressBar(Math.min(points, needed), needed));
			right.add(shadowed(unlocked ? "Unlocked" : tier.getPercent() + "% of points (" + needed + ")", unlocked ? GREEN : MUTED, false));
			row.add(right, BorderLayout.CENTER);

			tierRows.add(row);
			tierRows.add(spacer());
		}
		tierRows.revalidate();
		tierRows.repaint();
	}

	private void rebuildTasks()
	{
		if (taskList == null)
		{
			return;
		}

		Object type = typeFilter.getSelectedItem();
		String content = (String) contentFilter.getSelectedItem();
		Status status = (Status) statusFilter.getSelectedItem();

		taskRows.removeAll();
		int shown = 0;
		for (MasteryTask task : taskList.getTasks())
		{
			boolean done = progress != null && progress.isCompleted(task.getId());
			if ((status == Status.COMPLETED && !done) || (status == Status.INCOMPLETE && done))
			{
				continue;
			}
			if (type instanceof TaskCategory && task.getCategory() != type)
			{
				continue;
			}
			if (content != null && !ALL.equals(content) && !Objects.equals(content, task.getContent()))
			{
				continue;
			}
			taskRows.add(buildTaskRow(task, done, shown % 2 == 0));
			shown++;
		}
		shownLabel.setText("Showing " + shown + " of " + taskList.size() + " tasks");
		taskRows.revalidate();
		taskRows.repaint();
	}

	private JPanel buildTaskRow(MasteryTask task, boolean done, boolean even)
	{
		JPanel row = new JPanel(new BorderLayout(4, 2));
		row.setBackground(even ? STONE : STONE_LIGHT);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, EDGE),
			new EmptyBorder(5, 6, 5, 6)));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel name = new JLabel("<html><body style='width:140px'>" + escapeHtml(task.getName()) + "</body></html>");
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(done ? GREEN : ORANGE);

		String status = done ? "Done" : task.isTracked() ? "1 pt" : "Soon";
		JShadowedLabel points = shadowed(status, done ? GREEN : task.isTracked() ? YELLOW : MUTED, false);
		points.setVerticalAlignment(JLabel.TOP);

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(name, BorderLayout.CENTER);
		top.add(points, BorderLayout.EAST);

		JLabel description = new JLabel("<html><body style='width:155px'>"
			+ escapeHtml(task.getDescription())
			+ "<br><span style='color:#aaa08c'>" + task.getCategory() + " &middot; " + escapeHtml(task.getContent()) + "</span>"
			+ completionText(task)
			+ "</body></html>");
		description.setFont(FontManager.getRunescapeSmallFont());
		description.setForeground(TEXT);

		row.add(top, BorderLayout.NORTH);
		row.add(description, BorderLayout.CENTER);
		return row;
	}

	private String completionText(MasteryTask task)
	{
		MasteryProgress.Completion completion = progress == null ? null : progress.getCompletion(task.getId());
		if (completion == null)
		{
			return task.isTracked() ? "" : "<br><span style='color:#aaa08c'>Tracking coming soon</span>";
		}
		String how = completion.isFromPersonalBest() ? "Credited from your record" : "Completed";
		return "<br><span style='color:#0dc10d'>" + how + " " + DATE_FORMAT.format(new Date(completion.getTimestamp())) + "</span>";
	}

	private static JPanel stonePanel(LayoutManager layout)
	{
		// In a vertical box, only ever as tall as the content needs
		JPanel panel = new JPanel(layout)
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		panel.setBackground(STONE);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(EDGE),
			new EmptyBorder(6, 6, 6, 6)));
		return panel;
	}

	private static JComponent spacer()
	{
		JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		spacer.setPreferredSize(new Dimension(0, 6));
		spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		spacer.setAlignmentX(LEFT_ALIGNMENT);
		return spacer;
	}

	private static JShadowedLabel shadowed(String text, Color color, boolean bold)
	{
		JShadowedLabel label = new JShadowedLabel(text);
		label.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setShadow(Color.BLACK);
		label.setShadowSize(new Point(1, 1));
		return label;
	}

	private static JButton tabButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeBoldFont());
		button.setFocusPainted(false);
		return button;
	}

	private static void styleTab(JButton tab, boolean selected)
	{
		tab.setBackground(selected ? STONE_LIGHT : STONE_DARK);
		tab.setForeground(selected ? ORANGE : MUTED);
		tab.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(selected ? ORANGE.darker() : EDGE),
			new EmptyBorder(4, 4, 4, 4)));
	}

	private static String escapeHtml(String text)
	{
		return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * A game-style progress bar: green over dark red, with the count in shadowed white.
	 */
	private static final class ProgressBar extends JComponent
	{
		private final int value;
		private final int maximum;

		ProgressBar(int value, int maximum)
		{
			this.value = value;
			this.maximum = Math.max(1, maximum);
			setPreferredSize(new Dimension(100, 14));
			setFont(FontManager.getRunescapeSmallFont());
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			int w = getWidth();
			int h = getHeight();
			g.setColor(EDGE);
			g.fillRect(0, 0, w, h);
			g.setColor(BAR_EMPTY);
			g.fillRect(1, 1, w - 2, h - 2);
			g.setColor(BAR_FILL);
			g.fillRect(1, 1, (w - 2) * value / maximum, h - 2);

			String text = value + " / " + maximum;
			FontMetrics fm = g.getFontMetrics(getFont());
			int x = (w - fm.stringWidth(text)) / 2;
			int y = (h - fm.getHeight()) / 2 + fm.getAscent();
			g.setFont(getFont());
			g.setColor(Color.BLACK);
			g.drawString(text, x + 1, y + 1);
			g.setColor(Color.WHITE);
			g.drawString(text, x, y);
		}
	}
}
