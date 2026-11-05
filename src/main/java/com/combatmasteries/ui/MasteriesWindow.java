package com.combatmasteries.ui;

import com.combatmasteries.ui.window.SpriteManagerSource;
import com.combatmasteries.ui.window.SpriteSource;
import com.combatmasteries.ui.window.WindowChrome;
import com.combatmasteries.ui.window.WindowController;
import com.combatmasteries.ui.window.WindowModel;
import com.combatmasteries.ui.window.WindowView;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The Combat Masteries window in game: drawn over the game like the Combat Achievements window, and taking
 * the mouse and keyboard while it is open. Clicks, drags and wheel turns inside it never reach the game; a
 * left click outside it closes it and goes on to the game, as acting elsewhere closes the game's windows.
 * <p>
 * Drawing happens on the client thread and input arrives on the AWT thread; {@link WindowController}
 * synchronizes the two. The model is replaced whole, never changed, so it can be read from either.
 */
@Singleton
public class MasteriesWindow extends Overlay implements MouseListener, MouseWheelListener, KeyListener
{
	/**
	 * The Combat Achievements window's own minimum height.
	 */
	static final int MIN_HEIGHT = 314;
	/**
	 * Space above and below the window when it is placed in the viewport rather than over an open Combat
	 * Achievements window.
	 */
	static final int MARGIN = 16;
	/**
	 * How long the window's last drawn bounds stay valid for input. If the overlay stops being drawn (the
	 * client stops drawing overlays) the window must not keep taking clicks where it is no longer seen.
	 */
	static final long STALE_MILLIS = 1000;
	private static final int[] CA_UNIVERSES = {
		InterfaceID.CaOverview.UNIVERSE, InterfaceID.CaTasks.UNIVERSE, InterfaceID.CaBosses.UNIVERSE, InterfaceID.CaRewards.UNIVERSE,
	};

	private final Client client;
	private final SpriteSource sprites;
	private final WindowController controller = new WindowController();

	private volatile boolean open;
	@Nullable
	private volatile WindowModel model;
	/**
	 * Where the window was drawn last frame, in canvas coordinates, or null if it was not drawn.
	 */
	@Nullable
	private volatile Rectangle bounds;
	private volatile long boundsDrawnAt;
	/**
	 * Whether the current mouse press started inside the window, so its drag and release are ours too.
	 * Only touched on the AWT thread.
	 */
	private boolean pressedInside;
	/**
	 * Whether the last mouse press was taken by the window, so the click event that follows its release is
	 * kept from the game too. Only touched on the AWT thread.
	 */
	private boolean clickOurs;

	@Inject
	MasteriesWindow(Client client, SpriteManager spriteManager)
	{
		this.client = client;
		this.sprites = new SpriteManagerSource(spriteManager);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGHEST);
	}

	public boolean isOpen()
	{
		return open;
	}

	/**
	 * Opens the window on a tab. Call on the client thread.
	 */
	public void open(WindowView view, WindowModel model)
	{
		this.model = model;
		controller.show(view);
		open = true;
	}

	/**
	 * Replaces what the window shows, keeping the tab, filters and scrolling.
	 */
	public void setModel(WindowModel model)
	{
		this.model = model;
	}

	public void close()
	{
		open = false;
		bounds = null;
		controller.release();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WindowModel shown = model;
		if (!open || shown == null)
		{
			bounds = null;
			return null;
		}

		Rectangle area = windowBounds();
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.translate(area.x, area.y);
			g.clipRect(0, 0, area.width, area.height);
			controller.render(g, sprites, shown, area.width, area.height, System.currentTimeMillis());
		}
		finally
		{
			g.dispose();
		}
		bounds = area;
		boundsDrawnAt = System.currentTimeMillis();
		return null;
	}

	/**
	 * Over an open Combat Achievements window, exactly where it is. Otherwise centred in the viewport,
	 * stretched to its height as the game stretches the Combat Achievements window.
	 */
	private Rectangle windowBounds()
	{
		for (int id : CA_UNIVERSES)
		{
			Widget universe = client.getWidget(id);
			if (universe != null && !universe.isHidden())
			{
				Rectangle ca = universe.getBounds();
				if (ca.width >= WindowChrome.WIDTH && ca.height >= MIN_HEIGHT)
				{
					return new Rectangle(ca.x + (ca.width - WindowChrome.WIDTH) / 2, ca.y, WindowChrome.WIDTH, ca.height);
				}
			}
		}
		return centred(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(),
			client.getViewportHeight());
	}

	/**
	 * @return the window's bounds centred in a viewport, as tall as it allows less a margin, and never
	 * shorter than the Combat Achievements window's minimum
	 */
	static Rectangle centred(int x, int y, int w, int h)
	{
		int height = Math.max(MIN_HEIGHT, h - 2 * MARGIN);
		return new Rectangle(x + (w - WindowChrome.WIDTH) / 2, y + (h - height) / 2, WindowChrome.WIDTH, height);
	}

	// ---- mouse ----

	/**
	 * @return where the window was drawn if it is open and was drawn just now, else null
	 */
	@Nullable
	private Rectangle shown()
	{
		Rectangle area = bounds;
		return open && area != null && System.currentTimeMillis() - boundsDrawnAt < STALE_MILLIS ? area : null;
	}

	/**
	 * @return the window's bounds if it is shown and the event is inside them, and the game's right-click
	 * menu is not open (it is drawn over the window and takes the mouse first), else null
	 */
	@Nullable
	private Rectangle hit(MouseEvent e)
	{
		Rectangle area = shown();
		return area != null && area.contains(e.getPoint()) && !client.isMenuOpen() ? area : null;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		Rectangle area = hit(e);
		pressedInside = area != null;
		clickOurs = area != null;
		if (area == null)
		{
			if (shown() != null && !client.isMenuOpen() && SwingUtilities.isLeftMouseButton(e))
			{
				// Like the game's own windows, acting elsewhere closes it; the click still reaches the game
				close();
			}
			return e;
		}
		if (SwingUtilities.isLeftMouseButton(e)
			&& controller.press(e.getX() - area.x, e.getY() - area.y, System.currentTimeMillis()))
		{
			close();
		}
		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		// Only a release whose press the window took: the game must see the release of any press it saw
		// (a camera drag with the middle button, say), or it would think the button is still held
		boolean ours = pressedInside;
		pressedInside = false;
		controller.release();
		if (ours)
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (clickOurs)
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		Rectangle area = shown();
		if (area != null && pressedInside)
		{
			controller.drag(e.getX() - area.x, e.getY() - area.y);
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		Rectangle area = hit(e);
		if (area == null)
		{
			controller.exit();
			return e;
		}
		controller.move(e.getX() - area.x, e.getY() - area.y);
		// The game would otherwise hover (and show menu options for) what is under the window
		e.consume();
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		controller.exit();
		return e;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e)
	{
		if (hit(e) != null)
		{
			controller.wheel(e.getWheelRotation());
			e.consume();
		}
		return e;
	}

	// ---- keyboard ----

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (shown() == null)
		{
			return;
		}
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			if (controller.escape())
			{
				close();
			}
			e.consume();
		}
		else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
		{
			if (controller.backspace())
			{
				e.consume();
			}
		}
		else if (isTypeable(e.getKeyChar()) && !e.isControlDown() && !e.isAltDown() && controller.type(e.getKeyChar()))
		{
			// The character goes to the Bosses search, not the chatbox
			e.consume();
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// Characters were taken on key press; keep the typed event from the chatbox too
		if (shown() != null && isTypeable(e.getKeyChar()) && !e.isControlDown() && !e.isAltDown()
			&& controller.isSearching())
		{
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	private static boolean isTypeable(char c)
	{
		return c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c);
	}
}
