package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.image.BufferedImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ItemIconsTest
{
	private static final int RED = 0xffc02020;

	/**
	 * A 36x32 item sprite as the client draws it: a w x h block of colour at (x, y), outlined and shadowed.
	 */
	private static BufferedImage sprite(int x, int y, int w, int h)
	{
		BufferedImage img = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		for (int yy = y - 1; yy <= y + h; yy++)
		{
			for (int xx = x - 1; xx <= x + w; xx++)
			{
				boolean inside = xx >= x && xx < x + w && yy >= y && yy < y + h;
				boolean corner = (xx == x - 1 || xx == x + w) && (yy == y - 1 || yy == y + h);
				if (inside)
				{
					img.setRGB(xx, yy, RED);
				}
				else if (!corner)
				{
					img.setRGB(xx, yy, 0xff000000 | ItemIcons.OUTLINE);
				}
			}
		}
		// Bottom up, as the client does, so the shadow does not spread
		for (int yy = y + h + 1; yy >= y; yy--)
		{
			for (int xx = x + w + 1; xx >= x; xx--)
			{
				if ((img.getRGB(xx, yy) >>> 24) == 0 && (img.getRGB(xx - 1, yy - 1) >>> 24) != 0)
				{
					img.setRGB(xx, yy, 0xff000000 | ItemIcons.ITEM_SHADOW);
				}
			}
		}
		return img;
	}

	private static int count(BufferedImage img, int argb)
	{
		int n = 0;
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				n += img.getRGB(x, y) == argb ? 1 : 0;
			}
		}
		return n;
	}

	@Test
	public void itemThatFitsIsNotScaled()
	{
		BufferedImage icon = ItemIcons.fit(sprite(10, 8, 10, 6), ItemIcons.ITEM_SHADOW, 24);
		assertEquals(24, icon.getWidth());
		assertEquals(24, icon.getHeight());
		assertEquals(60, count(icon, RED));
		// Outline all round (corners left out, as the client draws it) and the shadow
		assertEquals(2 * 10 + 2 * 6, count(icon, 0xff000000 | ItemIcons.OUTLINE));
		assertTrue(count(icon, 0xff000000 | ItemIcons.ITEM_SHADOW) > 0);
	}

	@Test
	public void largeItemIsScaledIntoTheSlotWithAnOutline()
	{
		BufferedImage icon = ItemIcons.fit(sprite(1, 1, 33, 29), ItemIcons.ITEM_SHADOW, 24);
		assertEquals(24, icon.getWidth());
		// 33 wide scales to the 21 left after the outline and shadow, keeping the aspect ratio
		assertEquals(21 * 18, count(icon, RED));
		// Outline, item, outline, shadow across the full width
		assertEquals(0xff000000 | ItemIcons.OUTLINE, icon.getRGB(0, 12));
		assertEquals(RED, icon.getRGB(1, 12));
		assertEquals(RED, icon.getRGB(21, 12));
		assertEquals(0xff000000 | ItemIcons.OUTLINE, icon.getRGB(22, 12));
		assertEquals(0xff000000 | ItemIcons.ITEM_SHADOW, icon.getRGB(23, 12));
	}

	@Test
	public void emptySpriteGivesEmptyIcon()
	{
		BufferedImage icon = ItemIcons.fit(new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB), ItemIcons.ITEM_SHADOW, 24);
		assertEquals(0, 24 * 24 - count(icon, 0));
	}

	@Test
	public void fittedItemsAreCached()
	{
		BufferedImage s = sprite(1, 1, 33, 29);
		assertSame(ItemIcons.fitItem(s, 24), ItemIcons.fitItem(s, 24));
	}

	@Test
	public void smallTierIconsAreTheLargeOnesFitted()
	{
		for (MasteryTier tier : MasteryTier.values())
		{
			BufferedImage small = TierIcons.small(tier);
			assertNotNull(tier.name(), small);
			assertEquals(TierIcons.SMALL, small.getWidth());
			assertEquals(TierIcons.SMALL, small.getHeight());
		}
	}
}
