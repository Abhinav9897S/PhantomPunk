package phantom.art;

import java.util.Arrays;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * A small software rasteriser used to paint every sprite and background at startup.
 * Shapes are drawn in "logical" units (world pixels, y pointing down) onto a supersampled
 * floating-point buffer, then {@link #downsample(int)} produces smooth anti-aliased edges.
 * Pure Java except {@link #toPixmap()}, so it can be unit-tested without a GPU.
 * <p>OOP concept: <b>encapsulation</b> of pixel buffers behind drawing operations.</p>
 */
public final class Canvas {
    private final int width;
    private final int height;
    private final float scale;
    private final float[] r;
    private final float[] g;
    private final float[] b;
    private final float[] a;

    /** @param scale buffer pixels per logical unit */
    public Canvas(float logicalWidth, float logicalHeight, float scale) {
        this((int) Math.ceil(logicalWidth * scale), (int) Math.ceil(logicalHeight * scale), scale);
    }

    private Canvas(int width, int height, float scale) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.scale = scale;
        int n = this.width * this.height;
        r = new float[n];
        g = new float[n];
        b = new float[n];
        a = new float[n];
    }

    // ------------------------------------------------------------------ pixel ops

    private void blend(int i, float cr, float cg, float cb, float ca) {
        if (ca <= 0f) return;
        float da = a[i];
        float oa = ca + da * (1f - ca);
        if (oa <= 0f) return;
        float k = da * (1f - ca);
        r[i] = (cr * ca + r[i] * k) / oa;
        g[i] = (cg * ca + g[i] * k) / oa;
        b[i] = (cb * ca + b[i] * k) / oa;
        a[i] = oa;
    }

    private void addLight(int i, float cr, float cg, float cb, float ca) {
        if (ca <= 0f) return;
        float da = a[i];
        float oa = Math.min(1f, da + ca);
        // accumulate in premultiplied space, then convert back
        float pr = Math.min(1f, r[i] * da + cr * ca);
        float pg = Math.min(1f, g[i] * da + cg * ca);
        float pb = Math.min(1f, b[i] * da + cb * ca);
        r[i] = pr / oa;
        g[i] = pg / oa;
        b[i] = pb / oa;
        a[i] = oa;
    }

    private void plot(int x, int y, Color c, float alpha, boolean additive) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        int i = y * width + x;
        if (additive) addLight(i, c.r, c.g, c.b, c.a * alpha);
        else blend(i, c.r, c.g, c.b, c.a * alpha);
    }

    // ------------------------------------------------------------------ shapes

    /** Fills a polygon given as x0,y0,x1,y1,... in logical units (even-odd rule). */
    public void fillPolygon(float[] pts, Color c) {
        fillPolygon(pts, c, 1f);
    }

    public void fillPolygon(float[] pts, Color c, float alpha) {
        int n = pts.length / 2;
        if (n < 3) return;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            minY = Math.min(minY, pts[i * 2 + 1] * scale);
            maxY = Math.max(maxY, pts[i * 2 + 1] * scale);
        }
        int y0 = Math.max(0, (int) Math.floor(minY));
        int y1 = Math.min(height - 1, (int) Math.ceil(maxY));
        float[] xs = new float[n];
        for (int y = y0; y <= y1; y++) {
            float yc = y + 0.5f;
            int count = 0;
            for (int i = 0; i < n; i++) {
                float ax = pts[i * 2] * scale;
                float ay = pts[i * 2 + 1] * scale;
                float bx = pts[((i + 1) % n) * 2] * scale;
                float by = pts[((i + 1) % n) * 2 + 1] * scale;
                if ((ay <= yc && by > yc) || (by <= yc && ay > yc)) {
                    xs[count++] = ax + (yc - ay) / (by - ay) * (bx - ax);
                }
            }
            Arrays.sort(xs, 0, count);
            for (int k = 0; k + 1 < count; k += 2) {
                int xa = Math.max(0, (int) Math.ceil(xs[k] - 0.5f));
                int xb = Math.min(width - 1, (int) Math.floor(xs[k + 1] - 0.5f));
                for (int x = xa; x <= xb; x++) plot(x, y, c, alpha, false);
            }
        }
    }

    public void fillEllipse(float cx, float cy, float rx, float ry, Color c) {
        fillEllipse(cx, cy, rx, ry, c, 1f);
    }

    public void fillEllipse(float cx, float cy, float rx, float ry, Color c, float alpha) {
        float pcx = cx * scale;
        float pcy = cy * scale;
        float prx = Math.max(0.5f, rx * scale);
        float pry = Math.max(0.5f, ry * scale);
        int x0 = Math.max(0, (int) (pcx - prx - 1));
        int x1 = Math.min(width - 1, (int) (pcx + prx + 1));
        int y0 = Math.max(0, (int) (pcy - pry - 1));
        int y1 = Math.min(height - 1, (int) (pcy + pry + 1));
        for (int y = y0; y <= y1; y++) {
            float dy = (y + 0.5f - pcy) / pry;
            for (int x = x0; x <= x1; x++) {
                float dx = (x + 0.5f - pcx) / prx;
                if (dx * dx + dy * dy <= 1f) plot(x, y, c, alpha, false);
            }
        }
    }

    public void fillCircle(float cx, float cy, float radius, Color c) {
        fillEllipse(cx, cy, radius, radius, c, 1f);
    }

    public void fillRect(float x, float y, float w, float h, Color c) {
        fillRect(x, y, w, h, c, 1f);
    }

    public void fillRect(float x, float y, float w, float h, Color c, float alpha) {
        int x0 = Math.max(0, Math.round(x * scale));
        int y0 = Math.max(0, Math.round(y * scale));
        int x1 = Math.min(width, Math.round((x + w) * scale));
        int y1 = Math.min(height, Math.round((y + h) * scale));
        for (int py = y0; py < y1; py++) {
            for (int px = x0; px < x1; px++) plot(px, py, c, alpha, false);
        }
    }

    /** Vertical gradient from {@code top} to {@code bottom}. */
    public void gradientRect(float x, float y, float w, float h, Color top, Color bottom) {
        int x0 = Math.max(0, Math.round(x * scale));
        int y0 = Math.max(0, Math.round(y * scale));
        int x1 = Math.min(width, Math.round((x + w) * scale));
        int y1 = Math.min(height, Math.round((y + h) * scale));
        Color tmp = new Color();
        for (int py = y0; py < y1; py++) {
            float t = (py - y0) / (float) Math.max(1, y1 - y0 - 1);
            tmp.set(top).lerp(bottom, t);
            for (int px = x0; px < x1; px++) plot(px, py, tmp, 1f, false);
        }
    }

    /** Soft radial glow; {@code power} > 1 makes the falloff tighter. */
    public void radial(float cx, float cy, float radius, Color c, float power, boolean additive) {
        float pcx = cx * scale;
        float pcy = cy * scale;
        float pr = radius * scale;
        int x0 = Math.max(0, (int) (pcx - pr));
        int x1 = Math.min(width - 1, (int) (pcx + pr));
        int y0 = Math.max(0, (int) (pcy - pr));
        int y1 = Math.min(height - 1, (int) (pcy + pr));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = x + 0.5f - pcx;
                float dy = y + 0.5f - pcy;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / pr;
                if (d >= 1f) continue;
                float k = (float) Math.pow(1f - d, power);
                plot(x, y, c, k, additive);
            }
        }
    }

    /** A thick line with different widths at each end, with round caps. */
    public void stroke(float x0, float y0, float x1, float y1, float w0, float w1, Color c) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) {
            fillCircle(x0, y0, Math.max(w0, w1) / 2f, c);
            return;
        }
        float nx = -dy / len;
        float ny = dx / len;
        fillPolygon(new float[] {
            x0 + nx * w0 / 2, y0 + ny * w0 / 2, x1 + nx * w1 / 2, y1 + ny * w1 / 2,
            x1 - nx * w1 / 2, y1 - ny * w1 / 2, x0 - nx * w0 / 2, y0 - ny * w0 / 2}, c);
        if (w0 > 0.3f) fillCircle(x0, y0, w0 / 2f, c);
        if (w1 > 0.3f) fillCircle(x1, y1, w1 / 2f, c);
    }

    /** Tapered quadratic Bezier stroke: roots, tendrils, antennae, wing veins. */
    public void curve(float x0, float y0, float cx, float cy, float x1, float y1, float w0, float w1, Color c) {
        int segments = 14;
        float px = x0;
        float py = y0;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float u = 1f - t;
            float qx = u * u * x0 + 2 * u * t * cx + t * t * x1;
            float qy = u * u * y0 + 2 * u * t * cy + t * t * y1;
            float wa = w0 + (w1 - w0) * ((i - 1) / (float) segments);
            float wb = w0 + (w1 - w0) * t;
            stroke(px, py, qx, qy, wa, wb, c);
            px = qx;
            py = qy;
        }
    }

    /** Makes everything inside the ellipse transparent (eye sockets, holes). */
    public void cut(float cx, float cy, float rx, float ry) {
        float pcx = cx * scale;
        float pcy = cy * scale;
        float prx = rx * scale;
        float pry = ry * scale;
        for (int y = Math.max(0, (int) (pcy - pry)); y <= Math.min(height - 1, (int) (pcy + pry)); y++) {
            for (int x = Math.max(0, (int) (pcx - prx)); x <= Math.min(width - 1, (int) (pcx + prx)); x++) {
                float dx = (x + 0.5f - pcx) / prx;
                float dy = (y + 0.5f - pcy) / pry;
                if (dx * dx + dy * dy <= 1f) a[y * width + x] = 0f;
            }
        }
    }

    // ------------------------------------------------------------------ whole-image effects

    /**
     * Adds a rim light: opaque pixels whose neighbour in direction (dx, dy) is transparent
     * get tinted, as if lit from that side.
     */
    public void rimLight(float dx, float dy, Color rim, float strength, float thickness) {
        int ox = Math.round(dx * scale * thickness);
        int oy = Math.round(dy * scale * thickness);
        float[] mask = new float[a.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (a[i] < 0.5f) continue;
                int nx = x + ox;
                int ny = y + oy;
                float na = (nx < 0 || ny < 0 || nx >= width || ny >= height) ? 0f : a[ny * width + nx];
                if (na < 0.5f) mask[i] = strength;
            }
        }
        for (int i = 0; i < a.length; i++) {
            if (mask[i] > 0) {
                float k = mask[i] * rim.a;
                r[i] += (rim.r - r[i]) * k;
                g[i] += (rim.g - g[i]) * k;
                b[i] += (rim.b - b[i]) * k;
            }
        }
    }

    /** Random brightness variation on opaque pixels, for a painterly grain. */
    public void grain(float amount, long seed) {
        Random rnd = new Random(seed);
        for (int i = 0; i < a.length; i++) {
            if (a[i] <= 0f) continue;
            float n = (rnd.nextFloat() - 0.5f) * amount;
            r[i] = clamp01(r[i] + n);
            g[i] = clamp01(g[i] + n);
            b[i] = clamp01(b[i] + n);
        }
    }

    /** Multiplies the alpha of every pixel (used for translucent wings, mist). */
    public void fadeAlpha(float factor) {
        for (int i = 0; i < a.length; i++) a[i] *= factor;
    }

    // package-private access for CanvasOps (export and analysis helpers)
    float[] red() { return r; }
    float[] green() { return g; }
    float[] blue() { return b; }
    float[] alpha() { return a; }

    static Canvas blank(int width, int height, float scale) {
        return new Canvas(width, height, scale);
    }

    /** See {@link CanvasOps#downsample(Canvas, int)}. */
    public Canvas downsample(int factor) { return CanvasOps.downsample(this, factor); }
    /** See {@link CanvasOps#silhouette(Canvas)}. */
    public Canvas silhouette() { return CanvasOps.silhouette(this); }
    /** See {@link CanvasOps#toPixmap(Canvas)}. */
    public Pixmap toPixmap() { return CanvasOps.toPixmap(this); }
    /** See {@link CanvasOps#toArgb(Canvas)}. */
    public int[] toArgb() { return CanvasOps.toArgb(this); }
    public float alphaAt(int x, int y) { return a[y * width + x]; }
    public float coverage() { return CanvasOps.coverage(this); }
    public float maxAlpha() { return CanvasOps.maxAlpha(this); }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public int getPixelWidth() { return width; }
    public int getPixelHeight() { return height; }
    public float getScale() { return scale; }
}
