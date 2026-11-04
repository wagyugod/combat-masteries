package com.combatmasteries.cosmetic;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;

/**
 * Recolours the Tzkal slayer helmet's eyes on the local player's model.
 * <p>
 * The change is client-side only: nobody else sees it. Colours are painted into the
 * player's model each frame before rendering. The client caches the player's base model
 * and its animated copies share its colour arrays, so the originals are saved and written
 * back when the effect is turned off.
 */
@Singleton
public class HelmetGlow
{
	private static final Set<Integer> TZKAL_HELMETS = ImmutableSet.of(
		ItemID.SLAYER_HELM_ZUK,
		ItemID.SLAYER_HELM_I_ZUK,
		ItemID.SW_SLAYER_HELM_I_ZUK,
		ItemID.PVPA_SLAYER_HELM_I_ZUK
	);
	/**
	 * Distinct models painted since the effect was last cleared. Kept small: the client
	 * rebuilds the model when equipment changes, and older ones fall out of its cache.
	 */
	private static final int MAX_TRACKED_MODELS = 8;

	private final Client client;
	private final Deque<Painted> painted = new ArrayDeque<>();

	@Getter
	@Nullable
	private GlowStyle style;

	/**
	 * The colour array last searched (unlit or lit), so the search runs once per model.
	 */
	private Object lastSearched;
	private HelmetEyes.Location lastLocation;

	@Inject
	HelmetGlow(Client client)
	{
		this.client = client;
	}

	/**
	 * @param style the glow to show, or null for none
	 */
	public void setStyle(@Nullable GlowStyle style)
	{
		if (this.style != style)
		{
			restore();
			this.style = style;
		}
	}

	public boolean isWearingHelmet(@Nullable Player player)
	{
		if (player == null)
		{
			return false;
		}
		PlayerComposition composition = player.getPlayerComposition();
		return composition != null && TZKAL_HELMETS.contains(composition.getEquipmentId(KitType.HEAD));
	}

	/**
	 * Called before each frame is drawn.
	 */
	public void paint()
	{
		if (style == null)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		if (!isWearingHelmet(player))
		{
			return;
		}

		Model model = player.getModel();
		HelmetEyes.Location location = locate(model);
		if (location == null)
		{
			return;
		}

		Painted painted = remember(model.getFaceColors1(), model.getFaceColors2(), model.getFaceColors3(), location);
		painted.paint(style, System.currentTimeMillis());
	}

	/**
	 * Developer mode diagnostics: reports each step of the effect for the local player.
	 */
	public String describe()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getPlayerComposition() == null)
		{
			return "no local player";
		}

		PlayerComposition composition = player.getPlayerComposition();
		StringBuilder sb = new StringBuilder();
		sb.append("style=").append(style)
			.append(", head raw=").append(composition.getEquipmentIds()[KitType.HEAD.getIndex()])
			.append(" item=").append(composition.getEquipmentId(KitType.HEAD))
			.append(", recognised=").append(isWearingHelmet(player));

		Model model = player.getModel();
		if (model == null)
		{
			return sb.append(", model=null").toString();
		}
		short[] unlit = model.getUnlitFaceColors();
		sb.append(", faces=").append(model.getFaceCount())
			.append(", unlit=").append(unlit == null ? "null" : String.valueOf(unlit.length));

		HelmetEyes.Location location = unlit != null
			? HelmetEyes.find(unlit, model.getFaceCount())
			: HelmetEyes.findLit(model.getFaceColors1(), model.getFaceColors3(), model.getFaceCount());
		sb.append(", search=").append(unlit != null ? "unlit" : "lit");
		if (location == null)
		{
			return sb.append(", eyes=not found").toString();
		}
		sb.append(", eyes=").append(location.getVariant()).append("@").append(location.getOffset());

		if (style != null)
		{
			Model again = player.getModel();
			int eye = location.getOffset() + location.getVariant().getBrightFaces()[0];
			sb.append(", painted=").append(model.getFaceColors3()[eye] == -1)
				.append(", sharedColours=").append(again != null && again.getFaceColors1() == model.getFaceColors1());
		}
		return sb.toString();
	}

	/**
	 * Finds the helmet in a model, caching the result for the model's colour array.
	 */
	@Nullable
	public HelmetEyes.Location locate(@Nullable Model model)
	{
		if (model == null)
		{
			return null;
		}

		short[] unlit = model.getUnlitFaceColors();
		if (unlit != null)
		{
			if (unlit != lastSearched)
			{
				lastSearched = unlit;
				lastLocation = HelmetEyes.find(unlit, model.getFaceCount());
			}
			return lastLocation;
		}

		// Player models often have no unlit colours; match the helmet's lit colour pattern
		int[] colors1 = model.getFaceColors1();
		if (colors1 != lastSearched)
		{
			lastSearched = colors1;
			lastLocation = HelmetEyes.findLit(colors1, model.getFaceColors3(), model.getFaceCount());
		}
		return lastLocation;
	}

	/**
	 * Writes back every colour this class changed.
	 */
	public void restore()
	{
		for (Painted p : painted)
		{
			p.restore();
		}
		painted.clear();
	}

	private Painted remember(int[] colors1, int[] colors2, int[] colors3, HelmetEyes.Location location)
	{
		for (Painted p : painted)
		{
			if (p.colors1 == colors1)
			{
				return p;
			}
		}

		if (painted.size() >= MAX_TRACKED_MODELS)
		{
			painted.removeFirst();
		}
		Painted p = new Painted(colors1, colors2, colors3, location);
		painted.addLast(p);
		return p;
	}

	/**
	 * One model's eye and rim faces: their original colours, for repainting each frame and
	 * for restoring.
	 */
	private static final class Painted
	{
		final int[] colors1;
		final int[] colors2;
		final int[] colors3;
		final HelmetEyes.Location location;
		final int[] faces;
		final int[] saved;

		Painted(int[] colors1, int[] colors2, int[] colors3, HelmetEyes.Location location)
		{
			this.colors1 = colors1;
			this.colors2 = colors2;
			this.colors3 = colors3;
			this.location = location;

			HelmetEyes.Variant v = location.getVariant();
			int[][] groups = {v.getBrightFaces(), v.getDimFaces(), v.getInnerRim(), v.getOuterRim()};
			int count = 0;
			for (int[] g : groups)
			{
				count += g.length;
			}
			faces = new int[count];
			int i = 0;
			for (int[] g : groups)
			{
				for (int face : g)
				{
					faces[i++] = location.getOffset() + face;
				}
			}

			saved = new int[faces.length * 3];
			for (i = 0; i < faces.length; i++)
			{
				saved[i * 3] = colors1[faces[i]];
				saved[i * 3 + 1] = colors2[faces[i]];
				saved[i * 3 + 2] = colors3[faces[i]];
			}
		}

		void paint(GlowStyle style, long millis)
		{
			HelmetEyes.Variant v = location.getVariant();
			int offset = location.getOffset();
			int i = 0;

			// Eye slit: flat colour, brightness travelling across it
			for (int k = 0; k < v.getBrightFaces().length; k++, i++)
			{
				flat(faces[i], style.brightAt(GlowStyle.phaseAcross(millis, v.getBrightX()[k])));
			}
			for (int k = 0; k < v.getDimFaces().length; k++, i++)
			{
				flat(faces[i], style.dimAt(GlowStyle.phaseAcross(millis, v.getDimX()[k])));
			}

			// Metal around it: the glow's hue over the face's own shading
			float phase = GlowStyle.phase(millis);
			int hue = style.hueAt(phase);
			int saturation = style.saturationAt(phase);
			i = tintRim(i, v.getInnerRim().length, hue, Math.min(saturation, GlowStyle.INNER_RIM_SATURATION), GlowStyle.INNER_RIM_LUMINANCE);
			tintRim(i, v.getOuterRim().length, hue, Math.min(saturation, GlowStyle.OUTER_RIM_SATURATION), GlowStyle.OUTER_RIM_LUMINANCE);
		}

		private void flat(int face, int hsl)
		{
			colors1[face] = hsl;
			colors2[face] = hsl;
			colors3[face] = -1;
		}

		private int tintRim(int start, int count, int hue, int saturation, int luminance)
		{
			for (int i = start; i < start + count; i++)
			{
				int face = faces[i];
				if (saved[i * 3 + 2] == -2)
				{
					continue;
				}
				colors1[face] = HelmetEyes.tint(saved[i * 3], hue, saturation, luminance);
				colors2[face] = HelmetEyes.tint(saved[i * 3 + 1], hue, saturation, luminance);
				colors3[face] = HelmetEyes.tint(saved[i * 3 + 2], hue, saturation, luminance);
			}
			return start + count;
		}

		void restore()
		{
			for (int i = 0; i < faces.length; i++)
			{
				colors1[faces[i]] = saved[i * 3];
				colors2[faces[i]] = saved[i * 3 + 1];
				colors3[faces[i]] = saved[i * 3 + 2];
			}
		}
	}
}
