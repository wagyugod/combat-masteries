package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import javax.annotation.Nullable;

/**
 * Where the window renderer gets the game's interface sprites from. In the client this is RuneLite's
 * {@code SpriteManager}; offline it is read straight from a cache dump.
 * <p>
 * Images are trimmed to their pixels, without the sprite's offsets, which is what
 * {@code SpriteManager} hands out. {@link WidgetPainter} knows the offsets of the few sprites that have them.
 */
public interface SpriteSource
{
	/**
	 * @return the sprite, or null if it is not loaded (yet); the renderer then leaves that piece out
	 */
	@Nullable
	BufferedImage sprite(int id, int frame);
}
