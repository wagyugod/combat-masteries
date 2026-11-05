package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

/**
 * The tier helmet icons: 32x32 as drawn in the Rewards list (where the game draws a 36x32 item), and
 * scaled to 24x24 for the places the game draws a tier sword (Overview tier boxes, Rewards tier buttons).
 */
public final class TierIcons
{
	public static final int LARGE = 32;
	public static final int SMALL = 24;

	private static final Map<MasteryTier, BufferedImage> LARGE_ICONS = new EnumMap<>(MasteryTier.class);
	private static final Map<MasteryTier, BufferedImage> SMALL_ICONS = new EnumMap<>(MasteryTier.class);

	private TierIcons()
	{
	}

	@Nullable
	public static synchronized BufferedImage large(MasteryTier tier)
	{
		if (!LARGE_ICONS.containsKey(tier))
		{
			LARGE_ICONS.put(tier, load(tier));
		}
		return LARGE_ICONS.get(tier);
	}

	@Nullable
	public static synchronized BufferedImage small(MasteryTier tier)
	{
		if (!SMALL_ICONS.containsKey(tier))
		{
			BufferedImage large = large(tier);
			SMALL_ICONS.put(tier, large == null ? null : shrink(large, SMALL));
		}
		return SMALL_ICONS.get(tier);
	}

	/**
	 * Scales the icon's opaque part down to fit a size x size box, centred, with hard alpha edges and a
	 * fresh 1px black outline like a game sprite (scaling smears the original outline).
	 */
	static BufferedImage shrink(BufferedImage src, int size)
	{
		int minX = src.getWidth();
		int minY = src.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < src.getHeight(); y++)
		{
			for (int x = 0; x < src.getWidth(); x++)
			{
				if ((src.getRGB(x, y) >>> 24) != 0)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		if (maxX < 0)
		{
			return out;
		}
		int w = maxX - minX + 1;
		int h = maxY - minY + 1;
		int inner = size - 2;
		double scale = Math.min(1.0, Math.min((double) inner / w, (double) inner / h));
		int sw = Math.max(1, (int) Math.round(w * scale));
		int sh = Math.max(1, (int) Math.round(h * scale));
		Image scaled = src.getSubimage(minX, minY, w, h).getScaledInstance(sw, sh, Image.SCALE_AREA_AVERAGING);
		Graphics2D g = out.createGraphics();
		g.drawImage(scaled, (size - sw) / 2, (size - sh) / 2, null);
		g.dispose();
		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				int argb = out.getRGB(x, y);
				int alpha = argb >>> 24;
				if (alpha == 0)
				{
					continue;
				}
				if (alpha < 128)
				{
					out.setRGB(x, y, 0);
				}
				else
				{
					// Area averaging mixed in the transparent (black) pixels: undo that, then make it opaque
					int r = Math.min(255, ((argb >> 16) & 0xff) * 255 / alpha);
					int gr = Math.min(255, ((argb >> 8) & 0xff) * 255 / alpha);
					int b = Math.min(255, (argb & 0xff) * 255 / alpha);
					out.setRGB(x, y, 0xff000000 | r << 16 | gr << 8 | b);
				}
			}
		}
		return outline(out);
	}

	private static BufferedImage outline(BufferedImage img)
	{
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int argb = img.getRGB(x, y);
				if (argb >>> 24 != 0)
				{
					out.setRGB(x, y, argb);
				}
				else if (opaque(img, x - 1, y) || opaque(img, x + 1, y) || opaque(img, x, y - 1) || opaque(img, x, y + 1))
				{
					out.setRGB(x, y, 0xff000000);
				}
			}
		}
		return out;
	}

	private static boolean opaque(BufferedImage img, int x, int y)
	{
		return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight() && img.getRGB(x, y) >>> 24 != 0;
	}

	@Nullable
	private static BufferedImage load(MasteryTier tier)
	{
		String path = "/com/combatmasteries/tier_" + tier.name().toLowerCase(Locale.ROOT) + ".png";
		try (InputStream in = TierIcons.class.getResourceAsStream(path))
		{
			return in == null ? null : ImageIO.read(in);
		}
		catch (IOException e)
		{
			return null;
		}
	}
}
