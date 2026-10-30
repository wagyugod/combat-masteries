package com.combatmasteries.cosmetic;

import com.combatmasteries.CombatMasteriesConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws a soft halo over the helmet's eyes so the recoloured faces read as glowing.
 * Hidden when the player faces away from the camera.
 */
public class HelmetGlowOverlay extends Overlay
{
	/**
	 * Model units either side of the eye centre used to size the halo with camera zoom.
	 */
	private static final int SIZE_PROBE = 12;

	private final Client client;
	private final HelmetGlow helmetGlow;
	private final CombatMasteriesConfig config;

	@Inject
	HelmetGlowOverlay(Client client, HelmetGlow helmetGlow, CombatMasteriesConfig config)
	{
		this.client = client;
		this.helmetGlow = helmetGlow;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		GlowStyle style = helmetGlow.getStyle();
		int intensity = config.haloIntensity();
		if (style == null || intensity <= 0)
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		if (!helmetGlow.isWearingHelmet(player))
		{
			return null;
		}

		Model model = player.getModel();
		HelmetEyes.Location location = helmetGlow.locate(model);
		LocalPoint lp = player.getLocalLocation();
		if (location == null || lp == null)
		{
			return null;
		}

		float[] vx = model.getVerticesX();
		float[] vy = model.getVerticesY();
		float[] vz = model.getVerticesZ();
		int[] i1 = model.getFaceIndices1();
		int[] i2 = model.getFaceIndices2();
		int[] i3 = model.getFaceIndices3();

		// Centre of the eyes and of the whole helmet, in model space
		float[] eye = centroid(location.getVariant().getBrightFaces(), location.getOffset(), vx, vy, vz, i1, i2, i3);
		float[] head = blockCentroid(location, vx, vy, vz, i1, i2, i3);

		int orientation = player.getCurrentOrientation() & 2047;
		float sin = Perspective.SINEF[orientation];
		float cos = Perspective.COSINEF[orientation];

		float eyeX = lp.getX() + eye[2] * sin + eye[0] * cos;
		float eyeY = lp.getY() + eye[2] * cos - eye[0] * sin;
		float faceX = (eye[2] - head[2]) * sin + (eye[0] - head[0]) * cos;
		float faceY = (eye[2] - head[2]) * cos - (eye[0] - head[0]) * sin;

		// Skip when the eyes point away from the camera
		float toCameraX = client.getCameraX() - eyeX;
		float toCameraY = client.getCameraY() - eyeY;
		if (faceX * toCameraX + faceY * toCameraY <= 0)
		{
			return null;
		}

		int plane = player.getWorldView().getPlane();
		int tileHeight = Perspective.getTileHeight(client, lp, plane);
		int height = tileHeight + (int) eye[1];
		Point centre = Perspective.localToCanvas(client, (int) eyeX, (int) eyeY, height);
		Point probe = Perspective.localToCanvas(client, (int) (eyeX + SIZE_PROBE * cos), (int) (eyeY - SIZE_PROBE * sin), height);
		if (centre == null || probe == null)
		{
			return null;
		}

		float radius = Math.max(3f, (float) Math.hypot(probe.getX() - centre.getX(), probe.getY() - centre.getY()) * 1.6f);
		Color c = style.getHaloColor();
		int alpha = Math.min(255, intensity * 2);
		RadialGradientPaint paint = new RadialGradientPaint(
			centre.getX(), centre.getY(), radius,
			new float[]{0f, 0.35f, 1f},
			new Color[]{
				new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha),
				new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha / 3),
				new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)
			});
		graphics.setPaint(paint);
		graphics.fill(new Ellipse2D.Float(centre.getX() - radius, centre.getY() - radius, radius * 2, radius * 2));
		return null;
	}

	private static float[] centroid(int[] faces, int offset, float[] vx, float[] vy, float[] vz, int[] i1, int[] i2, int[] i3)
	{
		float x = 0, y = 0, z = 0;
		for (int face : faces)
		{
			int f = offset + face;
			x += vx[i1[f]] + vx[i2[f]] + vx[i3[f]];
			y += vy[i1[f]] + vy[i2[f]] + vy[i3[f]];
			z += vz[i1[f]] + vz[i2[f]] + vz[i3[f]];
		}
		int n = faces.length * 3;
		return new float[]{x / n, y / n, z / n};
	}

	private static float[] blockCentroid(HelmetEyes.Location location, float[] vx, float[] vy, float[] vz, int[] i1, int[] i2, int[] i3)
	{
		float x = 0, y = 0, z = 0;
		int start = location.getOffset();
		int count = location.getVariant().getFaceCount();
		for (int f = start; f < start + count; f++)
		{
			x += vx[i1[f]];
			y += vy[i1[f]];
			z += vz[i1[f]];
		}
		return new float[]{x / count, y / count, z / count};
	}
}
