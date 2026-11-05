package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.runelite.client.game.SpriteManager;

/**
 * {@link SpriteSource} over RuneLite's {@link SpriteManager}, which caches what it loads. Must be used on the
 * client thread, where overlays are drawn.
 * <p>
 * A sprite that cannot be loaded (the cache is not ready, or the id or frame does not exist) is skipped by the
 * renderer and asked for again at most once every {@link #RETRY_MILLIS}, so a missing sprite never costs a
 * cache lookup every frame. ({@link SpriteManager#getSpriteAsync} is not used: it retries a missing sprite on
 * every client cycle, forever, even after the plugin stops.)
 */
public class SpriteManagerSource implements SpriteSource
{
	static final long RETRY_MILLIS = 1000;

	private final SpriteManager spriteManager;
	/**
	 * When each sprite that failed to load was last asked for. Only touched on the client thread.
	 */
	private final Map<Long, Long> failed = new HashMap<>();

	public SpriteManagerSource(SpriteManager spriteManager)
	{
		this.spriteManager = spriteManager;
	}

	@Nullable
	@Override
	public BufferedImage sprite(int id, int frame)
	{
		long key = ((long) id << 32) | (frame & 0xffffffffL);
		long now = System.currentTimeMillis();
		Long last = failed.get(key);
		if (last != null && now - last < RETRY_MILLIS)
		{
			return null;
		}

		BufferedImage img;
		try
		{
			img = spriteManager.getSprite(id, frame);
		}
		catch (IndexOutOfBoundsException e)
		{
			// The sprite has fewer frames than asked for
			img = null;
		}
		if (img == null)
		{
			failed.put(key, now);
		}
		else if (last != null)
		{
			failed.remove(key);
		}
		return img;
	}
}
