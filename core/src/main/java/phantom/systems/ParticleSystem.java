package phantom.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import phantom.util.ObjectPool;

/**
 * Owns all live particles. New particles come from an {@link ObjectPool} so bursts of
 * hundreds of sparks create no garbage.
 * <p>OOP concept: composition (owned by the world) + <b>generics</b> ({@code ObjectPool<Particle>}).</p>
 */
public final class ParticleSystem {
    public static final int MAX_PARTICLES = 1800;

    private final ObjectPool<Particle> pool = new ObjectPool<>(Particle::new, MAX_PARTICLES);
    private final List<Particle> live = new ArrayList<>();
    private final Random random;
    private float time;

    public ParticleSystem(Random random) {
        this.random = random;
    }

    /** Spawns a single particle; returns null when the cap is reached. */
    public Particle spawn(float x, float y, float vx, float vy, float life, float size, float endSize,
                          Color c, float alpha, boolean additive) {
        if (live.size() >= MAX_PARTICLES) return null;
        Particle p = pool.obtain().set(x, y, vx, vy, life, size, endSize).color(c.r, c.g, c.b, alpha).additive(additive);
        live.add(p);
        return p;
    }

    /** Spawns a particle with gravity / drag / wobble. */
    public void spawnPhysical(float x, float y, float vx, float vy, float life, float size, float endSize,
                              Color c, float alpha, boolean additive, float gravity, float drag, float wobble) {
        Particle p = spawn(x, y, vx, vy, life, size, endSize, c, alpha, additive);
        if (p != null) p.physics(gravity, drag, wobble);
    }

    /** Radial burst in a cone ({@code angle} and {@code spread} in degrees). */
    public void burst(float x, float y, int count, float angle, float spread, float speedMin, float speedMax,
                      float life, float size, Color c, boolean additive, float gravity) {
        for (int i = 0; i < count; i++) {
            double a = Math.toRadians(angle + (random.nextFloat() - 0.5f) * spread);
            float speed = speedMin + random.nextFloat() * (speedMax - speedMin);
            float l = life * (0.6f + random.nextFloat() * 0.6f);
            spawnPhysical(x, y, (float) Math.cos(a) * speed, (float) Math.sin(a) * speed, l,
                    size * (0.6f + random.nextFloat() * 0.7f), 0f, c, 1f, additive, gravity, 2.5f, 0f);
        }
    }

    public void update(float dt) {
        time += dt;
        for (int i = live.size() - 1; i >= 0; i--) {
            Particle p = live.get(i);
            if (!p.update(dt, time)) {
                // swap-remove keeps this O(1)
                Collections.swap(live, i, live.size() - 1);
                live.remove(live.size() - 1);
                pool.free(p);
            }
        }
    }

    public void clear() {
        for (Particle p : live) pool.free(p);
        live.clear();
    }

    /** Read-only view for the renderer. */
    public List<Particle> getLive() { return Collections.unmodifiableList(live); }
    public int getLiveCount() { return live.size(); }
    public Random getRandom() { return random; }
    public ObjectPool<Particle> getPool() { return pool; }
}
