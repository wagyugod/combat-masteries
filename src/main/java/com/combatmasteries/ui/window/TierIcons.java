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
			BufferedImage small = null;
			if (large != null)
			{
				small = new BufferedImage(SMALL, SMALL, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = small.createGraphics();
				g.drawImage(large.getScaledInstance(SMALL, SMALL, Image.SCALE_AREA_AVERAGING), 0, 0, null);
				g.dispose();
			}
			SMALL_ICONS.put(tier, small);
		}
		return SMALL_ICONS.get(tier);
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
