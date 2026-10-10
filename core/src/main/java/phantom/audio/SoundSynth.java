package phantom.audio;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import phantom.world.Region;

/**
 * Synthesises every sound effect and the ambient drones from scratch (oscillators, filtered
 * noise and envelopes) and encodes them as WAV data. Pure Java, so it is unit-testable.
 * <p>OOP concept: stateless utility; no external audio files or unknown licences needed.</p>
 */
public final class SoundSynth {
    public static final int RATE = 22050;

    private SoundSynth() { }

    public static float[] effect(SoundId id) {
        switch (id) {
            case SLASH: return mix(noise(0.13f, 0.5f, 6000, 3), sweep(0.1f, 900, 300, 0.15f, Wave.SINE));
            case HIT: return mix(sweep(0.12f, 160, 55, 0.9f, Wave.SINE), noise(0.05f, 0.5f, 3000, 2));
            case ENEMY_DEATH: return mix(noise(0.4f, 0.45f, 1800, 1.5f), sweep(0.35f, 420, 70, 0.5f, Wave.TRIANGLE));
            case JUMP: return sweep(0.09f, 210, 430, 0.25f, Wave.TRIANGLE);
            case LAND: return noise(0.08f, 0.5f, 500, 4);
            case DASH: return noise(0.22f, 0.55f, 2500, 1.6f);
            case WISP_LEAP: return mix(tone(0.35f, 880, 0.25f, Wave.SINE, 2.5f), tone(0.35f, 1320, 0.18f, Wave.SINE, 3f));
            case PLAYER_HURT: return mix(sweep(0.28f, 240, 90, 0.5f, Wave.SQUARE), noise(0.2f, 0.4f, 2000, 2));
            case HEAL: return arpeggio(new float[] {523, 659, 784, 1046}, 0.11f, 0.35f);
            case FOCUS: return sweep(0.6f, 220, 440, 0.2f, Wave.SINE);
            case BOLT: return mix(sweep(0.28f, 1400, 380, 0.4f, Wave.SAW), noise(0.2f, 0.25f, 5000, 2));
            case SHARD: return tone(0.09f, 1650, 0.25f, Wave.SINE, 5f);
            case BENCH: return mix(tone(1.0f, 262, 0.25f, Wave.SINE, 1.2f), tone(1.0f, 392, 0.2f, Wave.SINE, 1.2f), tone(1.0f, 523, 0.15f, Wave.SINE, 1.4f));
            case DOOR: return noise(0.3f, 0.3f, 700, 2);
            case BOSS_ROAR: return mix(growl(1.2f), noise(1.0f, 0.3f, 900, 1.2f));
            case BOSS_SLAM: return mix(sweep(0.5f, 80, 35, 1f, Wave.SINE), noise(0.35f, 0.6f, 1200, 2.5f));
            case UI_MOVE: return tone(0.035f, 1000, 0.2f, Wave.TRIANGLE, 8f);
            case UI_SELECT: return mix(tone(0.07f, 660, 0.25f, Wave.TRIANGLE, 5f), delayed(tone(0.09f, 990, 0.25f, Wave.TRIANGLE, 5f), 0.05f));
            case UI_DENY: return tone(0.14f, 140, 0.3f, Wave.SQUARE, 4f);
            case BREAK: return noise(0.28f, 0.6f, 1500, 2.2f);
            case ABILITY: return mix(arpeggio(new float[] {392, 523, 659, 784, 1046}, 0.13f, 0.3f), tone(1.4f, 196, 0.25f, Wave.SINE, 0.9f));
            case SHOOT: return sweep(0.14f, 620, 280, 0.3f, Wave.TRIANGLE);
            default: return tone(0.05f, 440, 0.1f, Wave.SINE, 5f);
        }
    }

    /** A seamless ambient loop: detuned sine pads with slow swells plus filtered wind. */
    public static float[] ambient(Region region, float seconds) {
        float[] base;
        switch (region) {
            case WHISPERING_ROOTS: base = new float[] {55f, 82.5f, 110f, 164.8f}; break;
            case GILDED_CISTERN: base = new float[] {49f, 73.5f, 98f, 146.8f}; break;
            default: base = new float[] {41.2f, 61.7f, 87.3f, 130.8f}; break;
        }
        int n = (int) (seconds * RATE);
        float[] out = new float[n];
        for (int k = 0; k < base.length; k++) {
            // snap each frequency so a whole number of cycles fits the loop (no click at the seam)
            float f = Math.round(base[k] * seconds) / seconds;
            float f2 = Math.round(base[k] * 1.004f * seconds) / seconds;
            float lfo = Math.max(1, Math.round(seconds / 4f)) / seconds;
            for (int i = 0; i < n; i++) {
                double t = i / (double) RATE;
                double swell = 0.6 + 0.4 * Math.sin(2 * Math.PI * lfo * t + k * 1.7);
                out[i] += (float) ((Math.sin(2 * Math.PI * f * t) + Math.sin(2 * Math.PI * f2 * t)) * 0.07 * swell);
            }
        }
        Random rnd = new Random(region.ordinal());
        float lp = 0f;
        for (int i = 0; i < n; i++) {
            lp += (rnd.nextFloat() * 2 - 1 - lp) * 0.02f;
            double t = i / (double) RATE;
            out[i] += lp * 0.25f * (float) (0.6 + 0.4 * Math.sin(2 * Math.PI * t / seconds));
        }
        return out;
    }

    private enum Wave { SINE, TRIANGLE, SQUARE, SAW }

    private static float osc(Wave w, double phase) {
        double p = phase - Math.floor(phase);
        switch (w) {
            case TRIANGLE: return (float) (4 * Math.abs(p - 0.5) - 1);
            case SQUARE: return p < 0.5 ? 0.6f : -0.6f;
            case SAW: return (float) (2 * p - 1) * 0.6f;
            default: return (float) Math.sin(2 * Math.PI * p);
        }
    }

    private static float[] tone(float seconds, float freq, float volume, Wave w, float decay) {
        return sweep(seconds, freq, freq, volume, w, decay);
    }

    private static float[] sweep(float seconds, float f0, float f1, float volume, Wave w) {
        return sweep(seconds, f0, f1, volume, w, 3f);
    }

    private static float[] sweep(float seconds, float f0, float f1, float volume, Wave w, float decay) {
        int n = (int) (seconds * RATE);
        float[] out = new float[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float f = f0 + (f1 - f0) * t;
            phase += f / RATE;
            float attack = Math.min(1f, i / (RATE * 0.004f));
            out[i] = osc(w, phase) * volume * attack * (float) Math.exp(-decay * t);
        }
        return out;
    }

    private static float[] noise(float seconds, float volume, float cutoff, float decay) {
        int n = (int) (seconds * RATE);
        float[] out = new float[n];
        Random rnd = new Random(n);
        float k = (float) (1 - Math.exp(-2 * Math.PI * cutoff / RATE));
        float lp = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            lp += (rnd.nextFloat() * 2 - 1 - lp) * k;
            out[i] = lp * volume * (float) Math.exp(-decay * t * 2);
        }
        return out;
    }

    private static float[] growl(float seconds) {
        int n = (int) (seconds * RATE);
        float[] out = new float[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n;
            float f = 70 + 12 * (float) Math.sin(i / (float) RATE * 2 * Math.PI * 9) - 25 * t;
            phase += f / RATE;
            float env = Math.min(1f, t * 8) * (1 - t);
            out[i] = osc(Wave.SAW, phase) * 0.7f * env;
        }
        return out;
    }

    private static float[] arpeggio(float[] notes, float step, float volume) {
        float[] out = new float[0];
        for (int i = 0; i < notes.length; i++) {
            out = mix(out, delayed(tone(step * 3, notes[i], volume, Wave.SINE, 2.5f), step * i));
        }
        return out;
    }

    private static float[] delayed(float[] s, float seconds) {
        int d = (int) (seconds * RATE);
        float[] out = new float[s.length + d];
        System.arraycopy(s, 0, out, d, s.length);
        return out;
    }

    private static float[] mix(float[]... parts) {
        int n = 0;
        for (float[] p : parts) n = Math.max(n, p.length);
        float[] out = new float[n];
        for (float[] p : parts) {
            for (int i = 0; i < p.length; i++) out[i] += p[i];
        }
        return out;
    }

    /** Encodes mono 16-bit PCM WAV. */
    public static byte[] toWav(float[] samples) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + samples.length * 2);
        int dataLen = samples.length * 2;
        writeAscii(out, "RIFF");
        writeInt(out, 36 + dataLen);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeInt(out, 16);
        writeShort(out, 1);
        writeShort(out, 1);
        writeInt(out, RATE);
        writeInt(out, RATE * 2);
        writeShort(out, 2);
        writeShort(out, 16);
        writeAscii(out, "data");
        writeInt(out, dataLen);
        for (float s : samples) {
            int v = (int) (Math.max(-1f, Math.min(1f, s)) * 32767);
            writeShort(out, v);
        }
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream o, String s) {
        for (int i = 0; i < s.length(); i++) o.write(s.charAt(i));
    }

    private static void writeInt(ByteArrayOutputStream o, int v) {
        o.write(v & 0xff);
        o.write((v >> 8) & 0xff);
        o.write((v >> 16) & 0xff);
        o.write((v >> 24) & 0xff);
    }

    private static void writeShort(ByteArrayOutputStream o, int v) {
        o.write(v & 0xff);
        o.write((v >> 8) & 0xff);
    }
}
