package com.combatmasteries.cosmetic;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.Color;
import lombok.Getter;
import net.runelite.api.JagexColor;

/**
 * Eye colours for each tier. Each tier drifts slowly between two nearby shades so the eyes
 * read as a calm glow rather than a flat colour.
 * <p>
 * Model colours are Jagex packed HSL (6-bit hue, 3-bit saturation, 7-bit luminance), handled
 * as unsigned 16-bit values; the halo colours are drawn as an overlay around the eyes.
 */
public enum GlowStyle
{
	/**
	 * Red drifting to orange, staying close to the stock eye colour (HSL 0/7/72).
	 */
	ASCENDED(new int[]{1, 7, 60}, new int[]{4, 7, 66}, new Color(255, 60, 20), new Color(255, 125, 30), 1f),
	/**
	 * Ice blue drifting to azure: lighter blues read better against the black helmet.
	 */
	EXALTED(new int[]{36, 6, 76}, new int[]{39, 7, 60}, new Color(140, 215, 255), new Color(60, 135, 255), 1f),
	/**
	 * Radiant oathplate's white (HSL 37999 = 37/0/111) drifting to a faint cool white.
	 * The halo is weaker so white doesn't haze over the dark helmet.
	 */
	IMMORTAL(new int[]{37, 0, 111}, new int[]{37, 2, 100}, new Color(245, 248, 255), new Color(205, 225, 255), 0.7f);

	/**
	 * One full drift from the first shade to the second and back.
	 */
	static final long PERIOD_MS = 3600;
	/**
	 * Dim eye faces (the slit's corners) are drawn at this share of the bright luminance.
	 */
	private static final double DIM_LUMINANCE = 0.62;

	private final int[] from;
	private final int[] to;
	private final Color haloFrom;
	private final Color haloTo;
	@Getter
	private final float haloStrength;

	GlowStyle(int[] from, int[] to, Color haloFrom, Color haloTo, float haloStrength)
	{
		this.from = from;
		this.to = to;
		this.haloFrom = haloFrom;
		this.haloTo = haloTo;
		this.haloStrength = haloStrength;
	}

	/**
	 * @return 0 to 1 and back over {@link #PERIOD_MS}, easing in and out
	 */
	public static float phase(long millis)
	{
		double angle = 2 * Math.PI * (millis % PERIOD_MS) / PERIOD_MS;
		return (float) (0.5 - 0.5 * Math.cos(angle));
	}

	/**
	 * @return the bright eye colour at this phase, as unsigned packed HSL
	 */
	public int brightAt(float phase)
	{
		return hsl(lerp(from[0], to[0], phase), lerp(from[1], to[1], phase), lerp(from[2], to[2], phase));
	}

	/**
	 * @return the dim eye colour at this phase, as unsigned packed HSL
	 */
	public int dimAt(float phase)
	{
		int luminance = (int) Math.round(lerp(from[2], to[2], phase) * DIM_LUMINANCE);
		return hsl(lerp(from[0], to[0], phase), lerp(from[1], to[1], phase), luminance);
	}

	public Color haloAt(float phase)
	{
		return new Color(
			lerp(haloFrom.getRed(), haloTo.getRed(), phase),
			lerp(haloFrom.getGreen(), haloTo.getGreen(), phase),
			lerp(haloFrom.getBlue(), haloTo.getBlue(), phase));
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

	private static int lerp(int a, int b, float t)
	{
		return Math.round(a + (b - a) * t);
	}

	static int hsl(int hue, int saturation, int luminance)
	{
		return JagexColor.packHSL(hue, saturation, luminance) & 0xFFFF;
	}
}
