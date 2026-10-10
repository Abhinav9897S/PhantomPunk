package phantom.art;

import com.badlogic.gdx.graphics.Color;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints the four NPCs: Ossian the Lamplighter, Quill the Moth Merchant,
 * Seraphine the Drowned Cartographer and Old Vesper.
 * <p>OOP concept: implements {@link SpritePainter}.</p>
 */
public final class NpcPainter implements SpritePainter {
    private static final Color INK = rgb(0.01f, 0.01f, 0.015f);

    @Override
    public void paint(SpriteRegistry reg) {
        reg.sprite("npc_ossian", 2, false, ossian(reg));
        reg.sprite("npc_quill", 2, false, quill(reg));
        reg.sprite("npc_seraphine", 2, false, seraphine(reg));
        reg.sprite("npc_vesper", 2, false, vesper(reg));
    }

    private Canvas ossian(SpriteRegistry reg) {
        Canvas c = reg.canvas(64, 112);
        float cx = 32;
        Color coat = rgb(0.07f, 0.075f, 0.08f);
        Color hat = rgb(0.05f, 0.05f, 0.055f);
        c.stroke(cx + 12, 110, cx + 22, 20, 2.2f, 1.6f, rgb(0.25f, 0.18f, 0.1f));
        c.stroke(cx + 22, 20, cx + 22, 30, 1f, 1f, rgb(0.3f, 0.3f, 0.3f));
        c.fillEllipse(cx + 22, 26, 3.6f, 4.6f, rgb(0.2f, 0.18f, 0.15f));
        c.fillEllipse(cx + 22, 26, 2.2f, 3f, rgb(1f, 0.85f, 0.55f));
        c.fillPolygon(pts(cx - 8, 52, cx - 14, 104, cx - 6, 110, cx + 2, 104, cx + 8, 110, cx + 14, 102, cx + 9, 52), coat);
        c.stroke(cx + 6, 66, cx + 13, 84, 4, 3, coat);
        c.fillEllipse(cx + 1, 56, 9, 10, coat);
        c.fillEllipse(cx + 3, 58, 5, 5.5f, INK);
        c.fillEllipse(cx, 45, 19, 4, hat);
        c.fillPolygon(pts(cx - 8, 46, cx - 5, 32, cx + 6, 30, cx + 9, 46), hat);
        ProceduralArt.tendril(c, cx - 2, 70, cx - 16, 92, 0.2f, 4, 1, rgb(0.55f, 0.5f, 0.42f));
        c.rimLight(-0.7f, -1f, rgba(1f, 0.85f, 0.6f, 1f), 0.6f, 1f);
        return c;
    }

    private Canvas quill(SpriteRegistry reg) {
        Canvas c = reg.canvas(80, 72);
        Color fur = rgb(0.46f, 0.42f, 0.38f);
        Color furDark = rgb(0.25f, 0.22f, 0.21f);
        Color wing = rgb(0.2f, 0.17f, 0.22f);
        Color pack = rgb(0.2f, 0.13f, 0.08f);
        c.fillPolygon(pts(8, 18, 32, 14, 34, 60, 6, 62), pack);
        c.stroke(10, 30, 30, 28, 1.4f, 1.4f, rgb(0.35f, 0.25f, 0.15f));
        c.fillCircle(12, 20, 4, rgb(0.4f, 0.4f, 0.45f));
        c.stroke(18, 62, 18, 48, 1.2f, 1.2f, rgb(0.3f, 0.3f, 0.3f));
        c.fillEllipse(20, 36, 3.5f, 4, rgb(0.2f, 0.15f, 0.1f));
        // folded wings like a shawl
        c.fillPolygon(pts(30, 30, 60, 30, 66, 64, 44, 68, 26, 62), wing);
        c.stroke(44, 32, 46, 66, 1f, 1f, rgb(0.32f, 0.27f, 0.34f));
        ProceduralArt.blob(c, 46, 46, 15, 16, 0.18f, 7, furDark);
        ProceduralArt.blob(c, 49, 34, 13, 8, 0.3f, 8, fur);
        c.fillCircle(52, 27, 9, fur);
        c.fillEllipse(55, 28, 5.5f, 6, INK);
        for (int side = -1; side <= 1; side += 2) {
            float bx = 52 + side * 3;
            float tx = 52 + side * 14;
            ProceduralArt.tendril(c, bx, 19, tx, 4, side * 0.25f, 1.6f, 0.6f, furDark);
            for (int k = 1; k <= 4; k++) {
                float t = k / 5f;
                float px = bx + (tx - bx) * t;
                float py = 19 + (4 - 19) * t;
                c.stroke(px, py, px + side * 4, py + 2, 0.9f, 0.4f, furDark);
            }
        }
        c.stroke(40, 64, 38, 70, 3, 3, INK);
        c.stroke(52, 64, 54, 70, 3, 3, INK);
        c.rimLight(-0.7f, -1f, rgba(1f, 0.85f, 0.6f, 1f), 0.5f, 1f);
        return c;
    }

    private Canvas seraphine(SpriteRegistry reg) {
        Canvas c = reg.canvas(64, 80);
        float cx = 32;
        Color robe = rgb(0.14f, 0.17f, 0.22f);
        Color robeDark = rgb(0.07f, 0.08f, 0.11f);
        c.fillPolygon(pts(cx - 18, 78, cx - 12, 44, cx + 8, 40, cx + 20, 78), robe);
        c.fillPolygon(pts(cx - 4, 50, cx + 18, 60, cx + 18, 66, cx - 6, 58), rgb(0.78f, 0.72f, 0.58f));
        c.fillEllipse(cx, 32, 10, 11, robe);
        c.fillEllipse(cx + 3, 34, 5.5f, 6, INK);
        ProceduralArt.tendril(c, cx - 4, 24, cx - 16, 16, -0.2f, 6, 1, robe);
        c.stroke(cx - 10, 60, cx + 10, 74, 1.2f, 1.2f, robeDark);
        c.rimLight(-0.7f, -1f, rgba(0.7f, 0.85f, 1f, 1f), 0.6f, 1f);
        return c;
    }

    private Canvas vesper(SpriteRegistry reg) {
        Canvas c = reg.canvas(64, 104);
        float cx = 32;
        Color robe = rgb(0.09f, 0.08f, 0.1f);
        c.fillPolygon(pts(cx - 16, 102, cx - 12, 56, cx + 10, 50, cx + 18, 102), robe);
        c.stroke(cx + 10, 62, cx + 16, 102, 2.2f, 2f, rgb(0.22f, 0.17f, 0.1f));
        c.fillEllipse(cx + 2, 50, 10, 10, robe);
        c.fillEllipse(cx + 4, 52, 5, 5.5f, INK);
        for (int i = -2; i <= 2; i++) {
            float x = cx + 2 + i * 5;
            c.fillRect(x - 1.2f, 30 - Math.abs(i) * -2, 2.4f, 10 - Math.abs(i) * 2, rgb(0.85f, 0.82f, 0.74f));
        }
        c.fillEllipse(cx + 2, 41, 13, 3, robe);
        c.rimLight(-0.7f, -1f, rgba(1f, 0.85f, 0.6f, 1f), 0.6f, 1f);
        return c;
    }
}
