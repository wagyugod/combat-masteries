package com.combatmasteries;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(CombatMasteriesConfig.GROUP)
public interface CombatMasteriesConfig extends Config
{
	String GROUP = "combatmasteries";

	@ConfigSection(
		name = "Helmet cosmetic",
		description = "Glowing eyes on the Tzkal slayer helmet for each unlocked tier",
		position = 10
	)
	String cosmeticSection = "cosmetic";

	@ConfigItem(
		keyName = "chatMessages",
		name = "Chat messages",
		description = "Announce completed and failed tasks and tier unlocks in the chatbox, like combat tasks",
		position = 1
	)
	default boolean chatMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = "completionPopup",
		name = "Completion pop-up",
		description = "Show the game's task-completed pop-up when a task or tier is completed",
		position = 2
	)
	default boolean completionPopup()
	{
		return true;
	}

	@ConfigItem(
		keyName = "taskNotification",
		name = "Desktop notification",
		description = "Also send a RuneLite notification, e.g. for when you're tabbed out. Off by default because"
			+ " RuneLite's notification settings may copy it into the chatbox as an extra red line",
		position = 3
	)
	default Notification taskNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "creditPersonalBests",
		name = "Credit existing PBs",
		description = "Complete speed tasks from personal bests you set before installing the plugin."
			+ " Uses PBs from kill messages and from RuneLite's Chat Commands plugin (which reads your POH adventure log)",
		position = 4
	)
	default boolean creditPersonalBests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "helmetGlow",
		name = "Glowing eyes",
		description = "Recolour the Tzkal slayer helmet's eyes for your highest unlocked tier. Only you can see this",
		position = 1,
		section = cosmeticSection
	)
	default boolean helmetGlow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "displayTier",
		name = "Tier to display",
		description = "Show a lower tier's eyes than your highest. Tiers you have not unlocked are never shown",
		position = 2,
		section = cosmeticSection
	)
	default DisplayTier displayTier()
	{
		return DisplayTier.HIGHEST;
	}

	@Range(max = 100)
	@ConfigItem(
		keyName = "haloIntensity",
		name = "Glow halo",
		description = "Strength of the soft glow drawn around the eyes, 0 to turn it off",
		position = 3,
		section = cosmeticSection
	)
	default int haloIntensity()
	{
		return 45;
	}

	enum DisplayTier
	{
		HIGHEST,
		ASCENDED,
		EXALTED,
		IMMORTAL,
	}
}
