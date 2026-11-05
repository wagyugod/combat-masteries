package com.combatmasteries.ui.window;

import java.awt.Font;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.FontManager;

/**
 * The three game fonts the Combat Achievements window uses, with the game's own vertical metrics so text
 * lands on the same baseline as in the client.
 * <p>
 * The client places a line of text with {@code maxAscent} and {@code maxDescent}, which it works out from
 * the font's glyphs, and steps between lines by the font's ascent unless the widget sets a line height.
 */
@Getter
@RequiredArgsConstructor
public enum GameFont
{
	/**
	 * p11_full (494).
	 */
	SMALL(10, 10, 2, 1),
	/**
	 * p12_full (495).
	 */
	REGULAR(12, 12, 4, 2),
	/**
	 * b12_full (496).
	 */
	BOLD(12, 12, 4, 2);

	private final int ascent;
	private final int maxAscent;
	private final int maxDescent;
	/**
	 * How far below the game's baseline RuneLite's TrueType copy of the font has to be drawn for its
	 * glyphs to cover the same pixels.
	 */
	private final int baselineShift;

	public Font font()
	{
		switch (this)
		{
			case SMALL:
				return FontManager.getRunescapeSmallFont();
			case BOLD:
				return FontManager.getRunescapeBoldFont();
			default:
				return FontManager.getRunescapeFont();
		}
	}
}
