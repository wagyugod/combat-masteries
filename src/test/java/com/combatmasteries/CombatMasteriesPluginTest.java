package com.combatmasteries;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CombatMasteriesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CombatMasteriesPlugin.class);
		RuneLite.main(args);
	}
}
