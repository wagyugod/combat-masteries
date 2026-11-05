package com.combatmasteries.ui.window;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Where the clickable parts and scrolling lists of the window ended up on the last frame, in window
 * coordinates. The renderer fills it in as it draws; the controller hit-tests clicks against it.
 */
public final class WindowLayout
{
	/**
	 * A scrolling list.
	 */
	@Value
	public static class Scroll
	{
		ScrollTarget target;
		/**
		 * The scrollbar, arrows included, or null for a list without one (the Overview's personal bests).
		 */
		@Nullable
		Rectangle bar;
		/**
		 * Height of the list's visible area.
		 */
		int viewport;
		/**
		 * Height of the whole list.
		 */
		int content;
		/**
		 * How far one notch of the mouse wheel moves it.
		 */
		int step;

		public int max()
		{
			return ScrollMath.max(content, viewport);
		}
	}

	private final List<Hotspot> hotspots = new ArrayList<>();
	private final Map<ScrollTarget, Scroll> scrolls = new EnumMap<>(ScrollTarget.class);

	void add(Hotspot.Kind kind, @Nullable Object value, Rectangle bounds)
	{
		if (!bounds.isEmpty())
		{
			hotspots.add(new Hotspot(kind, value, bounds));
		}
	}

	void scroll(Scroll scroll)
	{
		scrolls.put(scroll.getTarget(), scroll);
	}

	/**
	 * @return the hotspot drawn last (so on top) under the point, or null
	 */
	@Nullable
	public Hotspot hit(int x, int y)
	{
		for (int i = hotspots.size() - 1; i >= 0; i--)
		{
			Hotspot hotspot = hotspots.get(i);
			if (hotspot.getBounds().contains(x, y))
			{
				return hotspot;
			}
		}
		return null;
	}

	/**
	 * @return the first hotspot of the kind and value, or null
	 */
	@Nullable
	public Hotspot find(Hotspot.Kind kind, @Nullable Object value)
	{
		for (Hotspot hotspot : hotspots)
		{
			if (hotspot.getKind() == kind && (value == null || value.equals(hotspot.getValue())))
			{
				return hotspot;
			}
		}
		return null;
	}

	@Nullable
	public Scroll scroll(ScrollTarget target)
	{
		return scrolls.get(target);
	}

	public List<Hotspot> hotspots()
	{
		return Collections.unmodifiableList(hotspots);
	}
}
