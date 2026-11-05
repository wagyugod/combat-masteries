package com.combatmasteries.ui;

import com.combatmasteries.ui.window.WindowView;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FontTypeFace;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/**
 * A button on the real Combat Achievements window's title bar, just right of the title, that opens the
 * Combat Masteries window on the same tab, the way plugins add buttons to the collection log. It is a
 * client-side widget: clicking it runs plugin code and sends nothing to the server.
 * <p>
 * The game rebuilds the title bar whenever a tab is drawn, so {@link #update} puts the button back if it
 * has gone. All methods must be called on the client thread.
 */
@Singleton
public class CombatAchievementsButton
{
	/**
	 * Interface groups of the Combat Achievements tabs.
	 */
	public static final int[] GROUPS = {InterfaceID.CA_OVERVIEW, InterfaceID.CA_TASKS, InterfaceID.CA_BOSSES, InterfaceID.CA_REWARDS};
	/**
	 * Each tab's frame, which holds the title (script 228), and the Combat Masteries tab to open from it.
	 */
	private static final int[] FRAMES = {InterfaceID.CaOverview.FRAME, InterfaceID.CaTasks.FRAME, InterfaceID.CaBosses.FRAME, InterfaceID.CaRewards.FRAME};
	private static final WindowView[] VIEWS = {WindowView.OVERVIEW, WindowView.TASKS, WindowView.BOSSES, WindowView.REWARDS};
	/**
	 * The scripts that build each tab and its title bar: 4804 Overview, 4816 Tasks, 4791 Bosses and 4811
	 * Rewards. After any of them runs the button may need adding again.
	 */
	public static final int[] BUILD_SCRIPTS = {4804, 4816, 4791, 4811};

	private static final String TITLE_PREFIX = "Combat Achievements";
	private static final int SIZE = 16;
	private static final int GAP = 6;
	/**
	 * Slightly faded until hovered, like the game's title bar buttons light up.
	 */
	private static final int IDLE_OPACITY = 60;
	private static final String NAME = "<col=ff9040>Combat Masteries</col>";

	private final Client client;
	@Nullable
	private Consumer<WindowView> onOpen;

	@Inject
	CombatAchievementsButton(Client client)
	{
		this.client = client;
	}

	/**
	 * Adds the button to whichever Combat Achievements tab is open, if it is not there already.
	 *
	 * @param onOpen called with the tab to open when the button is clicked
	 */
	public void update(Consumer<WindowView> onOpen)
	{
		this.onOpen = onOpen;
		for (int i = 0; i < FRAMES.length; i++)
		{
			Widget frame = client.getWidget(FRAMES[i]);
			if (frame != null && !frame.isHidden() && find(frame) == null)
			{
				add(frame, VIEWS[i]);
			}
		}
	}

	/**
	 * Hides the button wherever it was added. The game rebuilds the title bar the next time a tab opens.
	 */
	public void remove()
	{
		onOpen = null;
		for (int frameId : FRAMES)
		{
			Widget frame = client.getWidget(frameId);
			Widget button = frame == null ? null : find(frame);
			if (button != null)
			{
				button.setHidden(true);
				button.setHasListener(false);
				button.setOnOpListener((Object[]) null);
			}
		}
	}

	@Nullable
	private static Widget find(Widget frame)
	{
		Widget[] children = frame.getDynamicChildren();
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			if (child != null && !child.isHidden() && child.getSpriteId() == MasteriesSprite.WINDOW_BUTTON.getSpriteId())
			{
				return child;
			}
		}
		return null;
	}

	private void add(Widget frame, WindowView view)
	{
		Widget title = title(frame);
		int x;
		int y;
		if (title != null)
		{
			FontTypeFace font = title.getFont();
			int textWidth = font != null ? font.getTextWidth(title.getText()) : title.getWidth() / 2;
			// The title is centred in its box: start just past its right end
			x = title.getRelativeX() + (title.getWidth() + textWidth) / 2 + GAP;
			y = title.getRelativeY() + (title.getHeight() - SIZE) / 2;
		}
		else
		{
			x = frame.getWidth() / 2 + 110;
			y = 10;
		}

		Widget button = frame.createChild(-1, WidgetType.GRAPHIC);
		button.setSpriteId(MasteriesSprite.WINDOW_BUTTON.getSpriteId());
		button.setOriginalX(x);
		button.setOriginalY(y);
		button.setOriginalWidth(SIZE);
		button.setOriginalHeight(SIZE);
		button.setOpacity(IDLE_OPACITY);
		button.setName(NAME);
		button.setAction(0, "Open");
		button.setHasListener(true);
		button.setNoClickThrough(true);
		button.setOnMouseOverListener((JavaScriptCallback) ev -> button.setOpacity(0));
		button.setOnMouseLeaveListener((JavaScriptCallback) ev -> button.setOpacity(IDLE_OPACITY));
		button.setOnOpListener((JavaScriptCallback) ev ->
		{
			Consumer<WindowView> open = onOpen;
			if (open != null)
			{
				open.accept(view);
			}
		});
		button.revalidate();
	}

	/**
	 * @return the title text the game put on the frame, or null
	 */
	@Nullable
	private static Widget title(Widget frame)
	{
		Widget[] children = frame.getDynamicChildren();
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			if (child != null && child.getType() == WidgetType.TEXT && child.getText() != null
				&& child.getText().startsWith(TITLE_PREFIX))
			{
				return child;
			}
		}
		return null;
	}
}
