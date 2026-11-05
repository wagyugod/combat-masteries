package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.runelite.api.gameval.ItemID;

/**
 * The 24x24 content icons drawn in place of the Combat Achievements tier swords (task rows), 24x24 like the
 * CA tier sword sprites (3399-3404).
 * <p>
 * In game each icon is the content's signature item's own inventory sprite, from {@link SpriteSource#item},
 * fitted to the slot by {@link ItemIcons}. Until that sprite has loaded, and in the offline preview without
 * a cache, the plugin's own copy is drawn instead: one file per content under
 * {@code /com/combatmasteries/icons/}, named after the content with every run of non-alphanumerics turned into
 * an underscore ("Chambers of Xeric: CM" is {@code chambers_of_xeric_cm.png}).
 */
public final class ContentIcons
{
	public static final int SIZE = 24;

	private static final String DIRECTORY = "/com/combatmasteries/icons/";
	private static final Map<String, BufferedImage> CACHE = new HashMap<>();
	/**
	 * Each content's signature item. There is no blood torva helm item: the Awakened Bosses wear the red
	 * Sanguine torva full helm.
	 */
	private static final Map<String, Integer> ITEMS = Map.of(
		"Theatre of Blood", ItemID.SCYTHE_OF_VITUR,
		"Theatre of Blood: Hard Mode", ItemID.SCYTHE_OF_VITUR_BL,
		"Chambers of Xeric: CM", ItemID.TWISTED_BOW,
		"Tombs of Amascut", ItemID.TUMEKENS_SHADOW,
		"Colosseum", ItemID.DIZANAS_QUIVER_INFINITE,
		"Inferno", ItemID.INFERNAL_CAPE,
		"Doom of Mokhaiotl", ItemID.AVERNIC_TREADS_MAX,
		"Yama Contracts", ItemID.RADIANT_OATHPLATE_HELM,
		"Awakened Bosses", ItemID.TORVA_HELM_SANGUINE
	);

	private ContentIcons()
	{
	}

	/**
	 * @return the icon to draw for a task's content: its item's sprite once loaded, else the plugin's own
	 * icon; null when there is none
	 */
	@Nullable
	static BufferedImage icon(WidgetPainter p, String content)
	{
		int itemId = itemId(content);
		BufferedImage sprite = itemId < 0 ? null : p.item(itemId);
		return sprite != null ? ItemIcons.fitItem(sprite, SIZE) : get(content);
	}

	/**
	 * @return the item whose sprite stands for a task's content, or -1 when there is none
	 */
	public static int itemId(String content)
	{
		Integer itemId = content == null ? null : ITEMS.get(content);
		return itemId == null ? -1 : itemId;
	}

	/**
	 * @return the plugin's own icon for a task's content (e.g. "Theatre of Blood"), or null when there is none
	 */
	@Nullable
	public static synchronized BufferedImage get(String content)
	{
		if (content == null)
		{
			return null;
		}
		if (CACHE.containsKey(content))
		{
			return CACHE.get(content);
		}
		BufferedImage icon = null;
		try (InputStream in = ContentIcons.class.getResourceAsStream(DIRECTORY + fileName(content)))
		{
			if (in != null)
			{
				icon = ImageIO.read(in);
			}
		}
		catch (IOException e)
		{
			icon = null;
		}
		CACHE.put(content, icon);
		return icon;
	}

	static String fileName(String content)
	{
		if ("Awakened Bosses".equals(content))
		{
			// Named "Awakened DT2" when the icon was made
			return "awakened_dt2.png";
		}
		String slug = content.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		return slug + ".png";
	}
}
