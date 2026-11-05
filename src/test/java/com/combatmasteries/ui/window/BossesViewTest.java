package com.combatmasteries.ui.window;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class BossesViewTest
{
	@Test
	public void cardsSortByShownNameIgnoringThe()
	{
		List<WindowModel.Content> contents = new ArrayList<>();
		for (String name : WindowModel.CONTENT_ORDER)
		{
			contents.add(new WindowModel.Content(name, 0, 0));
		}

		List<String> order = new ArrayList<>();
		for (WindowModel.Content c : BossesView.sortedByCardName(contents))
		{
			order.add(c.getName());
		}

		assertEquals(Arrays.asList(
			"Awakened Bosses",
			"Chambers of Xeric: CM",
			"Doom of Mokhaiotl",
			"Colosseum",
			"Theatre of Blood",
			"Theatre of Blood: Hard Mode",
			"Tombs of Amascut",
			"Inferno",
			"Yama Contracts"), order);
	}
}
