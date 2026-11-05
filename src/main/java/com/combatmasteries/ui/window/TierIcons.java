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
 * model through the client's item-sprite code (1px outline, drop shadow) in its 3/4 inventory pose.
 * <ul>
 * <li>Large: the real 36x32 inventory sprite, where the Combat Achievements Rewards list draws an item.</li>
 * <li>Small: 24x24, where the game draws a tier sword (Overview tier boxes, Rewards tier buttons): the large
 * icon fitted to the slot by {@link ItemIcons}, so the two always match.</li>
 * </ul>
 */
public final class TierIcons
{
	public static final int LARGE_WIDTH = 36;
	public static final int LARGE_HEIGHT = 32;
	public static final int SMALL = 24;
	/**
	 * The drop shadow the large icons were rendered with.
	 */
	static final int SHADOW = 0x181818;

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
			SMALL_ICONS.put(tier, large == null ? null : ItemIcons.fit(large, SHADOW, SMALL));
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
