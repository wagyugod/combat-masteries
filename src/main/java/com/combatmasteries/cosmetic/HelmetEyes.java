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
 * <p>
 * The client does not always keep unlit colours for player models. In that case the block
 * is found from the lit colours instead: lighting changes a face's luminance but keeps its
 * hue and saturation, and every face of the helmet has hue 0 and a known saturation.
 */
public final class HelmetEyes
{
	public static final short BRIGHT_EYE_COLOR = 968;
	public static final short DIM_EYE_COLOR = 926;

	@Getter
	@RequiredArgsConstructor
	public enum Variant
	{
		MALE(472, new int[]{221, 222, 223, 224, 225, 344}, new int[]{226, 343},
			new float[]{-0.25f, -0.08f, 0.08f, 0.25f, 0.42f, -0.42f}, new float[]{0.67f, -0.67f},
			new int[]{105, 106, 107, 219, 220, 227, 228, 332, 333, 334, 335, 342, 370, 371, 372, 373, 420, 421},
			new int[]{43, 84, 85, 96, 97, 102, 103, 104, 108, 109, 217, 218, 229, 287, 336, 337, 350, 375, 376, 377, 387, 388, 389, 419},
			"00220000000000000000220000000000000000000000004440000020002200000002000000000000"
			+ "00000000004400000002244224442200000440000440000000000000000004444444000444444000"
			+ "00000044444440000444444400000000000004444440002422224222222447777774420044004400"
			+ "44422444004444444224422224400004422244444000000004422244224444444422224444224422"
			+ "22422244024422222200224770224244424400000000222444222222200000000000220000000222"
			+ "000000000000000000024400000000000220000000000000000022000000020000000404"),
		FEMALE(470, new int[]{221, 222, 223, 224, 343}, new int[]{225, 342},
			new float[]{-0.21f, 0.0f, 0.21f, 0.38f, -0.38f}, new float[]{0.58f, -0.58f},
			new int[]{105, 106, 107, 219, 220, 226, 227, 331, 332, 333, 334, 341, 369, 370, 371, 372, 419, 420},
			new int[]{43, 84, 85, 96, 97, 102, 103, 104, 108, 109, 217, 218, 228, 286, 335, 336, 349, 374, 375, 376, 386, 387, 388, 418},
			"00220000000000000000220000000000000000000000004440000020002200000002000000000000"
			+ "00000000004400000002244224442200000440000440000000000000000004444444000444444000"
			+ "00000044444440000444444400000000000004444440002422224222222447777744200440044004"
			+ "44224440044444442244222244000044222444440000000044222442244444444222244442244222"
			+ "24222440244222222002247702242444244000000002224442222222000000000002200000002220"
			+ "0000000000000000002440000000000022000000000000000002200000002000000044");

		private final int faceCount;
		private final int[] brightFaces;
		private final int[] dimFaces;
		/**
		 * Horizontal position of each bright and dim face across the slit, -1 (left) to 1 (right).
		 */
		private final float[] brightX;
		private final float[] dimX;
		/**
		 * Helmet faces touching the slit, and the ring beyond, at or above its bottom edge.
		 * The glow tints these so it appears to light the metal around the eyes.
		 */
		private final int[] innerRim;
		private final int[] outerRim;
		/**
		 * Saturation of every face in the cache model (all faces have hue 0), one digit per face.
		 */
		private final String saturations;

		boolean isEye(int face)
		{
			for (int f : brightFaces)
			{
				if (f == face)
				{
					return true;
				}
			}
			for (int f : dimFaces)
			{
				if (f == face)
				{
					return true;
				}
			}
			return false;
		}
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

	/**
	 * Finds the helmet from lit colours, for models without unlit colours.
	 *
	 * @param colors1 the model's first face colours (lit packed HSL)
	 * @param colors3 the model's third face colours, where -2 marks a hidden face
	 * @return the helmet's location, or null if the model does not contain it
	 */
	public static Location findLit(int[] colors1, int[] colors3, int faceCount)
	{
		if (colors1 == null || colors3 == null)
		{
			return null;
		}

		int limit = Math.min(faceCount, Math.min(colors1.length, colors3.length));
		for (Variant variant : Variant.values())
		{
			for (int offset = 0; offset + variant.faceCount <= limit; offset++)
			{
				if (matchesLit(colors1, colors3, variant, offset))
				{
					return new Location(variant, offset);
				}
			}
		}
		return null;
	}

	static boolean matchesLit(int[] colors1, int[] colors3, Variant variant, int offset)
	{
		for (int face = 0; face < variant.faceCount; face++)
		{
			int f = offset + face;
			if (colors3[f] == -2 || variant.isEye(face))
			{
				// Hidden faces carry no colour; eye faces may already be painted
				continue;
			}
			int hsl = colors1[f];
			int hue = (hsl >> 10) & 63;
			int saturation = (hsl >> 7) & 7;
			if (hue != 0 || saturation != variant.saturations.charAt(face) - '0')
			{
				return false;
			}
		}
		return true;
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

	/**
	 * Recolours a lit colour towards a glow: the glow's hue and saturation, with the face's
	 * own shading kept and brightened by {@code addLuminance}. -1 and -2 (flat and hidden
	 * markers) pass through unchanged.
	 */
	public static int tint(int litHsl, int hue, int saturation, int addLuminance)
	{
		if (litHsl < 0)
		{
			return litHsl;
		}
		int luminance = Math.min(126, (litHsl & 127) + addLuminance);
		return (hue << 10) | (saturation << 7) | luminance;
	}

	private static void setFlat(int[] colors1, int[] colors2, int[] colors3, int face, int hsl)
	{
		colors1[face] = hsl;
		colors2[face] = hsl;
		colors3[face] = -1;
	}
}
