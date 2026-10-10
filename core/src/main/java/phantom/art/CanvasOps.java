package phantom.art;

import java.util.Arrays;

import com.badlogic.gdx.graphics.Pixmap;

/**
 * Whole-image operations on a {@link Canvas}: downsampling (anti-aliasing), white silhouettes
 * for hit flashes, conversion to a libGDX {@link Pixmap} or packed ARGB, and coverage statistics.
 * <p>OOP concept: separates "drawing" from "exporting/analysing" so each class stays small.</p>
 */
final class CanvasOps {
    private CanvasOps() { }

    /** Box-filter downsample by an integer factor (premultiplied, so edges stay clean). */
    static Canvas downsample(Canvas src, int factor) {
        int width = src.getPixelWidth();
        Canvas out = Canvas.blank(width / factor, src.getPixelHeight() / factor, src.getScale() / factor);
        float[] r = src.red();
        float[] g = src.green();
        float[] b = src.blue();
        float[] a = src.alpha();
        float[] or = out.red();
        float[] og = out.green();
        float[] ob = out.blue();
        float[] oa = out.alpha();
        int ow = out.getPixelWidth();
        float inv = 1f / (factor * factor);
        for (int y = 0; y < out.getPixelHeight(); y++) {
            for (int x = 0; x < ow; x++) {
                float sr = 0;
                float sg = 0;
                float sb = 0;
                float sa = 0;
                for (int j = 0; j < factor; j++) {
                    int row = (y * factor + j) * width;
                    for (int k = 0; k < factor; k++) {
                        int i = row + x * factor + k;
                        sr += r[i] * a[i];
                        sg += g[i] * a[i];
                        sb += b[i] * a[i];
                        sa += a[i];
                    }
                }
                int o = y * ow + x;
                oa[o] = sa * inv;
                if (sa > 0) {
                    or[o] = sr / sa;
                    og[o] = sg / sa;
                    ob[o] = sb / sa;
                }
            }
        }
        return out;
    }

    /** White copy with the same alpha, used for hit flashes. */
    static Canvas silhouette(Canvas src) {
        Canvas out = Canvas.blank(src.getPixelWidth(), src.getPixelHeight(), src.getScale());
        Arrays.fill(out.red(), 1f);
        Arrays.fill(out.green(), 1f);
        Arrays.fill(out.blue(), 1f);
        System.arraycopy(src.alpha(), 0, out.alpha(), 0, src.alpha().length);
        return out;
    }

    /** Converts to a libGDX Pixmap (needs the native library, so only at runtime). */
    static Pixmap toPixmap(Canvas c) {
        int w = c.getPixelWidth();
        int h = c.getPixelHeight();
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        int[] argb = toArgb(c);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = argb[y * w + x];
                pixmap.drawPixel(x, y, (v << 8) | (v >>> 24));
            }
        }
        return pixmap;
    }

    /** Packed ARGB pixels (used by tests and the art preview tool; no GPU needed). */
    static int[] toArgb(Canvas c) {
        float[] r = c.red();
        float[] g = c.green();
        float[] b = c.blue();
        float[] a = c.alpha();
        int[] out = new int[a.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte8(a[i]) << 24) | (byte8(r[i]) << 16) | (byte8(g[i]) << 8) | byte8(b[i]);
        }
        return out;
    }

    /** Fraction of pixels that are mostly opaque. */
    static float coverage(Canvas c) {
        int count = 0;
        for (float v : c.alpha()) if (v > 0.5f) count++;
        return count / (float) c.alpha().length;
    }

    /** Strongest alpha anywhere (faint sprites such as rim strips). */
    static float maxAlpha(Canvas c) {
        float m = 0f;
        for (float v : c.alpha()) m = Math.max(m, v);
        return m;
    }

    private static int byte8(float v) {
        return (int) ((v < 0 ? 0 : (v > 1 ? 1 : v)) * 255);
    }
}
