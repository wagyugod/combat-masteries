package com.combatmasteries.ui.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.combatmasteries.tasks.MasteryTask;
import com.combatmasteries.tasks.TaskList;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class ContentIconsTest
{
	@Test
	public void fileNamesAreSlugs()
	{
		assertEquals("chambers_of_xeric_cm.png", ContentIcons.fileName("Chambers of Xeric: CM"));
		assertEquals("theatre_of_blood_hard_mode.png", ContentIcons.fileName("Theatre of Blood: Hard Mode"));
		assertEquals("awakened_dt2.png", ContentIcons.fileName("Awakened DT2"));
	}

	@Test
	public void everyContentHasAnIcon()
	{
		TaskList taskList = TaskList.load(new Gson(), TaskList.class.getResourceAsStream("/com/combatmasteries/tasks.json"));
		for (MasteryTask task : taskList.getTasks())
		{
			BufferedImage icon = ContentIcons.get(task.getContent());
			assertNotNull(task.getContent(), icon);
			assertEquals(task.getContent(), ContentIcons.SIZE, icon.getWidth());
			assertEquals(task.getContent(), ContentIcons.SIZE, icon.getHeight());
		}
	}

	@Test
	public void unknownContentHasNoIcon()
	{
		assertNull(ContentIcons.get("Barbarian Assault"));
		assertNull(ContentIcons.get(null));
	}
}
