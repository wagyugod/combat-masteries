package com.combatmasteries.cosmetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class HelmetEyesTest
{
	/**
	 * Builds a fake composed model: {@code before} faces of other equipment, then the
	 * helmet, then 300 more faces. Non-eye faces use the helmet's obsidian greys.
	 */
	private static short[] composedModel(HelmetEyes.Variant variant, int before)
	{
		short[] unlit = new short[before + variant.getFaceCount() + 300];
		Arrays.fill(unlit, (short) 12);
		for (int face : variant.getBrightFaces())
		{
			unlit[before + face] = HelmetEyes.BRIGHT_EYE_COLOR;
		}
		for (int face : variant.getDimFaces())
		{
			unlit[before + face] = HelmetEyes.DIM_EYE_COLOR;
		}
		return unlit;
	}

	@Test
	public void findsMaleHelmetAtStart()
	{
		short[] unlit = composedModel(HelmetEyes.Variant.MALE, 0);
		HelmetEyes.Location location = HelmetEyes.find(unlit, unlit.length);
		assertNotNull(location);
		assertEquals(HelmetEyes.Variant.MALE, location.getVariant());
		assertEquals(0, location.getOffset());
	}

	@Test
	public void findsFemaleHelmetAfterOtherParts()
	{
		short[] unlit = composedModel(HelmetEyes.Variant.FEMALE, 517);
		HelmetEyes.Location location = HelmetEyes.find(unlit, unlit.length);
		assertNotNull(location);
		assertEquals(HelmetEyes.Variant.FEMALE, location.getVariant());
		assertEquals(517, location.getOffset());
	}

	@Test
	public void noHelmet()
	{
		short[] unlit = new short[2000];
		Arrays.fill(unlit, HelmetEyes.BRIGHT_EYE_COLOR);
		assertNull(HelmetEyes.find(unlit, unlit.length));
		assertNull(HelmetEyes.find(null, 0));
		assertNull(HelmetEyes.find(new short[10], 10));
	}

	@Test
	public void paintsOnlyEyeFacesFlat()
	{
		short[] unlit = composedModel(HelmetEyes.Variant.MALE, 40);
		HelmetEyes.Location location = HelmetEyes.find(unlit, unlit.length);
		assertNotNull(location);

		int n = unlit.length;
		int[] c1 = new int[n];
		int[] c2 = new int[n];
		int[] c3 = new int[n];
		Arrays.fill(c1, 5);
		Arrays.fill(c2, 6);
		Arrays.fill(c3, 7);

		GlowStyle style = GlowStyle.EXALTED;
		assertFalse(HelmetEyes.isPainted(c1, c3, location, style.getBrightHsl()));
		HelmetEyes.paint(c1, c2, c3, location, style.getBrightHsl(), style.getDimHsl());
		assertTrue(HelmetEyes.isPainted(c1, c3, location, style.getBrightHsl()));

		int painted = 0;
		for (int f = 0; f < n; f++)
		{
			if (c3[f] == -1)
			{
				painted++;
				int expected = unlit[f] == HelmetEyes.BRIGHT_EYE_COLOR ? style.getBrightHsl() : style.getDimHsl();
				assertEquals(expected, c1[f]);
				assertEquals(expected, c2[f]);
			}
			else
			{
				assertEquals(5, c1[f]);
			}
		}
		assertEquals(8, painted);
	}

	@Test
	public void immortalUsesRadiantOathplateWhite()
	{
		assertEquals(37999, GlowStyle.IMMORTAL.getBrightHsl());
	}

	@Test
	public void ascendedKeepsTheStockRedHue()
	{
		int stockHueSaturation = HelmetEyes.BRIGHT_EYE_COLOR & 0xFF80;
		assertEquals(stockHueSaturation, GlowStyle.ASCENDED.getBrightHsl() & 0xFF80);
		assertEquals(stockHueSaturation, GlowStyle.ASCENDED.getDimHsl() & 0xFF80);
	}
}
