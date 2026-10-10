package phantom.art;

import com.badlogic.gdx.graphics.Color;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints the four regular enemies: Mosscrawler, Duskmoth, Lantern Sentinel and Husk Brute.
 * All face right; the renderer mirrors them.
 * <p>OOP concept: implements {@link SpritePainter}; one private method per creature.</p>
 */
public final class CreaturePainter implements SpritePainter {
    private static final Color SHELL = rgb(0.07f, 0.09f, 0.085f);
    private static final Color SHELL_LIGHT = rgb(0.13f, 0.19f, 0.16f);
    private static final Color MOSS = rgb(0.17f, 0.36f, 0.25f);
    private static final Color INK = rgb(0.02f, 0.02f, 0.03f);
    private static final Color RIM = rgba(0.6f, 0.75f, 0.8f, 1f);

    @Override
    public void paint(SpriteRegistry reg) {
        reg.sprite("crawler", 3, true, crawler(reg, 0), crawler(reg, 1), crawler(reg, 2));
        reg.sprite("flutterer", 13, true, moth(reg, -55), moth(reg, -20), moth(reg, 20), moth(reg, -20));
        reg.sprite("sentinel", 3, true, sentinel(reg, false), sentinel(reg, true));
        reg.sprite("brute", 3, true, brute(reg, 0, false), brute(reg, 1, false), brute(reg, 2, false),
                brute(reg, 3, false), brute(reg, 0, true), brute(reg, 2, true));
    }

    private Canvas crawler(SpriteRegistry reg, int f) {
        Canvas c = reg.canvas(48, 32);
        float foot = 29;
        for (int i = 0; i < 4; i++) {
            float x = 13 + i * 7;
            float swing = ((i + f) % 3 - 1) * 2.5f;
            c.stroke(x, 22, x + swing - 1, foot, 2.2f, 1.6f, INK);
        }
        ProceduralArt.blob(c, 23, 19, 17, 10, 0.06f, 11 + f, SHELL);
        c.fillEllipse(37, 22, 7, 5.5f, INK);
        for (int i = 0; i < 3; i++) {
            ProceduralArt.tendril(c, 12 + i * 8, 23, 16 + i * 8, 10.5f, 0.25f, 2.2f, 1.4f, SHELL_LIGHT);
        }
        ProceduralArt.blob(c, 20, 10.5f, 9, 3.5f, 0.3f, 3, MOSS);
        ProceduralArt.blob(c, 29, 11, 5, 2.8f, 0.3f, 5, MOSS);
        ProceduralArt.tendril(c, 41, 19, 46, 13 + f, 0.3f, 1.2f, 0.5f, INK);
        c.fillEllipse(39.5f, 21, 2.6f, 2.2f, rgb(0.01f, 0.01f, 0.01f));
        c.rimLight(-0.6f, -1f, RIM, 0.7f, 1f);
        return c;
    }

    private Canvas moth(SpriteRegistry reg, float wingAngle) {
        Canvas c = reg.canvas(56, 48);
        float cx = 28;
        float cy = 24;
        Color membrane = rgba(0.16f, 0.12f, 0.24f, 0.82f);
        Color vein = rgb(0.06f, 0.04f, 0.09f);
        double a = Math.toRadians(wingAngle);
        float tipX = (float) Math.cos(a) * 25f;
        float tipY = (float) Math.sin(a) * 25f;
        // back wing (left), then front wing (right) so the body overlaps both
        c.fillPolygon(pts(cx - 2, cy - 2, cx - 6 - tipX * 0.9f, cy + tipY - 4, cx - 14 - tipX * 0.5f, cy + tipY * 0.4f + 6, cx - 3, cy + 5), membrane);
        c.fillPolygon(pts(cx + 1, cy - 2, cx + 6 + tipX, cy + tipY - 6, cx + 15 + tipX * 0.5f, cy + tipY * 0.4f + 5, cx + 2, cy + 5), membrane);
        c.stroke(cx, cy, cx + 6 + tipX * 0.9f, cy + tipY - 5, 1.2f, 0.4f, vein);
        c.stroke(cx, cy, cx - 6 - tipX * 0.8f, cy + tipY - 3, 1.2f, 0.4f, vein);
        c.fillPolygon(pts(cx - 1, cy + 3, cx - 9, cy + 13, cx - 3, cy + 10), membrane);
        c.fillPolygon(pts(cx + 1, cy + 3, cx + 10, cy + 12, cx + 4, cy + 10), membrane);
        c.fillEllipse(cx, cy + 3, 4.5f, 8.5f, INK);
        c.fillEllipse(cx + 2, cy - 5, 5f, 4.5f, INK);
        ProceduralArt.tendril(c, cx + 2, cy - 8, cx - 4, cy - 17, 0.3f, 1.2f, 0.4f, INK);
        ProceduralArt.tendril(c, cx + 4, cy - 8, cx + 10, cy - 17, -0.3f, 1.2f, 0.4f, INK);
        c.rimLight(-0.5f, -1f, rgba(0.85f, 0.75f, 1f, 1f), 0.6f, 1f);
        return c;
    }

    private Canvas sentinel(SpriteRegistry reg, boolean charging) {
        Canvas c = reg.canvas(48, 64);
        Color stone = rgb(0.11f, 0.11f, 0.13f);
        Color stoneDark = rgb(0.06f, 0.06f, 0.075f);
        Color rune = charging ? rgb(1f, 0.6f, 0.3f) : rgb(0.42f, 0.25f, 0.14f);
        c.fillPolygon(pts(11, 61, 37, 61, 34, 52, 14, 52), stoneDark);
        c.fillPolygon(pts(15, 52, 33, 52, 31, 22, 17, 22), stone);
        c.fillPolygon(pts(6, 20, 24, 6, 42, 20, 36, 22, 12, 22), stoneDark);
        c.fillPolygon(pts(4, 21, 7, 17, 11, 21), stoneDark);
        c.fillPolygon(pts(37, 21, 41, 17, 44, 21), stoneDark);
        c.fillCircle(24, 6, 2.5f, stone);
        c.fillEllipse(24.5f, 26, 5f, 5.5f, rgb(0.015f, 0.01f, 0.01f));
        c.stroke(19, 36, 29, 36, 1.2f, 1.2f, rune);
        c.stroke(24, 32, 24, 47, 1.2f, 1.2f, rune);
        c.stroke(20, 42, 28, 45, 1f, 1f, rune);
        c.grain(0.05f, 77);
        c.rimLight(-0.7f, -1f, RIM, 0.6f, 1f);
        return c;
    }

    private Canvas brute(SpriteRegistry reg, int f, boolean charging) {
        Canvas c = reg.canvas(96, 80);
        float foot = 77;
        double ph = f / 4.0 * Math.PI * 2;
        float legA = (float) Math.sin(ph) * 5;
        float legB = -legA;
        float headY = charging ? 60 : 50;
        float headX = charging ? 76 : 71;
        Color hide = rgb(0.08f, 0.07f, 0.075f);
        Color plate = rgb(0.14f, 0.12f, 0.12f);
        c.stroke(30, 58, 25 + legB, foot, 9, 7, INK);
        c.stroke(44, 60, 42 + legA, foot, 8, 7, INK);
        ProceduralArt.blob(c, 44, charging ? 48 : 45, 32, 23, 0.05f, 21, hide);
        for (int i = 0; i < 4; i++) {
            float sx = 24 + i * 11;
            ProceduralArt.spike(c, sx, 26 + Math.abs(i - 1.5f) * 2.5f + (charging ? 3 : 0), 9, 12 - Math.abs(i - 1.5f) * 2, -15 + i * 8, plate);
        }
        for (int i = 0; i < 3; i++) {
            ProceduralArt.tendril(c, 22 + i * 13, 40, 30 + i * 13, 64, 0.15f, 2.5f, 1f, plate);
        }
        c.stroke(64, 54, 72 + legA * 0.6f, foot, 10, 8, INK);
        c.stroke(54, 58, 58 + legB * 0.6f, foot, 9, 7, hide);
        c.fillEllipse(headX, headY, 13, 11, hide);
        ProceduralArt.tendril(c, headX - 4, headY - 8, headX - 14, headY - 22, -0.35f, 5, 1, plate);
        ProceduralArt.tendril(c, headX + 5, headY - 8, headX + 14, headY - 20, 0.35f, 5, 1, plate);
        c.fillEllipse(headX + 4, headY + 3, 5, 3.5f, rgb(0.01f, 0.01f, 0.01f));
        c.grain(0.04f, 5 + f);
        c.rimLight(-0.7f, -1f, rgba(0.85f, 0.7f, 0.65f, 1f), 0.6f, 1.2f);
        return c;
    }
}
