package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.runelite.client.game.SpriteManager;

/**
 * {@link SpriteSource} over RuneLite's {@link SpriteManager}. Sprites are requested the first time they
 * are asked for and handed out once loaded, so the overlay can draw from any thread; until then the
 * renderer skips the piece.
 */
public class SpriteManagerSource implements SpriteSource
{
	private final SpriteManager spriteManager;
	private final Map<Long, BufferedImage> loaded = new ConcurrentHashMap<>();
	private final Set<Long> requested = ConcurrentHashMap.newKeySet();

	public SpriteManagerSource(SpriteManager spriteManager)
	{
		this.spriteManager = spriteManager;
	}

	@Nullable
	@Override
	public BufferedImage sprite(int id, int frame)
	{
		long key = ((long) id << 32) | frame;
		BufferedImage img = loaded.get(key);
		if (img == null && requested.add(key))
		{
			spriteManager.getSpriteAsync(id, frame, image ->
			{
				if (image != null)
				{
					loaded.put(key, image);
				}
				else
				{
					// Let a later frame ask again
					requested.remove(key);
				}
			});
		}
		return img;
	}
}
