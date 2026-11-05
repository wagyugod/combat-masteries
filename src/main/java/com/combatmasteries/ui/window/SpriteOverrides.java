package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

/**
 * Wraps a {@link SpriteSource} and hands out the plugin's own copies of a few interface sprites instead:
 * the steel window frame, its dividers and buttons, and the stone box bevels. The current game draws
 * these in slightly different shades than the sprites in the cache the renderer was built against, so
 * the copies (same size and shape, under {@code /com/combatmasteries/sprites}) carry the shades the
 * game shows in the Combat Achievements window.
 */
final class SpriteOverrides implements SpriteSource
{
	private static final int[] IDS = {
		WindowChrome.EDGE_LEFT, WindowChrome.EDGE_BOTTOM, WindowChrome.EDGE_TOP, WindowChrome.EDGE_RIGHT,
		WindowChrome.CORNER_TOP_LEFT, WindowChrome.CORNER_TOP_RIGHT, WindowChrome.CORNER_BOTTOM_LEFT,
		WindowChrome.CORNER_BOTTOM_RIGHT, WindowChrome.DIVIDER_HORIZONTAL, WindowChrome.DIVIDER_VERTICAL,
		WindowChrome.CLOSE, WindowChrome.MENU,
		WindowChrome.BOX_FIRST, WindowChrome.BOX_FIRST + 1, WindowChrome.BOX_FIRST + 2, WindowChrome.BOX_FIRST + 3,
		WindowChrome.BOX_FIRST + 4, WindowChrome.BOX_FIRST + 5, WindowChrome.BOX_FIRST + 6, WindowChrome.BOX_FIRST + 7,
	};

	private static final Map<Integer, BufferedImage> IMAGES = load();

	private final SpriteSource sprites;

	SpriteOverrides(SpriteSource sprites)
	{
		this.sprites = sprites;
	}

	@Nullable
	@Override
	public BufferedImage sprite(int id, int frame)
	{
		BufferedImage img = frame == 0 ? IMAGES.get(id) : null;
		return img != null ? img : sprites.sprite(id, frame);
	}

	private static Map<Integer, BufferedImage> load()
	{
		Map<Integer, BufferedImage> images = new HashMap<>();
		for (int id : IDS)
		{
			try (InputStream in = SpriteOverrides.class.getResourceAsStream("/com/combatmasteries/sprites/" + id + ".png"))
			{
				if (in != null)
				{
					images.put(id, ImageIO.read(in));
				}
			}
			catch (IOException e)
			{
				// Fall back to the game's sprite
			}
		}
		return Collections.unmodifiableMap(images);
	}
}
