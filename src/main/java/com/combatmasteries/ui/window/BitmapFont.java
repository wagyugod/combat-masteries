package com.combatmasteries.ui.window;

import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * A game font drawn from its own glyph bitmaps, the way the client draws text: each character's glyph
 * at its left and top bearing from the pen position, then the pen moves on by the character's advance.
 * <p>
 * RuneLite's TrueType copy of the bold font has several glyphs and advances that differ from the game's,
 * so the window draws bold text with the game's glyphs instead. The glyphs are read from a text resource
 * exported from the game cache (one line per character: code, advance, bearings, size and pixel rows).
 */
final class BitmapFont
{
	private static final Charset CP1252 = Charset.forName("windows-1252");

	private final int ascent;
	private final int[] advances = new int[256];
	/**
	 * Per character, runs of ink as {x, y, length} triples relative to the pen position on the baseline.
	 */
	private final int[][] runs = new int[256][];

	private BitmapFont(int ascent)
	{
		this.ascent = ascent;
	}

	/**
	 * @return the font, or null if the resource is missing or unreadable
	 */
	@Nullable
	static BitmapFont load(String resource)
	{
		try (InputStream in = BitmapFont.class.getResourceAsStream(resource))
		{
			if (in == null)
			{
				return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
			BitmapFont font = null;
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				String[] parts = line.split(" ");
				if (parts[0].equals("ascent"))
				{
					font = new BitmapFont(Integer.parseInt(parts[1]));
					continue;
				}
				if (font == null)
				{
					return null;
				}
				font.readGlyph(parts);
			}
			return font;
		}
		catch (IOException | RuntimeException e)
		{
			return null;
		}
	}

	private void readGlyph(String[] parts)
	{
		int code = Integer.parseInt(parts[0]);
		advances[code] = Integer.parseInt(parts[1]);
		int left = Integer.parseInt(parts[2]);
		int top = Integer.parseInt(parts[3]) - ascent;
		int height = Integer.parseInt(parts[5]);
		int[] glyph = new int[0];
		int n = 0;
		for (int row = 0; row < height; row++)
		{
			String pixels = parts[6 + row];
			int x = 0;
			while (x < pixels.length())
			{
				if (pixels.charAt(x) != '#')
				{
					x++;
					continue;
				}
				int start = x;
				while (x < pixels.length() && pixels.charAt(x) == '#')
				{
					x++;
				}
				if (n + 3 > glyph.length)
				{
					glyph = Arrays.copyOf(glyph, glyph.length * 2 + 6);
				}
				glyph[n++] = left + start;
				glyph[n++] = top + row;
				glyph[n++] = x - start;
			}
		}
		runs[code] = Arrays.copyOf(glyph, n);
	}

	private static int code(char c)
	{
		if (c < 128)
		{
			return c;
		}
		byte[] b = String.valueOf(c).getBytes(CP1252);
		return b.length == 1 ? b[0] & 0xff : '?';
	}

	int stringWidth(String text)
	{
		int w = 0;
		for (int i = 0; i < text.length(); i++)
		{
			w += advances[code(text.charAt(i))];
		}
		return w;
	}

	/**
	 * Draws the text in the graphics' current colour with the pen starting at x on the given baseline.
	 *
	 * @return the pen position after the text
	 */
	int drawString(Graphics2D g, String text, int x, int baseline)
	{
		for (int i = 0; i < text.length(); i++)
		{
			int c = code(text.charAt(i));
			int[] glyph = runs[c];
			if (glyph != null)
			{
				for (int r = 0; r < glyph.length; r += 3)
				{
					g.fillRect(x + glyph[r], baseline + glyph[r + 1], glyph[r + 2], 1);
				}
			}
			x += advances[c];
		}
		return x;
	}
}
