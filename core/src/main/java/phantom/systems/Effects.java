package phantom.systems;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import phantom.world.Region;

/**
 * Ready-made particle recipes (hit sparks, dust, death bursts, ambient spores...).
 * Keeps visual tuning in one place instead of scattering numbers through gameplay code.
 * <p>OOP concept: <b>composition</b> - wraps a {@link ParticleSystem} and adds intent-revealing methods.</p>
 */
public final class Effects {
    private static final Color SPARK = new Color(1f, 0.93f, 0.72f, 1f);
    private static final Color INK = new Color(0.03f, 0.03f, 0.05f, 1f);
    private static final Color DUST = new Color(0.55f, 0.58f, 0.6f, 1f);
    private static final Color SOUL = new Color(0.85f, 0.95f, 1f, 1f);
    private static final Color SHARD = new Color(0.75f, 0.95f, 1f, 1f);
    private static final Color SPORE = new Color(0.45f, 1f, 0.8f, 1f);
    private static final Color AMBER = new Color(1f, 0.72f, 0.35f, 1f);
    private static final Color DRIP = new Color(0.55f, 0.75f, 1f, 1f);
    private static final Color ASH = new Color(0.75f, 0.6f, 1f, 1f);
    private static final Color MIST = new Color(0.9f, 0.88f, 1f, 1f);

    private final ParticleSystem particles;
    private final Random random;
    private float ambientAccumulator;

    public Effects(ParticleSystem particles) {
        this.particles = particles;
        this.random = particles.getRandom();
    }

    public void hitSparks(float x, float y, float dirX) {
        float angle = dirX >= 0 ? 0f : 180f;
        particles.burst(x, y, 12, angle, 110f, 220f, 620f, 0.28f, 5f, SPARK, true, 300f);
        particles.burst(x, y, 6, angle, 360f, 40f, 160f, 0.4f, 9f, INK, false, 0f);
    }

    public void enemyDeath(float x, float y, Color glow) {
        particles.burst(x, y, 26, 90f, 360f, 60f, 340f, 0.7f, 12f, INK, false, -40f);
        particles.burst(x, y, 16, 90f, 360f, 40f, 220f, 1.0f, 6f, glow, true, -60f);
    }

    public void bossDeath(float x, float y, Color glow) {
        particles.burst(x, y, 90, 90f, 360f, 80f, 620f, 1.6f, 18f, INK, false, -30f);
        particles.burst(x, y, 70, 90f, 360f, 60f, 480f, 2.2f, 9f, glow, true, -50f);
    }

    public void dust(float x, float y, int count) {
        particles.burst(x, y + 2, count, 90f, 150f, 20f, 110f, 0.45f, 7f, DUST, false, 60f);
    }

    public void dashTrail(float x, float y) {
        particles.spawnPhysical(x + rand(-4, 4), y + rand(-14, 14), rand(-30, 30), rand(-10, 10), 0.35f,
                10f, 2f, INK, 0.8f, false, 0f, 3f, 0f);
    }

    public void wisp(float x, float y, Color c) {
        particles.spawnPhysical(x + rand(-6, 6), y + rand(-6, 6), rand(-40, 40), rand(20, 90), 0.6f,
                6f, 1f, c, 0.9f, true, -40f, 1.5f, 10f);
    }

    public void soulGain(float x, float y) {
        particles.burst(x, y, 6, 90f, 120f, 40f, 150f, 0.6f, 5f, SOUL, true, -120f);
    }

    public void focusGather(float x, float y) {
        double a = random.nextDouble() * Math.PI * 2;
        float r = rand(40, 70);
        float px = x + (float) Math.cos(a) * r;
        float py = y + (float) Math.sin(a) * r;
        particles.spawnPhysical(px, py, (x - px) * 2.2f, (y - py) * 2.2f, 0.45f, 5f, 1f, SOUL, 1f, true, 0f, 0f, 0f);
    }

    public void shardSparkle(float x, float y) {
        particles.burst(x, y, 5, 90f, 360f, 30f, 90f, 0.4f, 4f, SHARD, true, 0f);
    }

    public void debris(float x, float y, float w, float h, Color tint, int count) {
        for (int i = 0; i < count; i++) {
            particles.spawnPhysical(x + rand(0, w), y + rand(0, h), rand(-160, 160), rand(40, 320), rand(0.6f, 1.1f),
                    rand(5, 11), 3f, tint, 1f, false, 900f, 0.5f, 0f);
        }
        particles.burst(x + w / 2, y + h / 2, count / 2, 90f, 360f, 20f, 90f, 0.9f, 16f, DUST, false, -20f);
    }

    public void splash(float x, float y) {
        particles.burst(x, y, 10, 90f, 70f, 120f, 280f, 0.5f, 4f, DRIP, true, 900f);
    }

    public void lightBurst(float x, float y, Color c, int count) {
        particles.burst(x, y, count, 90f, 360f, 80f, 300f, 0.5f, 6f, c, true, 0f);
    }

    /** Continuous atmospheric particles around the camera, depending on the region. */
    public void ambient(Region region, float camX, float camY, float viewW, float viewH, float dt) {
        ambientAccumulator += dt * 22f;
        while (ambientAccumulator >= 1f) {
            ambientAccumulator -= 1f;
            float x = camX + rand(-viewW * 0.6f, viewW * 0.6f);
            float y = camY + rand(-viewH * 0.6f, viewH * 0.6f);
            switch (region) {
                case WHISPERING_ROOTS:
                    particles.spawnPhysical(x, y, rand(-8, 8), rand(8, 26), rand(3f, 6f), rand(2f, 4.5f), 0.5f,
                            SPORE, rand(0.3f, 0.8f), true, 0f, 0f, 14f);
                    break;
                case GILDED_CISTERN:
                    if (random.nextFloat() < 0.35f) {
                        particles.spawnPhysical(x, camY + viewH * 0.55f, 0, -rand(250, 420), 2.2f, 2.5f, 2f,
                                DRIP, 0.55f, true, 200f, 0f, 0f);
                    } else {
                        particles.spawnPhysical(x, y, rand(-12, 12), rand(-5, 12), rand(3f, 5f), rand(1.5f, 3.5f), 0.5f,
                                AMBER, rand(0.3f, 0.7f), true, 0f, 0f, 8f);
                    }
                    break;
                default:
                    particles.spawnPhysical(x, y, rand(10, 40), rand(-25, -5), rand(3f, 6f), rand(2f, 4f), 1f,
                            random.nextFloat() < 0.5f ? ASH : MIST, rand(0.25f, 0.7f), true, 0f, 0f, 20f);
                    break;
            }
        }
    }

    private float rand(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
