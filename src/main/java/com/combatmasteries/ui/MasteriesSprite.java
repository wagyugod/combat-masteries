package com.combatmasteries.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.game.SpriteOverride;

/**
 * The plugin's own sprites, added to the client so game widgets can show them.
 */
@Getter
@RequiredArgsConstructor
public enum MasteriesSprite implements SpriteOverride
{
	/**
	 * The button on the Combat Achievements window's title bar: the sidebar's helmet icon.
	 */
	WINDOW_BUTTON(-20_250_101, "/com/combatmasteries/icon.png");

	private final int spriteId;
	private final String fileName;
}
