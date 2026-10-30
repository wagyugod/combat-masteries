package com.combatmasteries.tasks;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tells the attempt tracker when a fight with a boss begins.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BossDefinition
{
	/**
	 * The boss as named in its kill count message.
	 */
	private String name;
	/**
	 * NPC names that start an attempt when the player attacks them or they attack the
	 * player.
	 */
	private List<String> npcs = new ArrayList<>();
	/**
	 * Region IDs where the whole stay counts as one attempt, for wave content such as the
	 * Inferno. Takes priority over {@link #npcs}.
	 */
	private List<Integer> regions = new ArrayList<>();
}
