package phantom.art;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

/**
 * Higher-level drawing helpers built on {@link Canvas}: organic blobs, tendrils, spikes and
 * crystals. The painters combine these into creatures and scenery.
 * <p>OOP concept: a stateless helper class (no global state, only pure functions).</p>
 */
public final class ProceduralArt {
    private ProceduralArt() { }

    public static Color rgb(float r, float g, float b) {
        return new Color(r, g, b, 1f);
    }

    public static Color rgba(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }

    /** Linear blend of two colours. */
    public static Color mix(Color a, Color b, float t) {
        return new Color(a).lerp(b, t);
    }

    public static float[] pts(float... values) {
        return values;
    }

    /** An irregular, organic ellipse (rocks, shells, moss clumps). */
    public static void blob(Canvas c, float cx, float cy, float rx, float ry, float wobble, long seed, Color color) {
        Random rnd = new Random(seed);
        float p1 = rnd.nextFloat() * 6.28f;
        float p2 = rnd.nextFloat() * 6.28f;
        int n = 40;
        float[] poly = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = i / (double) n * Math.PI * 2;
            float k = 1f + wobble * (float) (Math.sin(a * 3 + p1) * 0.6 + Math.sin(a * 7 + p2) * 0.4);
            poly[i * 2] = cx + (float) Math.cos(a) * rx * k;
            poly[i * 2 + 1] = cy + (float) Math.sin(a) * ry * k;
        }
        c.fillPolygon(poly, color);
    }

    /** A tapered curved stroke bent sideways by {@code bend} (relative to its length). */
    public static void tendril(Canvas c, float x0, float y0, float x1, float y1, float bend, float w0, float w1, Color color) {
        float mx = (x0 + x1) / 2f;
        float my = (y0 + y1) / 2f;
        float dx = x1 - x0;
        float dy = y1 - y0;
        c.curve(x0, y0, mx - dy * bend, my + dx * bend, x1, y1, w0, w1, color);
    }

    /** A pointed spike with its base centred at (x, y) pointing in direction {@code angleDeg} (0 = up). */
    public static void spike(Canvas c, float x, float y, float baseWidth, float length, float angleDeg, Color color) {
        double a = Math.toRadians(angleDeg);
        float tx = x + (float) Math.sin(a) * length;
        float ty = y - (float) Math.cos(a) * length;
        float px = (float) Math.cos(a) * baseWidth / 2f;
        float py = (float) Math.sin(a) * baseWidth / 2f;
        c.fillPolygon(pts(x - px, y - py, tx, ty, x + px, y + py), color);
    }

    /** A four-sided crystal with a lit face. */
    public static void crystal(Canvas c, float x, float y, float w, float h, float tilt, Color dark, Color light) {
        float tx = x + tilt;
        c.fillPolygon(pts(x - w / 2, y - h * 0.3f, tx, y - h, x + w / 2, y - h * 0.3f, x, y), dark);
        c.fillPolygon(pts(x - w / 2, y - h * 0.3f, tx, y - h, x, y), light);
    }

    /** Scatter of soft glowing dots (spores, motes, distant lamps). */
    public static void motes(Canvas c, int count, float x, float y, float w, float h, float minR, float maxR,
                             Color color, long seed) {
        Random rnd = new Random(seed);
        for (int i = 0; i < count; i++) {
            float r = minR + rnd.nextFloat() * (maxR - minR);
            c.radial(x + rnd.nextFloat() * w, y + rnd.nextFloat() * h, r, color, 1.6f, true);
        }
    }

    /** A gear outline with teeth. */
    public static void gear(Canvas c, float cx, float cy, float radius, int teeth, float toothDepth, Color color) {
        int n = teeth * 4;
        float[] poly = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = i / (double) n * Math.PI * 2;
            float r = (i % 4 == 1 || i % 4 == 2) ? radius : radius - toothDepth;
            poly[i * 2] = cx + (float) Math.cos(a) * r;
            poly[i * 2 + 1] = cy + (float) Math.sin(a) * r;
        }
        c.fillPolygon(poly, color);
    }

    /** Draws the shape three times (x - period, x, x + period) so a background tiles seamlessly. */
    public static void wrapped(float period, java.util.function.DoubleConsumer drawAtOffset) {
        drawAtOffset.accept(-period);
        drawAtOffset.accept(0);
        drawAtOffset.accept(period);
    }
}
