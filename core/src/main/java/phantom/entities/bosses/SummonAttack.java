package phantom.entities.bosses;

import phantom.entities.enemies.Flutterer;
import phantom.world.WorldContext;

/**
 * Calls a few Duskmoths to the fight (capped so the screen never floods).
 * <p>OOP concept: {@link BossAttack} that creates enemies.</p>
 */
public final class SummonAttack implements BossAttack {
    private static final int MAX_ALIVE = 3;
    private final int count;
    private float time;

    public SummonAttack(int count) {
        this.count = count;
    }

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        int alive = world.getEntities().getAll(Flutterer.class).size();
        for (int i = 0; i < count && alive < MAX_ALIVE; i++, alive++) {
            float x = boss.centerX() + (i % 2 == 0 ? -1 : 1) * (90f + 40f * i);
            float y = boss.getY() + boss.getHeight() * 0.5f;
            world.spawn(new Flutterer(x, y));
            world.getEffects().lightBurst(x, y + 50, com.badlogic.gdx.graphics.Color.WHITE, 14);
        }
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        boss.setVelocity(0, 0);
        return time > 0.9f;
    }

    @Override
    public String getPose() {
        return "cast";
    }
}
