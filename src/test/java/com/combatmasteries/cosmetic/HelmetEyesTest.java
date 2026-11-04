package com.combatmasteries.cosmetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Random;
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
		assertFalse(HelmetEyes.isPainted(c1, c3, location, style.brightAt(0)));
		HelmetEyes.paint(c1, c2, c3, location, style.brightAt(0), style.dimAt(0));
		assertTrue(HelmetEyes.isPainted(c1, c3, location, style.brightAt(0)));

		int painted = 0;
		for (int f = 0; f < n; f++)
		{
			if (c3[f] == -1)
			{
				painted++;
				int expected = unlit[f] == HelmetEyes.BRIGHT_EYE_COLOR ? style.brightAt(0) : style.dimAt(0);
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
	public void immortalStartsOnRadiantOathplateWhite()
	{
		assertEquals(37999, GlowStyle.IMMORTAL.brightAt(0));
	}

	@Test
	public void ascendedStaysRedToOrange()
	{
		for (float phase = 0; phase <= 1; phase += 0.1f)
		{
			int hue = (GlowStyle.ASCENDED.brightAt(phase) >> 10) & 63;
			assertTrue("hue " + hue, hue >= 0 && hue <= 5);
		}
	}

	@Test
	public void glowDriftsSubtly()
	{
		assertEquals(0f, GlowStyle.phase(0), 1e-6);
		assertEquals(1f, GlowStyle.phase(GlowStyle.PERIOD_MS / 2), 1e-6);
		for (GlowStyle style : GlowStyle.values())
		{
			int a = style.brightAt(0);
			int b = style.brightAt(1);
			assertTrue(style + " hue", Math.abs(((a >> 10) & 63) - ((b >> 10) & 63)) <= 3);
			assertTrue(style + " luminance", Math.abs((a & 127) - (b & 127)) <= 16);
			assertTrue(style + " dim is darker", (style.dimAt(0.5f) & 127) < (style.brightAt(0.5f) & 127));
		}
	}

	/**
	 * Lit colours as the client produces them: the helmet's hue and saturation per face with
	 * a varying luminance, embedded after {@code before} faces of other equipment.
	 */
	private static int[][] litModel(HelmetEyes.Variant variant, int before)
	{
		int n = before + variant.getFaceCount() + 200;
		int[] c1 = new int[n];
		int[] c3 = new int[n];
		Random random = new Random(7);
		for (int f = 0; f < n; f++)
		{
			// Other equipment: a saturated green, which never matches the helmet
			c1[f] = (21 << 10) | (5 << 7) | random.nextInt(128);
			c3[f] = c1[f];
		}
		for (int face = 0; face < variant.getFaceCount(); face++)
		{
			int saturation = variant.getSaturations().charAt(face) - '0';
			c1[before + face] = (saturation << 7) | (2 + random.nextInt(120));
			c3[before + face] = c1[before + face];
		}
		return new int[][]{c1, c3};
	}

	@Test
	public void findsHelmetFromLitColours()
	{
		for (HelmetEyes.Variant variant : HelmetEyes.Variant.values())
		{
			int[][] model = litModel(variant, 311);
			HelmetEyes.Location location = HelmetEyes.findLit(model[0], model[1], model[0].length);
			assertNotNull(variant.name(), location);
			assertEquals(variant, location.getVariant());
			assertEquals(311, location.getOffset());
		}
	}

	@Test
	public void litSearchToleratesPaintedEyesAndHiddenFaces()
	{
		int[][] model = litModel(HelmetEyes.Variant.MALE, 0);
		HelmetEyes.Location location = HelmetEyes.findLit(model[0], model[1], model[0].length);
		assertNotNull(location);
		GlowStyle style = GlowStyle.EXALTED;
		HelmetEyes.paint(model[0], new int[model[0].length], model[1], location, style.brightAt(0), style.dimAt(0));
		model[1][10] = -2;
		model[0][10] = 12345;

		HelmetEyes.Location again = HelmetEyes.findLit(model[0], model[1], model[0].length);
		assertNotNull(again);
		assertEquals(0, again.getOffset());
	}

	@Test
	public void litSearchRejectsOtherModels()
	{
		int[][] model = litModel(HelmetEyes.Variant.MALE, 50);
		// One face with the wrong saturation is enough to reject
		model[0][50 + 3] = (6 << 7) | 40;
		assertNull(HelmetEyes.findLit(model[0], model[1], model[0].length));
		assertNull(HelmetEyes.findLit(null, null, 0));
	}

	@Test
	public void signaturesCoverEveryFace()
	{
		for (HelmetEyes.Variant variant : HelmetEyes.Variant.values())
		{
			assertEquals(variant.getFaceCount(), variant.getSaturations().length());
		}
	}
}
