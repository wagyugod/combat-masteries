package com.combatmasteries.ui.window;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ScrollMathTest
{
	@Test
	public void maxIsWhatDoesNotFit()
	{
		assertEquals(0, ScrollMath.max(100, 200));
		assertEquals(0, ScrollMath.max(200, 200));
		assertEquals(150, ScrollMath.max(350, 200));
	}

	@Test
	public void clampsToRange()
	{
		assertEquals(0, ScrollMath.clamp(-10, 50));
		assertEquals(50, ScrollMath.clamp(80, 50));
		assertEquals(20, ScrollMath.clamp(20, 50));
		assertEquals(0, ScrollMath.clamp(20, 0));
	}

	@Test
	public void thumbFillsTheTrackWhenEverythingFits()
	{
		// 232 tall bar: 200px track between the arrows
		assertEquals(200, ScrollMath.track(232));
		assertEquals(200, ScrollMath.thumbHeight(232, 100));
		assertEquals(0, ScrollMath.thumbTop(232, 100, 0));
	}

	@Test
	public void thumbIsTheShareInView()
	{
		// Half the list in view: half the track
		assertEquals(100, ScrollMath.thumbHeight(232, 464));
		// Never smaller than 8px
		assertEquals(ScrollMath.MIN_THUMB, ScrollMath.thumbHeight(232, 100_000));
	}

	@Test
	public void thumbTravelsWithTheScroll()
	{
		int height = 232;
		int content = 464;
		int max = ScrollMath.max(content, height);
		assertEquals(0, ScrollMath.thumbTop(height, content, 0));
		assertEquals(50, ScrollMath.thumbTop(height, content, max / 2));
		assertEquals(100, ScrollMath.thumbTop(height, content, max));
		// Out of range offsets are drawn at the ends
		assertEquals(100, ScrollMath.thumbTop(height, content, max * 2));
	}

	@Test
	public void draggingTheThumbScrollsBack()
	{
		int height = 232;
		int content = 1000;
		int max = ScrollMath.max(content, height);
		for (int scroll = 0; scroll <= max; scroll += 37)
		{
			int top = ScrollMath.thumbTop(height, content, scroll);
			int back = ScrollMath.scrollForThumb(height, content, top);
			assertEquals(ScrollMath.thumbTop(height, content, back), top);
		}
		assertEquals(0, ScrollMath.scrollForThumb(height, content, -50));
		assertEquals(max, ScrollMath.scrollForThumb(height, content, 10_000));
		assertEquals(0, ScrollMath.scrollForThumb(height, 100, 30));
	}
}
