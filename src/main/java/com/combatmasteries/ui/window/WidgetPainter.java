package com.combatmasteries.ui.window;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
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
	 * A one pixel tall run of solid colour; used for gradients given row by row.
	 */
	public void hline(int x, int y, int w, int rgb)
	{
		fill(x, y, w, 1, rgb, 0);
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
			g.setComposite(new ClientBlend(256 - trans));
		}
		draw.run();
		g.setComposite(before);
	}

	/**
	 * Blends the way the client's rasteriser fills a translucent rectangle: with {@code a = 256 - trans},
	 * each channel is {@code (src * a >> 8) + (dst * (256 - a) >> 8)}, the two halves rounded down
	 * separately. Java2D's own alpha blending rounds, which leaves translucent rows a shade brighter than
	 * the game's.
	 */
	private static final class ClientBlend implements Composite
	{
		private final int alpha;

		ClientBlend(int alpha)
		{
			this.alpha = alpha;
		}

		@Override
		public CompositeContext createContext(ColorModel srcModel, ColorModel dstModel, RenderingHints hints)
		{
			return new CompositeContext()
			{
				@Override
				public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
				{
					int w = Math.min(src.getWidth(), dstIn.getWidth());
					int h = Math.min(src.getHeight(), dstIn.getHeight());
					Object srcPixel = null;
					Object dstPixel = null;
					for (int y = 0; y < h; y++)
					{
						for (int x = 0; x < w; x++)
						{
							srcPixel = src.getDataElements(src.getMinX() + x, src.getMinY() + y, srcPixel);
							dstPixel = dstIn.getDataElements(dstIn.getMinX() + x, dstIn.getMinY() + y, dstPixel);
							int s = srcModel.getRGB(srcPixel);
							int d = dstModel.getRGB(dstPixel);
							if ((s >>> 24) != 0)
							{
								d = blend(s, d, alpha);
							}
							dstPixel = dstModel.getDataElements(d, dstPixel);
							dstOut.setDataElements(dstOut.getMinX() + x, dstOut.getMinY() + y, dstPixel);
						}
					}
				}

				@Override
				public void dispose()
				{
				}
			};
		}

		static int blend(int src, int dst, int a)
		{
			int inv = 256 - a;
			int rb = ((src & 0xff00ff) * a >> 8 & 0xff00ff) + ((dst & 0xff00ff) * inv >> 8 & 0xff00ff);
			int gr = ((src & 0xff00) * a >> 8 & 0xff00) + ((dst & 0xff00) * inv >> 8 & 0xff00);
			int dstAlpha = dst >>> 24;
			int outAlpha = dstAlpha + ((255 - dstAlpha) * a >> 8);
			return outAlpha << 24 | rb | gr;
		}
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

		for (Line line : lines)
		{
			int lw = stringWidth(line.plain(), font);
			int lx = xAlign == LEFT ? x : xAlign == CENTRE ? x + (w - lw) / 2 : x + w - lw;
			if (shadow)
			{
				g.setColor(Color.BLACK);
				drawString(line.plain(), font, lx + 1, baseline + 1);
			}
			int cx = lx;
			for (Run run : line.runs)
			{
				g.setColor(new Color(run.rgb));
				cx = drawString(run.text, font, cx, baseline);
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
		return stringWidth(plain.toString(), font);
	}

	/**
	 * @return the width of plain (untagged) text in the given font
	 */
	private int stringWidth(String text, GameFont font)
	{
		BitmapFont bitmap = font.bitmap();
		return bitmap != null ? bitmap.stringWidth(text) : g.getFontMetrics(font.font()).stringWidth(text);
	}

	/**
	 * Draws plain text in the current colour with the pen at x on the game's baseline.
	 *
	 * @return the pen position after the text
	 */
	private int drawString(String text, GameFont font, int x, int baseline)
	{
		BitmapFont bitmap = font.bitmap();
		if (bitmap != null)
		{
			return bitmap.drawString(g, text, x, baseline);
		}
		g.setFont(font.font());
		g.drawString(text, x, baseline + font.getBaselineShift());
		return x + g.getFontMetrics().stringWidth(text);
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
			wrap(all, start, end, width, font, spans);
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

	private void wrap(String all, int start, int end, int width, GameFont font, List<int[]> spans)
	{
		if (width <= 0 || stringWidth(all.substring(start, end), font) <= width)
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
				if (stringWidth(all.substring(lineStart, i), font) > width && lastSpace > lineStart)
				{
					spans.add(new int[]{lineStart, lastSpace});
					lineStart = lastSpace + 1;
				}
				lastSpace = i;
			}
		}
		if (stringWidth(all.substring(lineStart, end), font) > width && lastSpace > lineStart)
		{
			spans.add(new int[]{lineStart, lastSpace});
			lineStart = lastSpace + 1;
		}
		spans.add(new int[]{lineStart, end});
	}
}
