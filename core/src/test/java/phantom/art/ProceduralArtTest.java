package phantom.art;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;

import phantom.world.Region;

/** The software rasteriser and every painter work headless and produce visible art. */
class ProceduralArtTest {
    @Test
    void polygonFillCoversTheExpectedArea() {
        Canvas c = new Canvas(10, 10, 4);
        c.fillPolygon(new float[] {0, 0, 10, 0, 10, 5, 0, 5}, Color.WHITE);
        assertEquals(0.5f, c.coverage(), 0.02f);
        Canvas d = c.downsample(2);
        assertEquals(20, d.getPixelWidth());
        assertEquals(0.5f, d.coverage(), 0.03f);
    }

    @Test
    void cutAndSilhouetteBehave() {
        Canvas c = new Canvas(8, 8, 2);
        c.fillRect(0, 0, 8, 8, Color.BLACK);
        c.cut(4, 4, 2, 2);
        assertEquals(0f, c.alphaAt(8, 8), 1e-6);
        Canvas s = c.silhouette();
        assertEquals(c.coverage(), s.coverage(), 1e-6);
    }

    @Test
    void everyPainterProducesNonEmptySprites() {
        PaletteLibrary palettes = new PaletteLibrary();
        List<SpritePainter> painters = List.of(new CharacterPainter(), new CreaturePainter(), new BossPainter(),
                new PropPainter(), new NpcPainter(), new TilePainter(palettes), new UiPainter());
        RecordingRegistry reg = new RecordingRegistry();
        for (SpritePainter p : painters) p.paint(reg);
        for (Map.Entry<String, List<Canvas>> e : reg.getSprites().entrySet()) {
            for (Canvas c : e.getValue()) assertTrue(c.coverage() > 0.002f || c.maxAlpha() > 0.2f, e.getKey() + " is empty");
        }
        for (String needed : List.of("player_idle", "player_run", "crawler", "flutterer", "sentinel", "brute",
                "gnarlwarden", "warden", "heart_core", "bench", "slash", "mask_full", "soul_orb")) {
            assertTrue(reg.getSprites().containsKey(needed), needed);
        }
        assertEquals(6, reg.getSprites().get("player_run").size());
        for (Region r : Region.values()) assertTrue(reg.getSprites().containsKey(TilePainter.name(r, "top")));
    }

    @Test
    void backgroundsHaveFourLayersPerRegion() {
        RecordingRegistry reg = new RecordingRegistry();
        new BackgroundPainter(new PaletteLibrary()).paint(reg);
        for (Region r : Region.values()) {
            for (int i = 0; i < 4; i++) assertTrue(reg.getTextures().containsKey(BackgroundPainter.name(r, i)));
        }
    }
}
