package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fits an item sprite into the square slot where the Combat Achievements window draws a tier icon.
 * <p>
 * An item sprite (36x32, as {@code ItemManager} hands it out) is the item's pixels with a 1px outline of
 * colour {@link #OUTLINE} on every side and a drop shadow to the bottom right. Resampling the whole sprite
 * would smear the outline into the item, so the outline and shadow are taken off, the item is scaled down
 * on its own (only when it does not fit already), and the outline and shadow are put back around it the
 * way the client draws them ({@code SpritePixels.drawBorder} and {@code drawShadow}). The result keeps the
 * sprite's own shading and a crisp outline at the slot's size.
 */
public final class ItemIcons
{
	/**
	 * The outline colour of item sprites (border 1).
	 */
	public static final int OUTLINE = 0x000001;
	/**
	 * The drop shadow {@code ItemManager} draws item sprites with ({@code SpritePixels.DEFAULT_SHADOW_COLOR}).
	 */
	public static final int ITEM_SHADOW = 0x302020;

	/**
	 * Scaled pixels covered less than this are left out. Below one half so a 1px handle or shaft that the
	 * scaling spreads over two pixels still shows.
	 */
	private static final double COVERAGE = 0.25;
	/**
	 * A source pixel this far (summed over red, green and blue) from the average of the pixels it is
	 * scaled down with keeps its own colour, if it covers at least {@link #ACCENT_COVERAGE} of the target
	 * pixel: small bright details (a helmet's eyes, a gem, a highlight) stay instead of fading into the
	 * pixels around them.
	 */
	private static final int ACCENT_DISTANCE = 120;
	private static final double ACCENT_COVERAGE = 0.3;

	/**
	 * Fitted icons by source image and size. The source is the item manager's image, which it keeps.
	 */
	private static final Map<BufferedImage, BufferedImage[]> CACHE = new WeakHashMap<>();

	private ItemIcons()
	{
	}

	/**
	 * {@link #fit(BufferedImage, int, int)} for sprites from {@code ItemManager}, cached per image and size.
	 */
	public static synchronized BufferedImage fitItem(BufferedImage sprite, int size)
	{
		BufferedImage[] fitted = CACHE.get(sprite);
		if (fitted == null || fitted.length <= size)
		{
			fitted = new BufferedImage[size + 1];
			CACHE.put(sprite, fitted);
		}
		if (fitted[size] == null)
		{
			fitted[size] = fit(sprite, ITEM_SHADOW, size);
		}
		return fitted[size];
	}

	/**
	 * @param sprite an item sprite: the item, its {@link #OUTLINE} outline and its drop shadow
	 * @param shadow the colour of the sprite's drop shadow
	 * @param size the slot's width and height
	 * @return a size x size icon: the item centred, scaled down to fit when it has to, outlined and shadowed
	 */
	public static BufferedImage fit(BufferedImage sprite, int shadow, int size)
	{
		int sw = sprite.getWidth(), sh = sprite.getHeight();
		int[] src = sprite.getRGB(0, 0, sw, sh, null, 0, sw);
		boolean[] item = outside(src, sw, sh, shadow);
		int x0 = sw, y0 = sh, x1 = -1, y1 = -1;
		for (int y = 0; y < sh; y++)
		{
			for (int x = 0; x < sw; x++)
			{
				int i = y * sw + x;
				item[i] = opaque(src[i]) && !item[i];
				if (item[i])
				{
					x0 = Math.min(x0, x);
					y0 = Math.min(y0, y);
					x1 = Math.max(x1, x);
					y1 = Math.max(y1, y);
				}
			}
		}
		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		if (x1 < 0)
		{
			return out;
		}

		// The item, without outline and shadow, scaled to leave room for both: 1px all round, and 1px more
		// at the bottom right for the shadow
		int w = x1 - x0 + 1, h = y1 - y0 + 1, room = size - 3;
		double scale = Math.min(1, Math.min((double) room / w, (double) room / h));
		int dw = Math.max(1, (int) Math.round(w * scale)), dh = Math.max(1, (int) Math.round(h * scale));
		int[] scaled = scale == 1 ? crop(src, item, sw, x0, y0, w, h) : downscale(src, item, sw, x0, y0, w, h, dw, dh);

		int[] px = new int[size * size];
		int ox = 1 + (room - dw + 1) / 2, oy = 1 + (room - dh + 1) / 2;
		for (int y = 0; y < dh; y++)
		{
			System.arraycopy(scaled, y * dw, px, (oy + y) * size + ox, dw);
		}
		outline(px, size);
		shadow(px, size, shadow);
		out.setRGB(0, 0, size, size, px, 0, size);
		return out;
	}

	private static boolean opaque(int argb)
	{
		return (argb >>> 24) != 0;
	}

	/**
	 * The sprite's outline and shadow: the pixels of their colours reached from the sprite's edge or an
	 * empty pixel through other such pixels. A dark pixel inside the item stays part of it.
	 *
	 * @return per pixel, whether it is outline or shadow (or empty)
	 */
	private static boolean[] outside(int[] src, int w, int h, int shadow)
	{
		boolean[] seen = new boolean[w * h];
		int[] stack = new int[w * h];
		int top = 0;
		for (int i = 0; i < src.length; i++)
		{
			if (!opaque(src[i]))
			{
				seen[i] = true;
				stack[top++] = i;
			}
		}
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int i = y * w + x;
				if ((x == 0 || y == 0 || x == w - 1 || y == h - 1) && !seen[i] && border(src[i], shadow))
				{
					seen[i] = true;
					stack[top++] = i;
				}
			}
		}
		while (top > 0)
		{
			int i = stack[--top], x = i % w, y = i / w;
			for (int dy = -1; dy <= 1; dy++)
			{
				for (int dx = -1; dx <= 1; dx++)
				{
					int nx = x + dx, ny = y + dy;
					if (nx >= 0 && ny >= 0 && nx < w && ny < h)
					{
						int n = ny * w + nx;
						if (!seen[n] && border(src[n], shadow))
						{
							seen[n] = true;
							stack[top++] = n;
						}
					}
				}
			}
		}
		return seen;
	}

	private static boolean border(int argb, int shadow)
	{
		int rgb = argb & 0xffffff;
		return opaque(argb) && (rgb == OUTLINE || rgb == shadow);
	}

	private static int[] crop(int[] src, boolean[] item, int sw, int x0, int y0, int w, int h)
	{
		int[] out = new int[w * h];
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int i = (y0 + y) * sw + x0 + x;
				out[y * w + x] = item[i] ? src[i] | 0xff000000 : 0;
			}
		}
		return out;
	}

	/**
	 * Box-filter downscale: each target pixel averages the item pixels under it, weighted by how much of
	 * each it covers. Item sprites have no partial transparency, so neither do the results.
	 */
	private static int[] downscale(int[] src, boolean[] item, int sw, int x0, int y0, int w, int h, int dw, int dh)
	{
		int[] out = new int[dw * dh];
		double fx = (double) w / dw, fy = (double) h / dh;
		for (int ty = 0; ty < dh; ty++)
		{
			double sy0 = ty * fy, sy1 = sy0 + fy;
			for (int tx = 0; tx < dw; tx++)
			{
				double sx0 = tx * fx, sx1 = sx0 + fx;
				double cover = 0, r = 0, g = 0, b = 0;
				double accentWeight = 0;
				int accent = 0, accentDistance = 0;
				for (int y = (int) sy0; y < Math.min(h, Math.ceil(sy1)); y++)
				{
					double wy = Math.min(sy1, y + 1) - Math.max(sy0, y);
					for (int x = (int) sx0; x < Math.min(w, Math.ceil(sx1)); x++)
					{
						int i = (y0 + y) * sw + x0 + x;
						if (!item[i])
						{
							continue;
						}
						double wt = wy * (Math.min(sx1, x + 1) - Math.max(sx0, x));
						int c = src[i];
						cover += wt;
						r += wt * ((c >> 16) & 0xff);
						g += wt * ((c >> 8) & 0xff);
						b += wt * (c & 0xff);
					}
				}
				if (cover >= COVERAGE * fx * fy)
				{
					int rgb = (int) Math.round(r / cover) << 16 | (int) Math.round(g / cover) << 8 | (int) Math.round(b / cover);
					// The source pixel that stands out most from that average
					for (int y = (int) sy0; y < Math.min(h, Math.ceil(sy1)); y++)
					{
						double wy = Math.min(sy1, y + 1) - Math.max(sy0, y);
						for (int x = (int) sx0; x < Math.min(w, Math.ceil(sx1)); x++)
						{
							int i = (y0 + y) * sw + x0 + x;
							int dist = item[i] ? distance(src[i], rgb) : 0;
							double wt = wy * (Math.min(sx1, x + 1) - Math.max(sx0, x));
							if (dist > accentDistance && wt >= ACCENT_COVERAGE * fx * fy)
							{
								accent = src[i] & 0xffffff;
								accentDistance = dist;
								accentWeight = wt;
							}
						}
					}
					if (accentDistance >= ACCENT_DISTANCE && accentWeight > 0)
					{
						rgb = accent;
					}
					// Never the outline colour or transparent black, which would read as outline
					out[ty * dw + tx] = 0xff000000 | (rgb == OUTLINE || rgb == 0 ? 0x010101 : rgb);
				}
			}
		}
		return out;
	}

	private static int distance(int a, int b)
	{
		return Math.abs(((a >> 16) & 0xff) - ((b >> 16) & 0xff)) + Math.abs(((a >> 8) & 0xff) - ((b >> 8) & 0xff))
			+ Math.abs((a & 0xff) - (b & 0xff));
	}

	/**
	 * {@code SpritePixels.drawBorder}: every empty pixel next to (not diagonal to) the item.
	 */
	private static void outline(int[] px, int size)
	{
		int[] old = px.clone();
		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				int i = y * size + x;
				if (old[i] == 0 && ((x > 0 && old[i - 1] != 0) || (y > 0 && old[i - size] != 0)
					|| (x < size - 1 && old[i + 1] != 0) || (y < size - 1 && old[i + size] != 0)))
				{
					px[i] = 0xff000000 | OUTLINE;
				}
			}
		}
	}

	/**
	 * {@code SpritePixels.drawShadow}: every empty pixel with the item or its outline up and to the left.
	 */
	private static void shadow(int[] px, int size, int color)
	{
		for (int y = size - 1; y > 0; y--)
		{
			for (int x = size - 1; x > 0; x--)
			{
				int i = y * size + x;
				if (px[i] == 0 && px[i - 1 - size] != 0)
				{
					px[i] = 0xff000000 | color;
				}
			}
		}
	}
}
