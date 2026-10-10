package phantom.sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.art.BackgroundPainter;
import phantom.art.BossPainter;
import phantom.art.CharacterPainter;
import phantom.art.CreaturePainter;
import phantom.art.NpcPainter;
import phantom.art.PaletteLibrary;
import phantom.art.PropPainter;
import phantom.art.RecordingRegistry;
import phantom.art.SpritePainter;
import phantom.art.TilePainter;
import phantom.art.UiPainter;
import phantom.events.EventBus;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.util.Constants;
import phantom.world.GameWorld;

/**
 * Coarse timing checks (generous limits, so they only catch real regressions). The measured
 * numbers are printed and quoted in docs/TEST_REPORT.md.
 */
class PerformanceSmokeTest {
    @Test
    void simulationTickIsCheap() throws Exception {
        ScriptedInput input = new ScriptedInput();
        GameWorld world = new GameWorld(TestAssets.loadWorld(), new EventBus(), input, null);
        world.start();
        world.enterRoom("r2_aqueduct", "west");
        Random rnd = new Random(1);
        GameAction[] moves = {GameAction.RIGHT, GameAction.LEFT, GameAction.JUMP, GameAction.ATTACK};
        for (int i = 0; i < 600; i++) { input.poll(); world.update(Constants.STEP); } // warm up the JIT
        long start = System.nanoTime();
        int ticks = 6000;
        for (int i = 0; i < ticks; i++) {
            if (i % 20 == 0) { input.releaseAll(); input.press(moves[rnd.nextInt(moves.length)]); }
            input.poll();
            world.update(Constants.STEP);
            world.getPlayer().getHealth().makeInvincible(1f);
        }
        double msPerTick = (System.nanoTime() - start) / 1e6 / ticks;
        System.out.printf("[perf] simulation: %.4f ms per 1/60 s tick (%d particles live)%n", msPerTick,
                world.getParticles().getLiveCount());
        assertTrue(msPerTick < 4.0, "simulation too slow: " + msPerTick + " ms/tick");
    }

    @Test
    void allArtPaintsQuickly() {
        PaletteLibrary palettes = new PaletteLibrary();
        List<SpritePainter> painters = List.of(new CharacterPainter(), new CreaturePainter(), new BossPainter(),
                new PropPainter(), new NpcPainter(), new TilePainter(palettes), new BackgroundPainter(palettes), new UiPainter());
        long start = System.nanoTime();
        RecordingRegistry reg = new RecordingRegistry();
        for (SpritePainter p : painters) p.paint(reg);
        double seconds = (System.nanoTime() - start) / 1e9;
        System.out.printf("[perf] painting %d sprites and %d textures took %.2f s (CPU, before GPU upload)%n",
                reg.getSprites().size(), reg.getTextures().size(), seconds);
        assertTrue(seconds < 30, "art generation too slow: " + seconds + " s");
    }
}
