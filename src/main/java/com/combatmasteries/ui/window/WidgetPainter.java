package com.combatmasteries.ui.window;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the building blocks of game interfaces the way the client does: sprites (tiled, stretched or at
 * their own size), rectangles with the game's transparency, and text with shadows, {@code <col>} tags,
 * word wrapping and the client's alignment rules.
 * <p>
 * Coordinates are window pixels; one game pixel is one Java2D unit.
 */
public final class WidgetPainter
{
	/**
	 * Offsets of the sprites the window uses that do not fill their canvas: id to
	 * {offsetX, offsetY, canvasWidth, canvasHeight}. The images a {@link SpriteSource} hands out are trimmed.
	 */
	private static final Map<Integer, int[]> OFFSETS = new HashMap<>();

	static
	{
		for (int id : new int[]{172, 315, 2561})
		{
			OFFSETS.put(id, new int[]{15, 0, 36, 36});
		}
		for (int id : new int[]{173, 314, 2546})
		{
			OFFSETS.put(id, new int[]{0, 15, 36, 36});
		}
		for (int id : new int[]{917, 918, 925, 926})
		{
			OFFSETS.put(id, new int[]{0, 0, 9, 9});
		}
		OFFSETS.put(535, new int[]{1, 1, 26, 23});
		OFFSETS.put(536, new int[]{1, 1, 26, 23});
		OFFSETS.put(919, new int[]{6, 0, 9, 9});
		OFFSETS.put(920, new int[]{0, 6, 9, 9});
		OFFSETS.put(927, new int[]{6, 0, 9, 9});
		OFFSETS.put(928, new int[]{0, 6, 9, 9});
		OFFSETS.put(2314, new int[]{0, 6, 18, 18});
		OFFSETS.put(2315, new int[]{0, 6, 18, 18});
	}

	private final Graphics2D g;
	private final SpriteSource sprites;

	public WidgetPainter(Graphics2D g, SpriteSource sprites)
	{
		this.g = g;
		this.sprites = sprites;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	}

	public Graphics2D graphics()
	{
		return g;
	}

	// ---- sprites ----

	/**
	 * A graphic widget: the sprite tiled over the box (starting at its top left, clipped at the far edges),
	 * or stretched to fill it.
	 */
	public void graphic(int id, int x, int y, int w, int h, boolean tiled)
	{
		BufferedImage img = sprites.sprite(id, 0);
		if (img == null || w <= 0 || h <= 0)
		{
			return;
		}
		int[] off = offsets(id, img);
		if (tiled)
		{
			Shape clip = g.getClip();
			g.clipRect(x, y, w, h);
			for (int ty = y; ty < y + h; ty += off[3])
			{
				for (int tx = x; tx < x + w; tx += off[2])
				{
					g.drawImage(img, tx + off[0], ty + off[1], null);
				}
			}
			g.setClip(clip);
		}
		else if (w == off[2] && h == off[3])
		{
			g.drawImage(img, x + off[0], y + off[1], null);
		}
		else
		{
			// The client scales the whole canvas, offsets included
			int dx0 = x + off[0] * w / off[2];
			int dy0 = y + off[1] * h / off[3];
			int dx1 = x + (off[0] + img.getWidth()) * w / off[2];
			int dy1 = y + (off[1] + img.getHeight()) * h / off[3];
			g.drawImage(img, dx0, dy0, dx1, dy1, 0, 0, img.getWidth(), img.getHeight(), null);
		}
	}

	/**
	 * A sprite at its own size.
	 */
	public void sprite(int id, int x, int y)
	{
		BufferedImage img = sprites.sprite(id, 0);
		if (img != null)
		{
			int[] off = offsets(id, img);
			g.drawImage(img, x + off[0], y + off[1], null);
		}
	}

	public void image(BufferedImage img, int x, int y)
	{
		if (img != null)
		{
			g.drawImage(img, x, y, null);
		}
	}

	private static int[] offsets(int id, BufferedImage img)
	{
		int[] off = OFFSETS.get(id);
		return off != null ? off : new int[]{0, 0, img.getWidth(), img.getHeight()};
	}

	// ---- rectangles ----

	/**
	 * A filled rectangle widget. {@code trans} is the game's transparency: 0 opaque, 255 invisible.
	 */
	public void fill(int x, int y, int w, int h, int rgb, int trans)
	{
		if (trans >= 255 || w <= 0 || h <= 0)
		{
			return;
		}
		withAlpha(trans, () ->
		{
			g.setColor(new Color(rgb));
			g.fillRect(x, y, w, h);
		});
	}

	/**
	 * An unfilled rectangle widget: a 1px line just inside the box.
	 */
	public void outline(int x, int y, int w, int h, int rgb, int trans)
	{
		if (trans >= 255 || w <= 0 || h <= 0)
		{
			return;
		}
		withAlpha(trans, () ->
		{
			g.setColor(new Color(rgb));
			g.drawRect(x, y, w - 1, h - 1);
		});
	}

	public void outline(int x, int y, int w, int h, int rgb)
	{
		outline(x, y, w, h, rgb, 0);
	}

	private void withAlpha(int trans, Runnable draw)
	{
		Composite before = g.getComposite();
		if (trans > 0)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (255 - trans) / 255f));
		}
		draw.run();
		g.setComposite(before);
	}

	// ---- text ----

	public static final int LEFT = 0;
	public static final int CENTRE = 1;
	public static final int RIGHT = 2;
	public static final int TOP = 0;
	public static final int MIDDLE = 1;
	public static final int BOTTOM = 2;

	/**
	 * A text widget. Supports {@code <col=rrggbb>}, {@code </col>} and {@code <br>}, and wraps at spaces
	 * when a line is wider than the box, like the client.
	 *
	 * @param lineHeight 0 for the font's own
	 * @return the number of lines drawn
	 */
	public int text(String text, GameFont font, int rgb, boolean shadow, int x, int y, int w, int h,
		int xAlign, int yAlign, int lineHeight)
	{
		List<Line> lines = layout(text, font, rgb, w);
		int lh = lineHeight == 0 ? font.getAscent() : lineHeight;
		int n = lines.size();
		int baseline;
		if (yAlign == TOP)
		{
			baseline = y + font.getMaxAscent();
		}
		else if (yAlign == MIDDLE)
		{
			baseline = y + (h - font.getMaxAscent() - font.getMaxDescent() - lh * (n - 1)) / 2 + font.getMaxAscent();
		}
		else
		{
			baseline = y + h - font.getMaxDescent() - lh * (n - 1);
		}

		baseline += font.getBaselineShift();
		g.setFont(font.font());
		FontMetrics fm = g.getFontMetrics();
		for (Line line : lines)
		{
			int lw = fm.stringWidth(line.plain());
			int lx = xAlign == LEFT ? x : xAlign == CENTRE ? x + (w - lw) / 2 : x + w - lw;
			if (shadow)
			{
				g.setColor(Color.BLACK);
				g.drawString(line.plain(), lx + 1, baseline + 1);
			}
			int cx = lx;
			for (Run run : line.runs)
			{
				g.setColor(new Color(run.rgb));
				g.drawString(run.text, cx, baseline);
				cx += fm.stringWidth(run.text);
			}
			baseline += lh;
		}
		return n;
	}

	/**
	 * Single line, vertically centred, with shadow.
	 */
	public void text(String text, GameFont font, int rgb, int x, int y, int w, int h, int xAlign)
	{
		text(text, font, rgb, true, x, y, w, h, xAlign, MIDDLE, 0);
	}

	/**
	 * @return how many lines the text takes in a box of the given width
	 */
	public int lineCount(String text, GameFont font, int w)
	{
		return layout(text, font, 0, w).size();
	}

	public int width(String text, GameFont font)
	{
		StringBuilder plain = new StringBuilder();
		for (Line line : layout(text, font, 0, 0))
		{
			plain.append(line.plain());
		}
		return g.getFontMetrics(font.font()).stringWidth(plain.toString());
	}

	private static final class Run
	{
		final String text;
		final int rgb;

		Run(String text, int rgb)
		{
			this.text = text;
			this.rgb = rgb;
		}
	}

	private static final class Line
	{
		final List<Run> runs = new ArrayList<>();

		String plain()
		{
			StringBuilder sb = new StringBuilder();
			for (Run r : runs)
			{
				sb.append(r.text);
			}
			return sb.toString();
		}
	}

	/**
	 * Splits tagged text into lines of coloured runs, breaking at {@code <br>} and wrapping at spaces.
	 */
	private List<Line> layout(String text, GameFont font, int defaultRgb, int width)
	{
		FontMetrics fm = g.getFontMetrics(font.font());
		// One entry per visible character with its colour, plus line breaks as -1
		StringBuilder chars = new StringBuilder();
		List<Integer> colours = new ArrayList<>();
		int rgb = defaultRgb;
		int i = 0;
		while (i < text.length())
		{
			char c = text.charAt(i);
			if (c == '<')
			{
				int end = text.indexOf('>', i);
				if (end > 0)
				{
					String tag = text.substring(i + 1, end);
					if (tag.startsWith("col="))
					{
						rgb = Integer.parseInt(tag.substring(4), 16);
					}
					else if (tag.equals("/col"))
					{
						rgb = defaultRgb;
					}
					else if (tag.equals("br"))
					{
						chars.append('\n');
						colours.add(rgb);
					}
					else if (tag.equals("lt"))
					{
						chars.append('<');
						colours.add(rgb);
					}
					else if (tag.equals("gt"))
					{
						chars.append('>');
						colours.add(rgb);
					}
					i = end + 1;
					continue;
				}
			}
			chars.append(c);
			colours.add(rgb);
			i++;
		}

		List<int[]> spans = new ArrayList<>();
		String all = chars.toString();
		int start = 0;
		while (start <= all.length())
		{
			int nl = all.indexOf('\n', start);
			int end = nl < 0 ? all.length() : nl;
			wrap(all, start, end, width, fm, spans);
			if (nl < 0)
			{
				break;
			}
			start = nl + 1;
		}

		List<Line> lines = new ArrayList<>();
		for (int[] span : spans)
		{
			Line line = new Line();
			int s = span[0];
			while (s < span[1])
			{
				int c = colours.get(s);
				int e = s;
				while (e < span[1] && colours.get(e) == c)
				{
					e++;
				}
				line.runs.add(new Run(all.substring(s, e), c));
				s = e;
			}
			lines.add(line);
		}
		return lines;
	}

	private static void wrap(String all, int start, int end, int width, FontMetrics fm, List<int[]> spans)
	{
		if (width <= 0 || fm.stringWidth(all.substring(start, end)) <= width)
		{
			spans.add(new int[]{start, end});
			return;
		}
		int lineStart = start;
		int lastSpace = -1;
		for (int i = start; i < end; i++)
		{
			if (all.charAt(i) == ' ')
			{
				if (fm.stringWidth(all.substring(lineStart, i)) > width && lastSpace > lineStart)
				{
					spans.add(new int[]{lineStart, lastSpace});
					lineStart = lastSpace + 1;
				}
				lastSpace = i;
			}
		}
		if (fm.stringWidth(all.substring(lineStart, end)) > width && lastSpace > lineStart)
		{
			spans.add(new int[]{lineStart, lastSpace});
			lineStart = lastSpace + 1;
		}
		spans.add(new int[]{lineStart, end});
	}
}
