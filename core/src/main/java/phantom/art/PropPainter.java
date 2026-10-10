package phantom.art;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints props, pickups, projectiles and effects: benches, shrines, relics, shards, rocks,
 * gears, the Light Bolt, slash arcs, cracks, region decorations and eruption pillars.
 * <p>OOP concept: implements {@link SpritePainter}.</p>
 */
public final class PropPainter implements SpritePainter {
    private static final Color INK = rgb(0.02f, 0.02f, 0.03f);
    private static final Color STONE = rgb(0.13f, 0.13f, 0.15f);
    private static final Color STONE_DARK = rgb(0.07f, 0.07f, 0.085f);
    private static final Color RIM = rgba(0.7f, 0.75f, 0.85f, 1f);

    @Override
    public void paint(SpriteRegistry reg) {
        reg.sprite("bench", 2, false, bench(reg));
        reg.sprite("shrine", 2, false, shrine(reg, false), shrine(reg, true));
        reg.sprite("relic", 0, false, relic(reg));
        reg.sprite("shard", 0, false, shard(reg));
        reg.sprite("cache", 2, false, cache(reg));
        reg.sprite("rock", 0, false, rock(reg));
        reg.sprite("gear", 12, false, gear(reg));
        reg.sprite("bolt", 10, false, bolt(reg));
        reg.sprite("slash", 40, false, slash(reg, 0.55f, 0.7f), slash(reg, 1f, 1f), slash(reg, 0.8f, 0.45f));
        reg.sprite("cracks", 0, false, cracks(reg, 1), cracks(reg, 2), cracks(reg, 3));
        reg.sprite("decor_whispering_roots", 0, false, fungus(reg));
        reg.sprite("decor_gilded_cistern", 0, false, brazier(reg));
        reg.sprite("decor_umbral_heart", 0, false, floatingCrystal(reg));
        reg.sprite("eruption_whispering_roots", 0, false, rootPillar(reg));
        reg.sprite("eruption_gilded_cistern", 0, false, steamPillar(reg));
        reg.sprite("eruption_umbral_heart", 0, false, mistPillar(reg));
    }

    private Canvas bench(SpriteRegistry reg) {
        Canvas c = reg.canvas(72, 40);
        c.fillPolygon(pts(12, 28, 20, 28, 19, 38, 11, 38), STONE_DARK);
        c.fillPolygon(pts(52, 28, 60, 28, 61, 38, 53, 38), STONE_DARK);
        c.fillPolygon(pts(6, 22, 66, 22, 64, 29, 8, 29), STONE);
        c.fillPolygon(pts(10, 22, 13, 8, 18, 6, 22, 22), STONE_DARK);
        c.stroke(20, 25, 52, 25, 0.8f, 0.8f, STONE_DARK);
        c.fillRect(57, 8, 2.4f, 14, rgb(0.82f, 0.78f, 0.7f));
        c.fillRect(55.5f, 21, 5.5f, 1.6f, STONE_DARK);
        c.grain(0.04f, 1);
        c.rimLight(-0.7f, -1f, RIM, 0.5f, 1f);
        return c;
    }

    private Canvas shrine(SpriteRegistry reg, boolean spent) {
        Canvas c = reg.canvas(64, 96);
        Color body = spent ? STONE_DARK : STONE;
        c.fillPolygon(pts(12, 94, 52, 94, 48, 82, 16, 82), STONE_DARK);
        c.fillPolygon(pts(22, 82, 42, 82, 40, 44, 24, 44), body);
        c.fillPolygon(pts(10, 36, 54, 36, 46, 46, 18, 46), body);
        for (int i = 0; i < 3; i++) c.stroke(27, 52 + i * 9, 37, 52 + i * 9, 1f, 1f, spent ? STONE : rgb(0.5f, 0.6f, 0.7f));
        ProceduralArt.spike(c, 14, 37, 5, 9, -25, body);
        ProceduralArt.spike(c, 50, 37, 5, 9, 25, body);
        if (spent) {
            ProceduralArt.tendril(c, 30, 40, 36, 70, 0.2f, 1.4f, 0.3f, INK);
        }
        c.grain(0.04f, 2);
        c.rimLight(-0.7f, -1f, RIM, 0.5f, 1f);
        return c;
    }

    private Canvas relic(SpriteRegistry reg) {
        Canvas c = reg.canvas(24, 28);
        ProceduralArt.crystal(c, 12, 27, 14, 26, 1, rgb(0.55f, 0.18f, 0.08f), rgb(1f, 0.55f, 0.25f));
        c.radial(12, 15, 7, rgba(1f, 0.85f, 0.5f, 0.9f), 1.5f, true);
        return c;
    }

    private Canvas shard(SpriteRegistry reg) {
        Canvas c = reg.canvas(12, 14);
        ProceduralArt.crystal(c, 6, 14, 8, 13, 0.5f, rgb(0.35f, 0.5f, 0.6f), rgb(0.85f, 0.97f, 1f));
        return c;
    }

    private Canvas cache(SpriteRegistry reg) {
        Canvas c = reg.canvas(48, 48);
        Color dark = rgb(0.25f, 0.38f, 0.46f);
        Color light = rgb(0.75f, 0.93f, 1f);
        c.fillEllipse(24, 44, 20, 5, STONE_DARK);
        ProceduralArt.crystal(c, 14, 45, 12, 26, -3, dark, light);
        ProceduralArt.crystal(c, 34, 45, 12, 30, 3, dark, light);
        ProceduralArt.crystal(c, 24, 46, 16, 42, 0, dark, light);
        return c;
    }

    private Canvas rock(SpriteRegistry reg) {
        Canvas c = reg.canvas(24, 32);
        c.fillPolygon(pts(1, 0, 23, 0, 19, 12, 14, 24, 12, 32, 9, 22, 4, 11), STONE);
        c.fillPolygon(pts(12, 2, 23, 0, 19, 12, 14, 24, 12, 32), STONE_DARK);
        c.grain(0.05f, 3);
        return c;
    }

    private Canvas gear(SpriteRegistry reg) {
        Canvas c = reg.canvas(24, 24);
        ProceduralArt.gear(c, 12, 12, 11.5f, 8, 3, rgb(0.75f, 0.55f, 0.25f));
        c.fillCircle(12, 12, 6, rgb(0.45f, 0.3f, 0.12f));
        c.cut(12, 12, 2.5f, 2.5f);
        return c;
    }

    private Canvas bolt(SpriteRegistry reg) {
        Canvas c = reg.canvas(48, 20);
        c.fillPolygon(pts(2, 10, 30, 4, 46, 10, 30, 16), rgba(0.65f, 0.85f, 1f, 0.55f));
        c.fillPolygon(pts(14, 10, 34, 7, 45, 10, 34, 13), rgba(0.95f, 0.98f, 1f, 1f));
        return c;
    }

    /** A crescent arc pointing right; {@code width} thickens it, {@code reach} lengthens it. */
    private Canvas slash(SpriteRegistry reg, float width, float reach) {
        Canvas c = reg.canvas(96, 80);
        int n = 24;
        float[] poly = new float[n * 4];
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            double a = Math.toRadians(-75 + 150 * t);
            float outer = 36 * (0.7f + 0.3f * reach);
            float thick = (float) (Math.sin(t * Math.PI) * 13 * width) + 0.5f;
            poly[i * 2] = 34 + (float) Math.cos(a) * outer;
            poly[i * 2 + 1] = 40 + (float) Math.sin(a) * outer;
            int j = (2 * n - 1 - i) * 2;
            poly[j] = 34 + (float) Math.cos(a) * (outer - thick);
            poly[j + 1] = 40 + (float) Math.sin(a) * (outer - thick);
        }
        c.fillPolygon(poly, rgba(0.95f, 0.97f, 1f, 0.95f));
        c.radial(60, 40, 24, rgba(1f, 0.9f, 0.7f, 0.35f), 1.5f, true);
        return c;
    }

    private Canvas cracks(SpriteRegistry reg, int seed) {
        Canvas c = reg.canvas(32, 32);
        Random rnd = new Random(seed * 13L);
        for (int i = 0; i < 4; i++) {
            float x = 6 + rnd.nextFloat() * 20;
            float y = 4 + rnd.nextFloat() * 24;
            ProceduralArt.tendril(c, x, y, x + (rnd.nextFloat() - 0.5f) * 18, y + (rnd.nextFloat() - 0.5f) * 18, 0.4f, 1.6f, 0.3f,
                    rgba(0.55f, 0.6f, 0.65f, 0.8f));
        }
        return c;
    }

    private Canvas fungus(SpriteRegistry reg) {
        Canvas c = reg.canvas(32, 40);
        Color stem = rgb(0.08f, 0.12f, 0.11f);
        Color cap = rgb(0.18f, 0.55f, 0.45f);
        Color spot = rgb(0.6f, 1f, 0.85f);
        float[][] caps = {{9, 22, 7}, {20, 14, 9}, {27, 28, 5}};
        for (float[] m : caps) {
            c.stroke(m[0], 40, m[0] + 1, m[1], 2.5f, 2f, stem);
            c.fillPolygon(pts(m[0] - m[2], m[1] + 2, m[0], m[1] - m[2] * 0.8f, m[0] + m[2], m[1] + 2), cap);
            c.fillCircle(m[0] - m[2] * 0.3f, m[1] - 1, 1.2f, spot);
            c.fillCircle(m[0] + m[2] * 0.35f, m[1], 1f, spot);
        }
        return c;
    }

    private Canvas brazier(SpriteRegistry reg) {
        Canvas c = reg.canvas(32, 40);
        Color brass = rgb(0.45f, 0.32f, 0.14f);
        c.stroke(10, 40, 14, 26, 2f, 2f, brass);
        c.stroke(22, 40, 18, 26, 2f, 2f, brass);
        c.stroke(16, 40, 16, 26, 2f, 2f, brass);
        c.fillPolygon(pts(5, 18, 27, 18, 23, 27, 9, 27), brass);
        c.fillRect(5, 17, 22, 2, rgb(0.8f, 0.6f, 0.3f));
        ProceduralArt.blob(c, 16, 15, 7, 3, 0.3f, 4, rgb(1f, 0.6f, 0.25f));
        return c;
    }

    private Canvas floatingCrystal(SpriteRegistry reg) {
        Canvas c = reg.canvas(32, 40);
        ProceduralArt.crystal(c, 16, 38, 12, 34, 2, rgb(0.35f, 0.28f, 0.5f), rgb(0.88f, 0.82f, 1f));
        ProceduralArt.crystal(c, 7, 30, 5, 12, -1, rgb(0.35f, 0.28f, 0.5f), rgb(0.88f, 0.82f, 1f));
        return c;
    }

    private Canvas rootPillar(SpriteRegistry reg) {
        Canvas c = reg.canvas(32, 96);
        Color root = rgb(0.1f, 0.12f, 0.08f);
        ProceduralArt.spike(c, 16, 96, 16, 92, 3, root);
        ProceduralArt.spike(c, 8, 96, 8, 52, -12, root);
        ProceduralArt.spike(c, 25, 96, 8, 60, 14, root);
        c.rimLight(-0.7f, -1f, rgba(0.55f, 1f, 0.7f, 1f), 0.9f, 1.2f);
        return c;
    }

    private Canvas steamPillar(SpriteRegistry reg) {
        Canvas c = reg.canvas(32, 96);
        Random rnd = new Random(8);
        for (int i = 0; i < 12; i++) {
            float y = 92 - i * 7.5f;
            c.radial(16 + (rnd.nextFloat() - 0.5f) * 8, y, 9 + i * 0.4f, rgba(1f, 0.9f, 0.75f, 0.55f), 1.2f, false);
        }
        c.fillPolygon(pts(10, 96, 22, 96, 19, 86, 13, 86), rgb(0.45f, 0.32f, 0.14f));
        return c;
    }

    private Canvas mistPillar(SpriteRegistry reg) {
        Canvas c = reg.canvas(32, 96);
        Color dark = rgba(0.06f, 0.03f, 0.1f, 0.95f);
        ProceduralArt.tendril(c, 16, 96, 14, 2, 0.1f, 18, 2, dark);
        ProceduralArt.tendril(c, 10, 96, 4, 40, -0.2f, 8, 1, dark);
        ProceduralArt.tendril(c, 22, 96, 28, 30, 0.2f, 8, 1, dark);
        c.rimLight(-0.7f, -1f, rgba(0.9f, 0.8f, 1f, 1f), 0.9f, 1.2f);
        return c;
    }
}
