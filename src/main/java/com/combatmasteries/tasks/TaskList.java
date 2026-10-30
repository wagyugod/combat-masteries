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

	private void validate()
	{
		Set<String> ids = new HashSet<>();
		for (MasteryTask task : tasks)
		{
			if (task.getId() == null || task.getId().isEmpty())
			{
				throw new IllegalStateException("Task without an id: " + task.getName());
			}
			if (!ids.add(task.getId()))
			{
				throw new IllegalStateException("Duplicate task id: " + task.getId());
			}
			if (task.getConditions() == null || task.getConditions().isEmpty())
			{
				throw new IllegalStateException("Task has no conditions: " + task.getId());
			}
			if (!bosses.containsKey(task.getBoss()))
			{
				throw new IllegalStateException("Task " + task.getId() + " references unknown boss " + task.getBoss());
			}
		}
	}

	private static class Root
	{
		List<BossDefinition> bosses = new ArrayList<>();
		List<MasteryTask> tasks = new ArrayList<>();
	}
}
