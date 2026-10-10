package phantom.art;

import java.util.EnumMap;
import java.util.Map;

import phantom.world.Region;

import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * The three region palettes: teal/green roots, amber/blue cistern, violet/white heart.
 * <p>OOP concept: {@link EnumMap} keyed by {@link Region}.</p>
 */
public final class PaletteLibrary {
    private final Map<Region, Palette> palettes = new EnumMap<>(Region.class);

    public PaletteLibrary() {
        palettes.put(Region.WHISPERING_ROOTS, new Palette(
                rgb(0.015f, 0.04f, 0.05f), rgb(0.05f, 0.13f, 0.12f), rgba(0.12f, 0.3f, 0.27f, 1f),
                rgb(0.05f, 0.12f, 0.12f), rgb(0.03f, 0.075f, 0.075f), rgb(0.01f, 0.02f, 0.022f),
                rgb(0.045f, 0.06f, 0.06f), rgb(0.16f, 0.32f, 0.22f), rgba(0.55f, 0.85f, 0.75f, 0.85f),
                rgb(0.4f, 1f, 0.8f), rgb(0.29f, 0.33f, 0.34f)));
        palettes.put(Region.GILDED_CISTERN, new Palette(
                rgb(0.015f, 0.025f, 0.06f), rgb(0.12f, 0.09f, 0.07f), rgba(0.35f, 0.26f, 0.18f, 1f),
                rgb(0.09f, 0.08f, 0.1f), rgb(0.05f, 0.05f, 0.075f), rgb(0.012f, 0.012f, 0.02f),
                rgb(0.055f, 0.055f, 0.07f), rgb(0.42f, 0.3f, 0.14f), rgba(0.95f, 0.75f, 0.45f, 0.85f),
                rgb(1f, 0.7f, 0.35f), rgb(0.31f, 0.3f, 0.35f)));
        palettes.put(Region.UMBRAL_HEART, new Palette(
                rgb(0.02f, 0.01f, 0.04f), rgb(0.1f, 0.07f, 0.16f), rgba(0.3f, 0.24f, 0.42f, 1f),
                rgb(0.09f, 0.07f, 0.14f), rgb(0.05f, 0.04f, 0.08f), rgb(0.015f, 0.01f, 0.025f),
                rgb(0.05f, 0.045f, 0.07f), rgb(0.6f, 0.55f, 0.75f), rgba(0.85f, 0.8f, 1f, 0.85f),
                rgb(0.8f, 0.65f, 1f), rgb(0.3f, 0.28f, 0.36f)));
    }

    public Palette get(Region region) {
        return palettes.get(region);
    }
}
