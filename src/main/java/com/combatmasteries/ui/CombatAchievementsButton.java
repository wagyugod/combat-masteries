package com.combatmasteries.ui;

import com.combatmasteries.ui.window.WindowView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;

/**
 * A "Masteries" button on the real Combat Achievements window's title bar that opens the Combat Masteries
 * window on the same tab, built like the buttons WikiSync and Temple add to the collection log's title bar:
 * a small stone button with a metal rim and a text label, which lights up while hovered. It sits at the
 * right of the title bar, just left of the close button, where the collection log's Search button sits
 * mirrored on the left. It is made of client-side widgets: clicking it runs plugin code and sends nothing
 * to the server.
 * <p>
 * The game rebuilds the title bar whenever a tab is drawn, deleting the button, so {@link #update} puts it
 * back if it has gone. All methods must be called on the client thread.
 */
@Singleton
public class CombatAchievementsButton
{
	/**
	 * Interface groups of the Combat Achievements tabs.
	 */
	public static final int[] GROUPS = {InterfaceID.CA_OVERVIEW, InterfaceID.CA_TASKS, InterfaceID.CA_BOSSES, InterfaceID.CA_REWARDS};
	/**
	 * Each tab's frame, which holds the title bar (script 228), and the Combat Masteries tab to open from it.
	 */
	private static final int[] FRAMES = {InterfaceID.CaOverview.FRAME, InterfaceID.CaTasks.FRAME, InterfaceID.CaBosses.FRAME, InterfaceID.CaRewards.FRAME};
	private static final WindowView[] VIEWS = {WindowView.OVERVIEW, WindowView.TASKS, WindowView.BOSSES, WindowView.REWARDS};
	/**
	 * The scripts that build each tab and its title bar: 4804 Overview, 4816 Tasks, 4791 Bosses and 4811
	 * Rewards. After any of them runs the button may need adding again.
	 */
	public static final int[] BUILD_SCRIPTS = {4804, 4816, 4791, 4811};

	private static final String LABEL = "Masteries";
	/**
	 * Every widget of the button carries this name, which is how {@link #find} tells them from the title
	 * bar's own; on the label it is also the target of its menu option ("Open Combat Masteries").
	 */
	private static final String NAME = "<col=ff9040>Combat Masteries</col>";

	/**
	 * The collection log's Search button is 71x22, 7 from the top (script 4083), and WikiSync's button
	 * matches it. The Combat Achievements title (bold, centred) ends at x 350 at most, the close button
	 * (26 wide, 3 from the right edge) starts at x 451 of the 480 wide frame; the button takes x 376-446,
	 * leaving 4 before the close button as WikiSync does in the collection log.
	 */
	private static final int WIDTH = 71;
	private static final int HEIGHT = 22;
	private static final int TOP = 7;
	/**
	 * From the frame's right edge to the button's right edge.
	 */
	private static final int RIGHT = 33;
	/**
	 * The rim sprites are 9x9: corners, and edges whose 3px of rim sit on the button's side of the square.
	 */
	private static final int RIM = 9;

	/**
	 * Background, then corners top left, top right, bottom left, bottom right, then edges left, top, right,
	 * bottom: the stone and metal rim at rest, and the darker stone and lit rim while hovered.
	 */
	private static final int[] SPRITES = {
		SpriteID.TRADEBACKING,
		SpriteID.V2StoneButtonOut.A_TOP_LEFT, SpriteID.V2StoneButtonOut.A_TOP_RIGHT,
		SpriteID.V2StoneButtonOut.A_BOTTOM_LEFT, SpriteID.V2StoneButtonOut.A_BOTTOM_RIGHT,
		SpriteID.V2StoneButtonOut.A_MAP_EDGE_LEFT, SpriteID.V2StoneButtonOut.A_MAP_EDGE_TOP,
		SpriteID.V2StoneButtonOut.A_MAP_EDGE_RIGHT, SpriteID.V2StoneButtonOut.A_MAP_EDGE_BOTTOM,
	};
	private static final int[] SPRITES_HOVERED = {
		SpriteID.TRADEBACKING_DARK,
		SpriteID.V2StoneButtonIn.A_TOP_LEFT, SpriteID.V2StoneButtonIn.A_TOP_RIGHT,
		SpriteID.V2StoneButtonIn.A_BOTTOM_LEFT, SpriteID.V2StoneButtonIn.A_BOTTOM_RIGHT,
		SpriteID.V2StoneButtonIn.A_LEFT, SpriteID.V2StoneButtonIn.A_TOP,
		SpriteID.V2StoneButtonIn.A_RIGHT, SpriteID.V2StoneButtonIn.A_BOTTOM,
	};
	/**
	 * Where each sprite goes in the button: x, y, width, height.
	 */
	private static final int[][] SLICES = {
		// The stone stays inside the rim's rounded corners, as in the game's own title bar buttons (script 134)
		{1, 1, WIDTH - 2, HEIGHT - 2},
		{0, 0, RIM, RIM},
		{WIDTH - RIM, 0, RIM, RIM},
		{0, HEIGHT - RIM, RIM, RIM},
		{WIDTH - RIM, HEIGHT - RIM, RIM, RIM},
		{0, RIM, RIM, HEIGHT - 2 * RIM},
		{RIM, 0, WIDTH - 2 * RIM, RIM},
		{WIDTH - RIM, RIM, RIM, HEIGHT - 2 * RIM},
		{RIM, HEIGHT - RIM, WIDTH - 2 * RIM, RIM},
	};
	private static final int TEXT_COLOUR = 0xd6d6d6;
	private static final int TEXT_COLOUR_HOVERED = 0xffffff;

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
			if (frame != null && !frame.isHidden() && find(frame).isEmpty())
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
			if (frame == null)
			{
				continue;
			}
			for (Widget part : find(frame))
			{
				part.setHidden(true);
				part.setHasListener(false);
				part.setOnOpListener((Object[]) null);
				part.setOnMouseOverListener((Object[]) null);
				part.setOnMouseLeaveListener((Object[]) null);
			}
		}
	}

	/**
	 * @return the widgets of the button on the frame that are not hidden, empty if it has none
	 */
	private static List<Widget> find(Widget frame)
	{
		List<Widget> parts = new ArrayList<>();
		Widget[] children = frame.getDynamicChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (child != null && !child.isSelfHidden() && NAME.equals(child.getName()))
				{
					parts.add(child);
				}
			}
		}
		return parts;
	}

	private void add(Widget frame, WindowView view)
	{
		Widget[] slices = new Widget[SLICES.length];
		for (int i = 0; i < SLICES.length; i++)
		{
			int[] s = SLICES[i];
			slices[i] = part(frame, WidgetType.GRAPHIC, s[0], s[1], s[2], s[3]);
			slices[i].setSpriteId(SPRITES[i]);
			slices[i].setSpriteTiling(true);
			slices[i].revalidate();
		}

		// The label covers the whole button and takes the mouse for it
		Widget label = part(frame, WidgetType.TEXT, 0, 0, WIDTH, HEIGHT);
		label.setText(LABEL);
		label.setFontId(FontID.PLAIN_11);
		label.setTextShadowed(true);
		label.setTextColor(TEXT_COLOUR);
		label.setXTextAlignment(WidgetTextAlignment.CENTER);
		label.setYTextAlignment(WidgetTextAlignment.CENTER);
		label.setAction(0, "Open");
		label.setHasListener(true);
		label.setNoClickThrough(true);
		label.setOnMouseOverListener((JavaScriptCallback) ev -> paint(slices, label, true));
		label.setOnMouseLeaveListener((JavaScriptCallback) ev -> paint(slices, label, false));
		label.setOnOpListener((JavaScriptCallback) ev ->
		{
			Consumer<WindowView> open = onOpen;
			if (open != null)
			{
				open.accept(view);
			}
		});
		label.revalidate();
	}

	/**
	 * Creates one widget of the button at (x, y) within it, placed from the frame's right edge so the button
	 * stays by the close button.
	 */
	private static Widget part(Widget frame, int type, int x, int y, int w, int h)
	{
		Widget part = frame.createChild(-1, type);
		part.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		part.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		part.setOriginalX(RIGHT + WIDTH - x - w);
		part.setOriginalY(TOP + y);
		part.setOriginalWidth(w);
		part.setOriginalHeight(h);
		part.setName(NAME);
		return part;
	}

	private static void paint(Widget[] slices, Widget label, boolean hovered)
	{
		int[] sprites = hovered ? SPRITES_HOVERED : SPRITES;
		for (int i = 0; i < slices.length; i++)
		{
			slices[i].setSpriteId(sprites[i]);
		}
		label.setTextColor(hovered ? TEXT_COLOUR_HOVERED : TEXT_COLOUR);
	}
}
