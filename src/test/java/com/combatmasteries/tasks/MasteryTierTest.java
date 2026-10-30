package com.combatmasteries.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class MasteryTierTest
{
	@Test
	public void thresholdsRoundUp()
	{
		assertEquals(3, MasteryTier.ASCENDED.threshold(10));
		assertEquals(5, MasteryTier.EXALTED.threshold(10));
		assertEquals(10, MasteryTier.IMMORTAL.threshold(10));

		assertEquals(25, MasteryTier.ASCENDED.threshold(100));
		assertEquals(50, MasteryTier.EXALTED.threshold(100));
		assertEquals(100, MasteryTier.IMMORTAL.threshold(100));
	}

	@Test
	public void tierForPoints()
	{
		assertNull(MasteryTier.forPoints(2, 10));
		assertEquals(MasteryTier.ASCENDED, MasteryTier.forPoints(3, 10));
		assertEquals(MasteryTier.ASCENDED, MasteryTier.forPoints(4, 10));
		assertEquals(MasteryTier.EXALTED, MasteryTier.forPoints(9, 10));
		assertEquals(MasteryTier.IMMORTAL, MasteryTier.forPoints(10, 10));
		assertNull(MasteryTier.forPoints(0, 0));
	}

	@Test
	public void nextTier()
	{
		assertEquals(MasteryTier.ASCENDED, MasteryTier.next(null));
		assertEquals(MasteryTier.EXALTED, MasteryTier.next(MasteryTier.ASCENDED));
		assertNull(MasteryTier.next(MasteryTier.IMMORTAL));
	}
}
