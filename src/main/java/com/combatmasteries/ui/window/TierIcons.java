package com.combatmasteries.ui.window;

import com.combatmasteries.tasks.MasteryTier;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

/**
 * The tier icons: the Tzkal slayer helmet with each tier's eye colour, rendered offline from the item's cache
 * model through the client's item-sprite code (1px outline, drop shadow) at each size they are shown at, so
 * nothing is resampled at runtime.
 * <ul>
 * <li>Large: the real 36x32 inventory sprite, where the Combat Achievements Rewards list draws an item.</li>
 * <li>Small: 24x24, front on so the eye slit shows, where the game draws a tier sword (Overview tier boxes,
 * Rewards tier buttons).</li>
 * </ul>
 */
public final class TierIcons
{
	public static final int LARGE_WIDTH = 36;
	public static final int LARGE_HEIGHT = 32;
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
			LARGE_ICONS.put(tier, load(tier, ""));
		}
		return LARGE_ICONS.get(tier);
	}

	@Nullable
	public static synchronized BufferedImage small(MasteryTier tier)
	{
		if (!SMALL_ICONS.containsKey(tier))
		{
			SMALL_ICONS.put(tier, load(tier, "_small"));
		}
		return SMALL_ICONS.get(tier);
	}

	@Nullable
	private static BufferedImage load(MasteryTier tier, String suffix)
	{
		String path = "/com/combatmasteries/tier_" + tier.name().toLowerCase(Locale.ROOT) + suffix + ".png";
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
