package phantom.art;

import com.badlogic.gdx.graphics.Color;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints HUD art (flame masks, soul orb, shard icon) and the shared textures used by the
 * lighting system: a soft radial light, a screen vignette and a 1-pixel white texture.
 * <p>OOP concept: implements {@link SpritePainter}.</p>
 */
public final class UiPainter implements SpritePainter {
    @Override
    public void paint(SpriteRegistry reg) {
        reg.sprite("mask_full", 0, false, flame(reg, true));
        reg.sprite("mask_empty", 0, false, flame(reg, false));
        reg.sprite("soul_orb", 0, false, orbFrame(reg));
        reg.sprite("soul_fill", 0, false, orbFill(reg));
        reg.sprite("shard_icon", 0, false, shardIcon(reg));
        reg.sprite("pixel", 0, false, pixel(reg));

        Canvas soft = reg.canvas(128, 128, 1f);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                float dx = (x + 0.5f - 64) / 64f;
                float dy = (y + 0.5f - 64) / 64f;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 1f) {
                    float k = (1f - d) * (1f - d);
                    soft.fillRect(x, y, 1, 1, rgba(1, 1, 1, k));
                }
            }
        }
        reg.texture("soft", soft, 1, false);

        Canvas vignette = reg.canvas(256, 144, 1f);
        for (int y = 0; y < 144; y++) {
            for (int x = 0; x < 256; x++) {
                float dx = (x + 0.5f - 128) / 128f;
                float dy = (y + 0.5f - 72) / 72f;
                float d = (float) Math.sqrt(dx * dx * 0.8f + dy * dy * 0.9f);
                float k = Math.max(0f, Math.min(1f, (d - 0.55f) / 0.6f));
                vignette.fillRect(x, y, 1, 1, rgba(0, 0, 0, k * k * 0.9f));
            }
        }
        reg.texture("vignette", vignette, 1, false);
    }

    private Canvas flame(SpriteRegistry reg, boolean full) {
        Canvas c = reg.canvas(28, 36);
        float[] shape = pts(14, 1, 19, 9, 24, 17, 25, 25, 21, 32, 14, 35, 7, 32, 3, 25, 4, 17, 9, 9);
        if (full) {
            c.fillPolygon(shape, rgb(0.9f, 0.88f, 0.82f));
            c.fillPolygon(pts(14, 12, 18, 20, 19, 27, 14, 31, 9, 27, 10, 20), rgb(1f, 0.82f, 0.5f));
            c.fillEllipse(11, 22, 1.6f, 2.6f, rgb(0.08f, 0.07f, 0.06f));
            c.fillEllipse(17, 22, 1.6f, 2.6f, rgb(0.08f, 0.07f, 0.06f));
        } else {
            c.fillPolygon(shape, rgba(0.12f, 0.12f, 0.16f, 0.85f));
            c.fillPolygon(pts(14, 6, 19, 13, 22, 20, 22, 26, 18, 30, 14, 32, 10, 30, 6, 26, 6, 20, 9, 13), rgba(0.03f, 0.03f, 0.05f, 0.8f));
        }
        return c;
    }

    private Canvas orbFrame(SpriteRegistry reg) {
        Canvas c = reg.canvas(84, 84);
        Color stone = rgb(0.16f, 0.16f, 0.2f);
        c.fillCircle(42, 42, 40, stone);
        c.fillCircle(42, 42, 33, rgba(0.02f, 0.02f, 0.04f, 1f));
        c.cut(42, 42, 30, 30);
        for (int i = 0; i < 8; i++) {
            double a = i / 8.0 * Math.PI * 2;
            ProceduralArt.spike(c, 42 + (float) Math.cos(a) * 38, 42 + (float) Math.sin(a) * 38, 6, 6, (float) Math.toDegrees(a) + 90, stone);
        }
        c.rimLight(-0.7f, -1f, rgba(0.8f, 0.85f, 0.95f, 1f), 0.6f, 1.2f);
        return c;
    }

    private Canvas orbFill(SpriteRegistry reg) {
        Canvas c = reg.canvas(60, 60);
        c.fillCircle(30, 30, 30, rgb(0.78f, 0.88f, 0.96f));
        c.radial(22, 20, 22, rgba(1f, 1f, 1f, 0.8f), 1.5f, false);
        return c;
    }

    private Canvas shardIcon(SpriteRegistry reg) {
        Canvas c = reg.canvas(18, 22);
        ProceduralArt.crystal(c, 9, 21, 12, 20, 0.5f, rgb(0.35f, 0.5f, 0.6f), rgb(0.85f, 0.97f, 1f));
        return c;
    }

    private Canvas pixel(SpriteRegistry reg) {
        Canvas c = reg.canvas(4, 4, 1f);
        c.fillRect(0, 0, 4, 4, rgb(1, 1, 1));
        return c;
    }
}
