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

	private short[] lastUnlit;
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

		int[] colors1 = model.getFaceColors1();
		int[] colors2 = model.getFaceColors2();
		int[] colors3 = model.getFaceColors3();
		if (HelmetEyes.isPainted(colors1, colors3, location, style.getBrightHsl()))
		{
			return;
		}

		remember(colors1, colors2, colors3, location);
		HelmetEyes.paint(colors1, colors2, colors3, location, style.getBrightHsl(), style.getDimHsl());
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
		if (unlit == null)
		{
			return null;
		}
		if (unlit != lastUnlit)
		{
			lastUnlit = unlit;
			lastLocation = HelmetEyes.find(unlit, model.getFaceCount());
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

	private void remember(int[] colors1, int[] colors2, int[] colors3, HelmetEyes.Location location)
	{
		for (Painted p : painted)
		{
			if (p.colors1 == colors1)
			{
				return;
			}
		}

		if (painted.size() >= MAX_TRACKED_MODELS)
		{
			painted.removeFirst();
		}
		painted.addLast(new Painted(colors1, colors2, colors3, location));
	}

	/**
	 * Original eye colours of one model, for restoring.
	 */
	private static final class Painted
	{
		final int[] colors1;
		final int[] colors2;
		final int[] colors3;
		final int[] faces;
		final int[] saved;

		Painted(int[] colors1, int[] colors2, int[] colors3, HelmetEyes.Location location)
		{
			this.colors1 = colors1;
			this.colors2 = colors2;
			this.colors3 = colors3;

			HelmetEyes.Variant variant = location.getVariant();
			int bright = variant.getBrightFaces().length;
			faces = new int[bright + variant.getDimFaces().length];
			for (int i = 0; i < faces.length; i++)
			{
				int face = i < bright ? variant.getBrightFaces()[i] : variant.getDimFaces()[i - bright];
				faces[i] = location.getOffset() + face;
			}

			saved = new int[faces.length * 3];
			for (int i = 0; i < faces.length; i++)
			{
				saved[i * 3] = colors1[faces[i]];
				saved[i * 3 + 1] = colors2[faces[i]];
				saved[i * 3 + 2] = colors3[faces[i]];
			}
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
