package phantom.autoplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import phantom.components.Inventory;
import phantom.story.Ending;

/**
 * The "Autoplay" menu option, end to end: the AI plays a new game from the Fallen Gate to the final
 * choice using only button presses, with real physics, real enemies and the real bosses. Each seed
 * changes every random decision in the world (boss attack order, particle effects, enemy timing).
 */
class AutoplayPlaythroughTest {
    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L})
    void theAutopilotFinishesTheGame(long seed) throws Exception {
        PlaythroughRunner run = new PlaythroughRunner(seed);
        run.play(1200f);
        assertEquals(Ending.DAWN.name(), run.ending, run.summary());
        assertEquals(Inventory.TOTAL_RELICS, run.world.getPlayer().getInventory().getRelicCount(), run.summary());
        assertTrue(run.world.getProgress().isBossDefeated("gnarlwarden"), run.summary());
        assertTrue(run.world.getProgress().isBossDefeated("gilded_warden"), run.summary());
        assertTrue(run.world.getProgress().isBossDefeated("hollow_heart"), run.summary());
        assertTrue(run.deaths <= 3, run.summary());
    }
}
