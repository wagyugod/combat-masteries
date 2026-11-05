package com.combatmasteries.ui;

import static org.junit.Assert.assertEquals;
import com.combatmasteries.ui.window.WindowChrome;
import java.awt.Rectangle;
import org.junit.Test;

public class MasteriesWindowTest
{
	@Test
	public void centresInTheViewport()
	{
		Rectangle r = MasteriesWindow.centred(4, 4, 800, 600);
		assertEquals(new Rectangle(4 + (800 - WindowChrome.WIDTH) / 2, 4 + MasteriesWindow.MARGIN, WindowChrome.WIDTH,
			600 - 2 * MasteriesWindow.MARGIN), r);
	}

	@Test
	public void neverShorterThanTheGamesWindow()
	{
		// Fixed mode's 512x334 viewport
		Rectangle r = MasteriesWindow.centred(4, 4, 512, 334);
		assertEquals(MasteriesWindow.MIN_HEIGHT, r.height);
		assertEquals(4 + 16, r.x);
		assertEquals(4 + (334 - MasteriesWindow.MIN_HEIGHT) / 2, r.y);
	}
}
