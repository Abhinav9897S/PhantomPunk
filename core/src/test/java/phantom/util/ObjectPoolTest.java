package phantom.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import phantom.entities.Projectile;
import phantom.systems.Particle;
import phantom.systems.ParticleSystem;

/** Generic object pool reuses instances (no garbage spikes). */
class ObjectPoolTest {
    @Test
    void reusesFreedObjects() {
        ObjectPool<Projectile> pool = new ObjectPool<>(Projectile::new, 8);
        Projectile a = pool.obtain();
        pool.free(a);
        Projectile b = pool.obtain();
        assertSame(a, b);
        assertEquals(1, pool.getCreatedCount());
    }

    @Test
    void particleBurstsRecycleThroughThePool() {
        ParticleSystem ps = new ParticleSystem(new java.util.Random(1));
        for (int round = 0; round < 5; round++) {
            ps.burst(0, 0, 200, 0, 360, 10, 50, 0.2f, 4, com.badlogic.gdx.graphics.Color.WHITE, true, 0);
            for (int i = 0; i < 40; i++) ps.update(1 / 60f);
        }
        assertEquals(0, ps.getLiveCount());
        assertEquals(true, ps.getPool().getCreatedCount() <= 220, "about one burst's worth of particles allocated");
        Particle p = ps.getPool().obtain();
        assertEquals(0f, p.getAlpha(), 1e-6);
    }
}
