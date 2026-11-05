package com.combatmasteries.cosmetic;

import com.combatmasteries.tasks.MasteryTier;
import net.runelite.api.JagexColor;

/**
 * Eye colours for each tier. Each tier drifts slowly between two nearby shades, with the
 * brightness travelling across the slit, so the eyes read as a calm glow rather than a flat
 * colour. Every tier also spills a little of its colour onto the metal around the eyes.
 * <p>
 * Model colours are Jagex packed HSL (6-bit hue, 3-bit saturation, 7-bit luminance), handled
 * as unsigned 16-bit values.
 */
public enum GlowStyle
{
	/**
	 * Red, close to the stock eye colour (HSL 0/7/72), shimmering in brightness only.
	 */
	ASCENDED(new int[]{1, 7, 58}, new int[]{1, 7, 68}, true),
	/**
	 * Ice blue drifting to azure: lighter blues read better against the black helmet.
	 */
	EXALTED(new int[]{36, 6, 76}, new int[]{39, 7, 60}, true),
	/**
	 * Radiant oathplate's white (HSL 37999 = 37/0/111) drifting to a faint cool white.
	 */
	IMMORTAL(new int[]{37, 0, 111}, new int[]{37, 2, 100}, true);

	/**
	 * One full drift from the first shade to the second and back.
	 */
	static final long PERIOD_MS = 12000;
	/**
	 * How far the shimmer is spread across the slit, as a share of the cycle, so brightness
	 * travels from one side to the other like the infernal cape's flow.
	 */
	static final float SHIMMER_SPREAD = 0.12f;
	/**
	 * Light spill on the metal: saturation cap and extra luminance for the faces touching the
	 * slit, and for the ring beyond.
	 */
	static final int INNER_RIM_SATURATION = 4;
	static final int INNER_RIM_LUMINANCE = 7;
	static final int OUTER_RIM_SATURATION = 2;
	static final int OUTER_RIM_LUMINANCE = 3;
	/**
	 * Dim eye faces (the slit's corners) are drawn at this share of the bright luminance.
	 */
	private static final double DIM_LUMINANCE = 0.62;

	private final int[] from;
	private final int[] to;
	/**
	 * Whether the glow tints the metal around the slit (all tiers, as the user asked).
	 */
	private final boolean lightsMetal;

	GlowStyle(int[] from, int[] to, boolean lightsMetal)
	{
		this.from = from;
		this.to = to;
		this.lightsMetal = lightsMetal;
	}

	public boolean lightsMetal()
	{
		return lightsMetal;
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

	public int hueAt(float phase)
	{
		return lerp(from[0], to[0], phase);
	}

	public int saturationAt(float phase)
	{
		return lerp(from[1], to[1], phase);
	}

	/**
	 * @return the phase for a point across the slit (-1 left to 1 right), lagging so the
	 * brightness travels across it
	 */
	public static float phaseAcross(long millis, float position)
	{
		return phase(millis - (long) (position * SHIMMER_SPREAD * PERIOD_MS));
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
