package phantom.art;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import phantom.world.Region;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints the tile set of each region: rock interiors, overhanging top edges (moss, brass trim,
 * white crystal crust), hanging undersides, one-way ledges, spikes, gates and cracked walls.
 * Sprite names follow {@code tile_<region>_<part>}.
 * <p>OOP concept: implements {@link SpritePainter}; the region palette drives the colours.</p>
 */
public final class TilePainter implements SpritePainter {
    private final PaletteLibrary palettes;

    public TilePainter(PaletteLibrary palettes) {
        this.palettes = palettes;
    }

    public static String name(Region region, String part) {
        return "tile_" + region.name().toLowerCase() + "_" + part;
    }

    @Override
    public void paint(SpriteRegistry reg) {
        for (Region region : Region.values()) {
            Palette p = palettes.get(region);
            int seed = region.ordinal() * 100;
            reg.sprite(name(region, "solid"), 0, false,
                    solid(reg, p, seed), solid(reg, p, seed + 1), solid(reg, p, seed + 2), solid(reg, p, seed + 3));
            reg.sprite(name(region, "top"), 0, false, top(reg, p, region, seed), top(reg, p, region, seed + 7), top(reg, p, region, seed + 13));
            reg.sprite(name(region, "bottom"), 0, false, bottom(reg, p, region, seed), bottom(reg, p, region, seed + 5));
            reg.sprite(name(region, "side"), 0, false, side(reg, p));
            reg.sprite(name(region, "oneway"), 0, false, oneWay(reg, p, region));
            reg.sprite(name(region, "spikes"), 0, false, spikes(reg, p, region, seed), spikes(reg, p, region, seed + 3));
            reg.sprite(name(region, "gate"), 0, false, gate(reg, region));
            reg.sprite(name(region, "breakable"), 0, false, breakable(reg, p, seed));
        }
    }

    private Canvas solid(SpriteRegistry reg, Palette p, long seed) {
        Canvas c = reg.canvas(32, 32);
        Color g = p.ground();
        c.fillRect(0, 0, 32, 32, g);
        Random rnd = new Random(seed);
        for (int i = 0; i < 5; i++) {
            float shade = (rnd.nextFloat() - 0.5f) * 0.03f;
            Color blot = rgb(g.r + shade, g.g + shade, g.b + shade);
            ProceduralArt.blob(c, rnd.nextFloat() * 32, rnd.nextFloat() * 32, 4 + rnd.nextFloat() * 6, 3 + rnd.nextFloat() * 4,
                    0.25f, seed * 7 + i, blot);
        }
        c.grain(0.018f, seed);
        return c;
    }

    /** 32x20 strip drawn over the top 8px of a tile and overhanging 12px above it. */
    private Canvas top(SpriteRegistry reg, Palette p, Region region, long seed) {
        Canvas c = reg.canvas(32, 20);
        Color edge = p.groundEdge();
        Color dark = p.ground();
        Random rnd = new Random(seed);
        c.fillRect(0, 12, 32, 8, ProceduralArt.mix(dark, edge, 0.35f));
        c.fillRect(0, 12, 32, 2.2f, edge);
        switch (region) {
            case WHISPERING_ROOTS:
                for (int i = 0; i < 9; i++) {
                    float x = rnd.nextFloat() * 32;
                    float h = 3 + rnd.nextFloat() * 8;
                    ProceduralArt.tendril(c, x, 13, x + (rnd.nextFloat() - 0.5f) * 5, 13 - h, 0.2f, 1.6f, 0.3f,
                            ProceduralArt.mix(edge, rgb(0.35f, 0.6f, 0.45f), rnd.nextFloat()));
                }
                ProceduralArt.blob(c, rnd.nextFloat() * 32, 13, 6, 2.5f, 0.3f, seed, edge);
                break;
            case GILDED_CISTERN:
                c.fillRect(0, 11, 32, 3, rgb(0.5f, 0.36f, 0.16f));
                c.fillRect(0, 11, 32, 0.8f, rgb(0.9f, 0.7f, 0.38f));
                c.fillCircle(6 + rnd.nextFloat() * 4, 12.5f, 1f, rgb(0.85f, 0.65f, 0.35f));
                c.fillCircle(22 + rnd.nextFloat() * 4, 12.5f, 1f, rgb(0.85f, 0.65f, 0.35f));
                break;
            default:
                for (int i = 0; i < 4; i++) {
                    ProceduralArt.crystal(c, 3 + rnd.nextFloat() * 26, 14, 3 + rnd.nextFloat() * 3, 4 + rnd.nextFloat() * 7,
                            (rnd.nextFloat() - 0.5f) * 3, rgb(0.35f, 0.3f, 0.45f), rgb(0.85f, 0.82f, 0.95f));
                }
                break;
        }
        return c;
    }

    /** 32x14 strip hanging below a ceiling tile. */
    private Canvas bottom(SpriteRegistry reg, Palette p, Region region, long seed) {
        Canvas c = reg.canvas(32, 14);
        Random rnd = new Random(seed);
        Color dark = p.ground();
        c.fillRect(0, 0, 32, 3, ProceduralArt.mix(dark, p.groundEdge(), 0.15f));
        for (int i = 0; i < 4; i++) {
            float x = 2 + rnd.nextFloat() * 28;
            float len = 4 + rnd.nextFloat() * 9;
            if (region == Region.GILDED_CISTERN) {
                c.fillRect(x - 0.6f, 0, 1.2f, len * 0.6f, rgb(0.3f, 0.22f, 0.1f));
            } else {
                ProceduralArt.tendril(c, x, 1, x + (rnd.nextFloat() - 0.5f) * 4, len, 0.3f, 2.4f, 0.3f, dark);
            }
        }
        return c;
    }

    private Canvas side(SpriteRegistry reg, Palette p) {
        Canvas c = reg.canvas(6, 32);
        Color rim = p.groundEdge();
        for (int i = 0; i < 6; i++) {
            c.fillRect(i, 0, 1, 32, rim, 0.5f * (1f - i / 6f));
        }
        return c;
    }

    private Canvas oneWay(SpriteRegistry reg, Palette p, Region region) {
        Canvas c = reg.canvas(32, 14);
        switch (region) {
            case WHISPERING_ROOTS:
                ProceduralArt.tendril(c, -2, 5, 34, 5, 0.04f, 8, 7, rgb(0.11f, 0.09f, 0.07f));
                c.fillRect(0, 1.5f, 32, 1.4f, p.groundEdge());
                ProceduralArt.tendril(c, 10, 8, 12, 14, 0.3f, 1.6f, 0.3f, rgb(0.11f, 0.09f, 0.07f));
                break;
            case GILDED_CISTERN:
                c.fillRect(0, 1, 32, 5, rgb(0.38f, 0.27f, 0.11f));
                c.fillRect(0, 1, 32, 1, rgb(0.85f, 0.65f, 0.35f));
                for (int i = 0; i < 4; i++) c.fillRect(2 + i * 8, 6, 3, 6, rgb(0.22f, 0.15f, 0.06f));
                break;
            default:
                c.fillPolygon(pts(0, 1, 32, 1, 28, 9, 18, 13, 8, 10), rgb(0.07f, 0.06f, 0.1f));
                c.fillRect(0, 1, 32, 1.4f, rgb(0.85f, 0.8f, 1f));
                break;
        }
        return c;
    }

    private Canvas spikes(SpriteRegistry reg, Palette p, Region region, long seed) {
        Canvas c = reg.canvas(32, 32);
        Random rnd = new Random(seed);
        Color dark;
        Color tip;
        switch (region) {
            case WHISPERING_ROOTS: dark = rgb(0.06f, 0.08f, 0.06f); tip = rgb(0.3f, 0.45f, 0.32f); break;
            case GILDED_CISTERN: dark = rgb(0.25f, 0.17f, 0.07f); tip = rgb(0.85f, 0.65f, 0.35f); break;
            default: dark = rgb(0.2f, 0.16f, 0.3f); tip = rgb(0.9f, 0.85f, 1f); break;
        }
        for (int i = 0; i < 5; i++) {
            float x = 3 + i * 6.5f + (rnd.nextFloat() - 0.5f) * 2;
            float h = 12 + rnd.nextFloat() * 8;
            float ang = (rnd.nextFloat() - 0.5f) * 20;
            ProceduralArt.spike(c, x, 32, 6, h, ang, dark);
            ProceduralArt.spike(c, x, 32 - h * 0.55f, 2.6f, h * 0.45f, ang, tip);
        }
        return c;
    }

    private Canvas gate(SpriteRegistry reg, Region region) {
        Canvas c = reg.canvas(32, 32);
        Color bar = region == Region.GILDED_CISTERN ? rgb(0.4f, 0.28f, 0.12f) : rgb(0.12f, 0.12f, 0.14f);
        for (int i = 0; i < 3; i++) {
            c.fillRect(4 + i * 10, 0, 4, 32, bar);
            c.fillRect(4 + i * 10, 0, 1, 32, rgba(0.7f, 0.7f, 0.8f, 0.6f));
        }
        c.fillRect(0, 12, 32, 3, bar);
        return c;
    }

    private Canvas breakable(SpriteRegistry reg, Palette p, long seed) {
        Canvas c = solid(reg, p, seed + 50);
        Random rnd = new Random(seed);
        Color crack = ProceduralArt.mix(p.ground(), p.groundEdge(), 0.6f);
        for (int i = 0; i < 3; i++) {
            float x = 6 + rnd.nextFloat() * 20;
            ProceduralArt.tendril(c, x, 2, x + (rnd.nextFloat() - 0.5f) * 14, 30, 0.3f, 1.4f, 0.6f, crack);
        }
        return c;
    }
}
