package com.combatmasteries.ui.window;

import java.awt.Rectangle;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * A clickable part of the window, recorded while it is drawn so that clicks hit exactly what is on screen.
 */
@Value
public class Hotspot
{
	public enum Kind
	{
		/**
		 * The close button in the top right.
		 */
		CLOSE,
		/**
		 * The menu button in the top left.
		 */
		MENU,
		/**
		 * A tab in the open menu; the value is its {@link WindowView}.
		 */
		MENU_ITEM,
		/**
		 * Everywhere else while the menu is open: a click there closes it, as the game's menu overlay does.
		 */
		MENU_DISMISS,
		/**
		 * A Tasks filter's value box; the value is its {@link TaskFilter}.
		 */
		FILTER,
		/**
		 * An option in the open filter list; the value is the option's text.
		 */
		FILTER_OPTION,
		/**
		 * Everywhere else while a filter list is open: a click there closes it.
		 */
		FILTER_DISMISS,
		/**
		 * A task row; the value is the task id.
		 */
		TASK,
		/**
		 * A Rewards tier button; the value is its {@link com.combatmasteries.tasks.MasteryTier}.
		 */
		TIER,
		/**
		 * An Overview tier box; the value is its {@link com.combatmasteries.tasks.MasteryTier}. Like the
		 * Combat Achievements tier boxes, a click opens the Tasks tab.
		 */
		TIER_BOX,
		/**
		 * A scrollbar; the value is its {@link WindowLayout.Scroll}.
		 */
		SCROLLBAR,
		/**
		 * The Overview's screenshot button, left of the close button.
		 */
		SCREENSHOT,
		/**
		 * The Bosses search box: a click there sends typing to the search instead of the chatbox.
		 */
		SEARCH,
	}

	Kind kind;
	@Nullable
	Object value;
	Rectangle bounds;
}
