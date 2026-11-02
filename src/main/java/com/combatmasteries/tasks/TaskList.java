package com.combatmasteries.tasks;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * The full set of mastery tasks and the bosses they reference, loaded from tasks.json.
 */
public class TaskList
{
	@Getter
	private final List<MasteryTask> tasks;
	private final Map<String, BossDefinition> bosses;

	public TaskList(List<MasteryTask> tasks, List<BossDefinition> bosses)
	{
		this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
		Map<String, BossDefinition> byName = new LinkedHashMap<>();
		for (BossDefinition boss : bosses)
		{
			byName.put(boss.getName(), boss);
		}
		this.bosses = Collections.unmodifiableMap(byName);
		validate();
	}

	public static TaskList load(Gson gson, InputStream in)
	{
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			Root root = gson.fromJson(reader, Root.class);
			return new TaskList(root.tasks, root.bosses);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Unable to read task list", e);
		}
	}

	public int size()
	{
		return tasks.size();
	}

	public Map<String, BossDefinition> getBosses()
	{
		return bosses;
	}

	@Nullable
	public BossDefinition getBoss(String name)
	{
		return bosses.get(name);
	}

	/**
	 * @return true if time tasks for this boss use the raid's total completion time
	 */
	public boolean usesTotalTime(String boss)
	{
		BossDefinition def = bosses.get(boss);
		return def != null && def.isTotalTime();
	}

	private void validate()
	{
		Set<String> ids = new HashSet<>();
		for (MasteryTask task : tasks)
		{
			String id = task.getId();
			if (id == null || id.isEmpty())
			{
				throw new IllegalStateException("Task without an id: " + task.getName());
			}
			if (!ids.add(id))
			{
				throw new IllegalStateException("Duplicate task id: " + id);
			}
			if (task.getName() == null || task.getCategory() == null || task.getContent() == null)
			{
				throw new IllegalStateException("Task " + id + " needs a name, category and content");
			}
			if (!task.isTracked())
			{
				// Listed so it counts towards the total, but has no detection yet
				continue;
			}
			boolean delve = task.hasCondition(ConditionType.MIN_DELVE_LEVEL);
			if (!task.isTrip() && task.getConditions().isEmpty())
			{
				throw new IllegalStateException("Task " + id + " has no conditions or trip");
			}
			if (!task.isTrip() && !delve && task.getBoss() == null)
			{
				throw new IllegalStateException("Task " + id + " needs a boss");
			}
			if (task.hasCondition(ConditionType.INFERNO_NO_PILLARS)
				&& (getBoss(task.getBoss()) == null || getBoss(task.getBoss()).getRegions().isEmpty()))
			{
				throw new IllegalStateException("Task " + id + " needs its boss's region defined");
			}
		}
	}

	private static class Root
	{
		List<BossDefinition> bosses = new ArrayList<>();
		List<MasteryTask> tasks = new ArrayList<>();
	}
}
