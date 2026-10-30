package com.combatmasteries.cosmetic;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.JagexColor;

/**
 * Eye colours for each tier. The model colours are Jagex packed HSL (6-bit hue, 3-bit
 * saturation, 7-bit luminance), stored unsigned; the halo colour is drawn as an overlay
 * around the eyes.
 */
@Getter
@RequiredArgsConstructor
public enum GlowStyle
{
	/**
	 * Vivid red. The stock eyes are HSL 0/7/72 under shading; flat and brighter reads as lit.
	 */
	ASCENDED(hsl(0, 7, 80), hsl(0, 7, 52), new Color(255, 40, 25)),
	/**
	 * Deep saturated blue.
	 */
	EXALTED(hsl(42, 7, 54), hsl(42, 7, 34), new Color(40, 80, 255)),
	/**
	 * The white of Radiant oathplate (its brightest colour is HSL 37999 = 37/0/111).
	 */
	IMMORTAL(hsl(37, 0, 111), hsl(37, 0, 86), new Color(235, 242, 255));

	/**
	 * Packed HSL as an unsigned 16-bit value, the form the model colour arrays use.
	 */
	private final int brightHsl;
	private final int dimHsl;
	private final Color haloColor;

	private static int hsl(int hue, int saturation, int luminance)
	{
		return JagexColor.packHSL(hue, saturation, luminance) & 0xFFFF;
	}

	public static GlowStyle forTier(MasteryTier tier)
	{
		switch (tier)
		{
			case IMMORTAL:
				return IMMORTAL;
			case EXALTED:
				return EXALTED;
			case ASCENDED:
			default:
				return ASCENDED;
		}
	}
}
