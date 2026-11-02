package com.combatmasteries.tasks;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-boss settings that tasks need beyond the kill message.
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
	 * Region IDs where the whole stay counts as one attempt, for wave content such as the
	 * Inferno.
	 */
	private List<Integer> regions = new ArrayList<>();
	/**
	 * Time tasks use the raid's total completion time (including downtime) instead of the
	 * challenge time. True for Tombs of Amascut.
	 */
	private boolean totalTime;
}
