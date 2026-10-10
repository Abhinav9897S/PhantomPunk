package phantom.art;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import phantom.world.Region;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints four seamlessly tiling parallax layers per region (distant haze, far ruins,
 * mid pillars, near foreground silhouettes) plus shared light-shaft and fog textures.
 * Texture names: {@code bg_<region>_<0..3>}.
 * <p>OOP concept: implements {@link SpritePainter}; one method per region keeps each focused.</p>
 */
public final class BackgroundPainter implements SpritePainter {
    public static final float LAYER_W = 1024f;
    public static final float LAYER_H = 512f;
    private static final float SCALE = 1f;

    private final PaletteLibrary palettes;

    public BackgroundPainter(PaletteLibrary palettes) {
        this.palettes = palettes;
    }

    public static String name(Region region, int layer) {
        return "bg_" + region.name().toLowerCase() + "_" + layer;
    }

    @Override
    public void paint(SpriteRegistry reg) {
        for (Region region : Region.values()) {
            Palette p = palettes.get(region);
            reg.texture(name(region, 0), haze(reg, p, region), 1, true);
            reg.texture(name(region, 1), far(reg, p, region), 1, true);
            reg.texture(name(region, 2), mid(reg, p, region), 1, true);
            reg.texture(name(region, 3), near(reg, p, region), 1, true);
        }
        Canvas shaft = reg.canvas(128, 512, 1f);
        for (int y = 0; y < 512; y += 2) {
            float t = y / 512f;
            float half = 18 + 46 * t;
            shaft.fillRect(64 - half, y, half * 2, 2, rgba(1f, 1f, 1f, 0.5f * (1f - t) * (1f - t)));
        }
        reg.texture("shaft", shaft, 1, false);
        Canvas fog = reg.canvas(4, 256, 1f);
        for (int y = 0; y < 256; y++) fog.fillRect(0, y, 4, 1, rgba(1f, 1f, 1f, (y / 256f) * (y / 256f)));
        reg.texture("fog", fog, 1, false);
    }

    private Canvas base(SpriteRegistry reg) {
        return reg.canvas(LAYER_W, LAYER_H, SCALE);
    }

    private Canvas haze(SpriteRegistry reg, Palette p, Region region) {
        Canvas c = base(reg);
        c.gradientRect(0, 0, LAYER_W, LAYER_H, p.skyTop(), p.skyBottom());
        Random rnd = new Random(region.ordinal() * 17L + 1);
        Color glow = new Color(p.accent()).mul(1f, 1f, 1f, 0.06f);
        for (int i = 0; i < 6; i++) {
            float x = rnd.nextFloat() * LAYER_W;
            float y = 120 + rnd.nextFloat() * 300;
            float r = 120 + rnd.nextFloat() * 140;
            ProceduralArt.wrapped(LAYER_W, dx -> c.radial((float) (x + dx), y, r, glow, 1.5f, true));
        }
        Color far = new Color(p.far()).mul(1f, 1f, 1f, 0.55f);
        switch (region) {
            case WHISPERING_ROOTS:
                for (int i = 0; i < 7; i++) {
                    float x = i * LAYER_W / 7 + rnd.nextFloat() * 60;
                    float w = 30 + rnd.nextFloat() * 40;
                    ProceduralArt.wrapped(LAYER_W, dx -> c.fillPolygon(pts((float) (x + dx) - w, LAYER_H,
                            (float) (x + dx) - w * 0.4f, 0, (float) (x + dx) + w * 0.4f, 0, (float) (x + dx) + w, LAYER_H), far));
                }
                break;
            case GILDED_CISTERN:
                for (int i = 0; i < 8; i++) {
                    float x = i * LAYER_W / 8;
                    ProceduralArt.wrapped(LAYER_W, dx -> arch(c, (float) (x + dx), 330, 64, 150, 14, far));
                }
                break;
            default:
                ProceduralArt.motes(c, 90, 0, 0, LAYER_W, LAYER_H, 1f, 3f, rgba(0.95f, 0.9f, 1f, 0.7f), 99);
                c.radial(LAYER_W * 0.5f, LAYER_H * 0.75f, 300, rgba(0.6f, 0.45f, 0.9f, 0.18f), 1.5f, true);
                break;
        }
        return c;
    }

    private Canvas far(SpriteRegistry reg, Palette p, Region region) {
        Canvas c = base(reg);
        Random rnd = new Random(region.ordinal() * 31L + 2);
        Color col = p.far();
        switch (region) {
            case WHISPERING_ROOTS:
                for (int i = 0; i < 26; i++) {
                    float x = rnd.nextFloat() * LAYER_W;
                    float len = 60 + rnd.nextFloat() * 180;
                    float bend = (rnd.nextFloat() - 0.5f) * 0.5f;
                    float w = 6 + rnd.nextFloat() * 12;
                    ProceduralArt.wrapped(LAYER_W, dx -> ProceduralArt.tendril(c, (float) (x + dx), -10, (float) (x + dx) + bend * 60, len, bend, w, 1, col));
                }
                hills(c, LAYER_H - 70, 50, col, rnd);
                break;
            case GILDED_CISTERN:
                for (int i = 0; i < 6; i++) {
                    float x = i * LAYER_W / 6 + 40;
                    ProceduralArt.wrapped(LAYER_W, dx -> arch(c, (float) (x + dx), 260, 80, 260, 22, col));
                }
                Color water = rgba(0.55f, 0.7f, 0.9f, 0.12f);
                for (int i = 0; i < 5; i++) {
                    float x = 60 + i * 200 + rnd.nextFloat() * 40;
                    ProceduralArt.wrapped(LAYER_W, dx -> c.fillRect((float) (x + dx), 120, 10, LAYER_H, water));
                }
                hills(c, LAYER_H - 50, 20, col, rnd);
                break;
            default:
                for (int i = 0; i < 9; i++) {
                    float x = rnd.nextFloat() * LAYER_W;
                    float y = 80 + rnd.nextFloat() * 260;
                    float w = 40 + rnd.nextFloat() * 70;
                    long seed = rnd.nextLong();
                    ProceduralArt.wrapped(LAYER_W, dx -> island(c, (float) (x + dx), y, w, col, seed));
                }
                break;
        }
        return c;
    }

    private Canvas mid(SpriteRegistry reg, Palette p, Region region) {
        Canvas c = base(reg);
        Random rnd = new Random(region.ordinal() * 53L + 3);
        Color col = p.mid();
        Color accent = new Color(p.accent()).mul(1f, 1f, 1f, 0.8f);
        switch (region) {
            case WHISPERING_ROOTS:
                for (int i = 0; i < 9; i++) {
                    float x = rnd.nextFloat() * LAYER_W;
                    float len = 80 + rnd.nextFloat() * 160;
                    float bend = (rnd.nextFloat() - 0.5f) * 0.5f;
                    float w = 10 + rnd.nextFloat() * 14;
                    ProceduralArt.wrapped(LAYER_W, dx -> ProceduralArt.tendril(c, (float) (x + dx), -10, (float) (x + dx) + bend * 70, len, bend, w, 1.5f, col));
                }
                for (int i = 0; i < 4; i++) {
                    float x = i * 256 + rnd.nextFloat() * 60;
                    ProceduralArt.wrapped(LAYER_W, dx -> ProceduralArt.tendril(c, (float) (x + dx) - 120, LAYER_H, (float) (x + dx) + 120, LAYER_H, -0.9f, 34, 22, col));
                }
                for (int i = 0; i < 5; i++) {
                    float x = rnd.nextFloat() * LAYER_W;
                    float h = 90 + rnd.nextFloat() * 80;
                    float r = 30 + rnd.nextFloat() * 30;
                    long seed = rnd.nextLong();
                    ProceduralArt.wrapped(LAYER_W, dx -> mushroom(c, (float) (x + dx), LAYER_H, h, r, col, accent, seed));
                }
                break;
            case GILDED_CISTERN:
                for (int i = 0; i < 5; i++) {
                    float x = i * LAYER_W / 5 + 30;
                    ProceduralArt.wrapped(LAYER_W, dx -> c.fillRect((float) (x + dx), 0, 54, LAYER_H, col));
                }
                for (int i = 0; i < 4; i++) {
                    float x = 100 + i * 250;
                    float y = 120 + rnd.nextFloat() * 220;
                    float r = 50 + rnd.nextFloat() * 50;
                    ProceduralArt.wrapped(LAYER_W, dx -> {
                        ProceduralArt.gear(c, (float) (x + dx), y, r, 12, r * 0.15f, col);
                        c.cut((float) (x + dx), y, r * 0.35f, r * 0.35f);
                    });
                }
                break;
            default:
                for (int i = 0; i < 12; i++) {
                    float x = rnd.nextFloat() * LAYER_W;
                    float h = 120 + rnd.nextFloat() * 220;
                    float w = 16 + rnd.nextFloat() * 26;
                    float tilt = (rnd.nextFloat() - 0.5f) * 20;
                    ProceduralArt.wrapped(LAYER_W, dx -> ProceduralArt.crystal(c, (float) (x + dx), LAYER_H, w, h, tilt,
                            col, ProceduralArt.mix(col, accent, 0.25f)));
                }
                break;
        }
        return c;
    }

    private Canvas near(SpriteRegistry reg, Palette p, Region region) {
        Canvas c = base(reg);
        Random rnd = new Random(region.ordinal() * 71L + 4);
        Color col = p.near();
        for (int i = 0; i < 7; i++) {
            float x = rnd.nextFloat() * LAYER_W;
            float len = 50 + rnd.nextFloat() * 120;
            float w = 14 + rnd.nextFloat() * 24;
            float bend = (rnd.nextFloat() - 0.5f) * 0.6f;
            ProceduralArt.wrapped(LAYER_W, dx -> ProceduralArt.tendril(c, (float) (x + dx), -10, (float) (x + dx) + bend * 80, len, bend, w, 2, col));
        }
        for (int i = 0; i < 6; i++) {
            float x = rnd.nextFloat() * LAYER_W;
            float rx = 60 + rnd.nextFloat() * 90;
            float ry = 30 + rnd.nextFloat() * 50;
            long seed = rnd.nextLong();
            ProceduralArt.wrapped(LAYER_W, dx -> ProceduralArt.blob(c, (float) (x + dx), LAYER_H + 10, rx, ry, 0.15f, seed, col));
        }
        return c;
    }

    private static void arch(Canvas c, float x, float y, float w, float h, float thick, Color col) {
        c.fillRect(x - w, y - h * 0.2f, thick, LAYER_H, col);
        c.fillRect(x + w - thick, y - h * 0.2f, thick, LAYER_H, col);
        ProceduralArt.tendril(c, x - w + thick / 2, y - h * 0.2f, x + w - thick / 2, y - h * 0.2f, -0.45f, thick, thick, col);
        c.fillRect(x - w - 6, y - h * 0.2f - h * 0.55f - thick, w * 2 + 12, thick * 0.8f, col);
    }

    private static void hills(Canvas c, float y, float amp, Color col, Random rnd) {
        int n = 32;
        float[] poly = new float[(n + 3) * 2];
        float p1 = rnd.nextFloat() * 6;
        for (int i = 0; i <= n; i++) {
            float x = i / (float) n * LAYER_W;
            double a = i / (double) n * Math.PI * 2;
            poly[i * 2] = x;
            poly[i * 2 + 1] = y - amp * (float) (0.5 + 0.3 * Math.sin(a * 2 + p1) + 0.2 * Math.sin(a * 5 + p1 * 2));
        }
        poly[(n + 1) * 2] = LAYER_W;
        poly[(n + 1) * 2 + 1] = LAYER_H;
        poly[(n + 2) * 2] = 0;
        poly[(n + 2) * 2 + 1] = LAYER_H;
        c.fillPolygon(poly, col);
    }

    private static void mushroom(Canvas c, float x, float ground, float h, float r, Color col, Color accent, long seed) {
        c.stroke(x, ground, x + 6, ground - h, r * 0.3f, r * 0.22f, col);
        int n = 16;
        float[] cap = new float[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double a = Math.PI + i / (double) n * Math.PI;
            cap[i * 2] = x + 6 + (float) Math.cos(a) * r;
            cap[i * 2 + 1] = ground - h + 6 + (float) Math.sin(a) * r * 0.62f;
        }
        c.fillPolygon(cap, col);
        ProceduralArt.motes(c, 6, x + 6 - r * 0.7f, ground - h - r * 0.45f, r * 1.4f, r * 0.45f, 1.5f, 4f, accent, seed);
    }

    private static void island(Canvas c, float x, float y, float w, Color col, long seed) {
        ProceduralArt.blob(c, x, y, w, w * 0.18f, 0.2f, seed, col);
        c.fillPolygon(pts(x - w * 0.8f, y, x + w * 0.8f, y, x + w * 0.1f, y + w * 0.9f), col);
    }
}
