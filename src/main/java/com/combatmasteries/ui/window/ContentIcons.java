package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * The 24x24 content icons drawn in place of the Combat Achievements tier swords (task rows, rewards).
 * <p>
 * Each icon is the content's signature item rendered offline from its cache model through the client's item-sprite
 * code (1px 0x000001 outline, 0x181818 drop shadow) at 24x24, the size of the CA tier sword sprites (3399-3404): long
 * items laid along the bottom-left to top-right diagonal like the swords, the cape and helms in their inventory pose
 * and the boots side on. The items: Infernal cape, Blessed Dizana's quiver, Scythe of Vitur, Sanguine Scythe of Vitur, Twisted
 * bow, Tumeken's shadow, Avernic treads (max), Sanguine torva full helm, Radiant oathplate helm. One file per content
 * under {@code /com/combatmasteries/icons/}, named after the content with every run of non-alphanumerics turned into an underscore
 * ("Chambers of Xeric: CM" is {@code chambers_of_xeric_cm.png}).
 */
public final class ContentIcons
{
	public static final int SIZE = 24;

	private static final String DIRECTORY = "/com/combatmasteries/icons/";
	private static final Map<String, BufferedImage> CACHE = new HashMap<>();

	private ContentIcons()
	{
	}

	/**
	 * @return the icon for a task's content (e.g. "Theatre of Blood"), or null when there is none
	 */
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
