package com.combatmasteries.ui.window;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * {@link SpriteSource} over RuneLite's {@link SpriteManager} and {@link ItemManager}, which cache what they
 * load. Must be used on the client thread, where overlays are drawn.
 * <p>
 * An item sprite is asked of the item manager once. It hands out an image that it fills in later on the
 * client thread; until then {@link #item} returns null and the renderer draws its fallback, and from the
 * frame after it has loaded the window draws the sprite (the overlay is drawn every frame).
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
	@Nullable
	private final ItemManager itemManager;
	/**
	 * Item sprites asked for, and the ones that have loaded. Loading finishes on the client thread, but
	 * the maps are concurrent so nothing depends on it.
	 */
	private final Map<Integer, Boolean> itemsAsked = new ConcurrentHashMap<>();
	private final Map<Integer, BufferedImage> itemsLoaded = new ConcurrentHashMap<>();
	/**
	 * When each sprite that failed to load was last asked for. Only touched on the client thread.
	 */
	private final Map<Long, Long> failed = new HashMap<>();

	public SpriteManagerSource(SpriteManager spriteManager, @Nullable ItemManager itemManager)
	{
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
	}

	@Nullable
	@Override
	public BufferedImage item(int itemId)
	{
		BufferedImage img = itemsLoaded.get(itemId);
		if (img != null || itemManager == null || itemsAsked.putIfAbsent(itemId, Boolean.TRUE) != null)
		{
			return img;
		}
		AsyncBufferedImage async = itemManager.getImage(itemId);
		if (async != null)
		{
			async.onLoaded(() -> itemsLoaded.put(itemId, async));
		}
		return null;
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
