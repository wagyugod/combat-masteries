package com.combatmasteries.ui.window;

/**
 * Scrollbar geometry as the game's scrollbar script (31) lays it out: an arrow at each end, 16px square,
 * and a thumb on the track between them sized to the share of the list in view, at least 8px.
 */
public final class ScrollMath
{
	public static final int ARROW = 16;
	public static final int MIN_THUMB = 8;

	private ScrollMath()
	{
	}

	/**
	 * @return the furthest a list of {@code content} pixels can scroll in a viewport of {@code viewport}
	 */
	public static int max(int content, int viewport)
	{
		return Math.max(0, content - viewport);
	}

	public static int clamp(int scroll, int max)
	{
		return Math.max(0, Math.min(scroll, max));
	}

	/**
	 * @param height the scrollbar's height, arrows included, which is also the viewport's
	 */
	public static int track(int height)
	{
		return Math.max(0, height - 2 * ARROW);
	}

	public static int thumbHeight(int height, int content)
	{
		int track = track(height);
		int shown = Math.max(content, height);
		return Math.min(track, Math.max(MIN_THUMB, track * height / Math.max(1, shown)));
	}

	/**
	 * @return the thumb's top, from the top of the track
	 */
	public static int thumbTop(int height, int content, int scroll)
	{
		int max = max(content, height);
		int travel = track(height) - thumbHeight(height, content);
		return max <= 0 || travel <= 0 ? 0 : travel * clamp(scroll, max) / max;
	}

	/**
	 * @param thumbTop where the thumb's top has been dragged to, from the top of the track
	 * @return the scroll offset that puts the thumb there
	 */
	public static int scrollForThumb(int height, int content, int thumbTop)
	{
		int max = max(content, height);
		int travel = track(height) - thumbHeight(height, content);
		if (max <= 0 || travel <= 0)
		{
			return 0;
		}
		// Rounded up, so the thumb is drawn exactly where it was dragged
		long top = clamp(thumbTop, travel);
		return clamp((int) ((top * max + travel - 1) / travel), max);
	}
}
