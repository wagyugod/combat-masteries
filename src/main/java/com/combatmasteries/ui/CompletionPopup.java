package com.combatmasteries.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.callback.ClientThread;

/**
 * Shows the game's own notification pop-up (the one Combat Achievements and the collection
 * log use), with its open, hold and close animation.
 * <p>
 * Pop-ups are queued and shown one at a time, and never while the game is already showing
 * one. Everything here runs on the client thread.
 */
@Singleton
public class CompletionPopup
{
	/**
	 * notification_display_init(title, text, colour): fills in the pop-up and starts its
	 * animation. A colour of -1 gives the default orange title used by Combat Achievements.
	 */
	private static final int NOTIFICATION_DISPLAY_INIT = 3343;

	private final Client client;
	private final ClientThread clientThread;
	private final Deque<String[]> queue = new ArrayDeque<>();
	private boolean showing;

	@Inject
	CompletionPopup(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	public void show(String title, String text)
	{
		queue.add(new String[]{title, text});
	}

	public void clear()
	{
		queue.clear();
	}

	/**
	 * Called once per game tick: starts the next queued pop-up if nothing is showing.
	 */
	public void onTick()
	{
		if (showing || queue.isEmpty() || client.getWidget(InterfaceID.NotificationDisplay.CONTAINER) != null)
		{
			return;
		}

		int slot = notificationSlot();
		if (slot == -1)
		{
			return;
		}

		String[] next = queue.poll();
		WidgetNode node = client.openInterface(slot, InterfaceID.NOTIFICATION_DISPLAY, WidgetModalMode.MODAL_CLICKTHROUGH);
		client.runScript(NOTIFICATION_DISPLAY_INIT, next[0], next[1], -1);
		showing = true;

		// The pop-up shrinks back to nothing when its animation ends; then close it
		clientThread.invokeLater(() ->
		{
			Widget container = client.getWidget(InterfaceID.NotificationDisplay.CONTAINER);
			if (container != null && container.getWidth() > 0)
			{
				return false;
			}
			client.closeInterface(node, true);
			showing = false;
			return true;
		});
	}

	/**
	 * @return the notification slot of the current game frame (fixed, resizable classic or
	 * resizable modern), or -1 if unknown
	 */
	private int notificationSlot()
	{
		switch (client.getTopLevelInterfaceId())
		{
			case InterfaceID.TOPLEVEL:
				return InterfaceID.Toplevel.NOTIFICATIONS;
			case InterfaceID.TOPLEVEL_OSRS_STRETCH:
				return InterfaceID.ToplevelOsrsStretch.NOTIFICATIONS;
			case InterfaceID.TOPLEVEL_PRE_EOC:
				return InterfaceID.ToplevelPreEoc.NOTIFICATIONS;
			default:
				return -1;
		}
	}
}
