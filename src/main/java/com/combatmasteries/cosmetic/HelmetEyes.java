package com.combatmasteries.cosmetic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Locates the eye faces of the Tzkal slayer helmet inside a player's composed model.
 * <p>
 * The helmet's worn models (42733 male, 42736 female) have a red eye slit made of a
 * handful of faces coloured HSL 968 (bright) and 926 (dim). When the client builds a
 * player model it concatenates the equipment models, so the helmet's faces appear as one
 * contiguous block at some offset. The block is found by checking that every eye face
 * index, shifted by a candidate offset, has the expected unlit colour.
 */
public final class HelmetEyes
{
	public static final short BRIGHT_EYE_COLOR = 968;
	public static final short DIM_EYE_COLOR = 926;

	@Getter
	@RequiredArgsConstructor
	public enum Variant
	{
		MALE(472, new int[]{221, 222, 223, 224, 225, 344}, new int[]{226, 343}),
		FEMALE(470, new int[]{221, 222, 223, 224, 343}, new int[]{225, 342});

		private final int faceCount;
		private final int[] brightFaces;
		private final int[] dimFaces;
	}

	/**
	 * Where the helmet sits within a composed model.
	 */
	@Getter
	@RequiredArgsConstructor
	public static final class Location
	{
		private final Variant variant;
		private final int offset;
	}

	private HelmetEyes()
	{
	}

	/**
	 * @param unlitColors the composed model's unlit face colours
	 * @return the helmet's location, or null if the model does not contain it
	 */
	public static Location find(short[] unlitColors, int faceCount)
	{
		if (unlitColors == null)
		{
			return null;
		}

		int limit = Math.min(faceCount, unlitColors.length);
		for (Variant variant : Variant.values())
		{
			for (int offset = 0; offset + variant.faceCount <= limit; offset++)
			{
				if (matches(unlitColors, variant, offset))
				{
					return new Location(variant, offset);
				}
			}
		}
		return null;
	}

	static boolean matches(short[] unlitColors, Variant variant, int offset)
	{
		for (int face : variant.brightFaces)
		{
			if (unlitColors[offset + face] != BRIGHT_EYE_COLOR)
			{
				return false;
			}
		}
		for (int face : variant.dimFaces)
		{
			if (unlitColors[offset + face] != DIM_EYE_COLOR)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Paints the eye faces with flat (unshaded) colours, which reads as a glow.
	 * <p>
	 * In the client's face colour arrays, a third colour of -1 means "flat shaded, use the
	 * first colour".
	 */
	public static void paint(int[] colors1, int[] colors2, int[] colors3, Location location, int brightHsl, int dimHsl)
	{
		int offset = location.getOffset();
		for (int face : location.getVariant().brightFaces)
		{
			setFlat(colors1, colors2, colors3, offset + face, brightHsl);
		}
		for (int face : location.getVariant().dimFaces)
		{
			setFlat(colors1, colors2, colors3, offset + face, dimHsl);
		}
	}

	/**
	 * @return true if the eye faces already carry the given colours
	 */
	public static boolean isPainted(int[] colors1, int[] colors3, Location location, int brightHsl)
	{
		int face = location.getOffset() + location.getVariant().brightFaces[0];
		return colors1[face] == brightHsl && colors3[face] == -1;
	}

	private static void setFlat(int[] colors1, int[] colors2, int[] colors3, int face, int hsl)
	{
		colors1[face] = hsl;
		colors2[face] = hsl;
		colors3[face] = -1;
	}
}
