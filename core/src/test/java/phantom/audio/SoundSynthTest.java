package phantom.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.world.Region;

/** Synthesised audio is valid, audible and loops cleanly. */
class SoundSynthTest {
    @Test
    void everyEffectIsAudibleAndInRange() {
        for (SoundId id : SoundId.values()) {
            float[] s = SoundSynth.effect(id);
            float peak = 0;
            for (float v : s) peak = Math.max(peak, Math.abs(v));
            assertTrue(s.length > 100, id + " too short");
            assertTrue(peak > 0.05f, id + " silent");
        }
    }

    @Test
    void wavHeaderIsCorrect() {
        byte[] wav = SoundSynth.toWav(new float[100]);
        assertEquals(44 + 200, wav.length);
        assertEquals('R', wav[0]);
        assertEquals('W', wav[8]);
        assertEquals('d', wav[36]);
    }

    @Test
    void ambientLoopsWithoutAClick() {
        float[] a = SoundSynth.ambient(Region.GILDED_CISTERN, 8f);
        float jump = Math.abs(a[0] - a[a.length - 1]);
        assertTrue(jump < 0.08f, "seam discontinuity " + jump);
    }
}
