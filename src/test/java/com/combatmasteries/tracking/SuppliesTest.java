package com.combatmasteries.tracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SuppliesTest
{
	@Test
	public void consumables()
	{
		assertTrue(Supplies.isConsumable(new String[]{"Eat", null, null, "Drop"}));
		assertTrue(Supplies.isConsumable(new String[]{"Drink", "Empty"}));
		assertFalse(Supplies.isConsumable(new String[]{"Wield", "Drop"}));
		assertFalse(Supplies.isConsumable(null));
	}

	@Test
	public void potionsCountByDose()
	{
		assertEquals(4, Supplies.units("Saradomin brew(4)", 1));
		assertEquals(6, Supplies.units("Super restore(3)", 2));
		assertEquals(3, Supplies.units("Cooked karambwan", 3));
		// Combining two 2-dose potions into a 4-dose and an empty vial changes nothing
		assertEquals(Supplies.units("Prayer potion(2)", 2), Supplies.units("Prayer potion(4)", 1));
	}
}
