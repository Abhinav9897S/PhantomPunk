package phantom.entities.bosses;

import phantom.entities.props.FallingRock;
import phantom.world.WorldContext;

/**
 * The boss roars and rocks fall from the ceiling around the player.
 * <p>OOP concept: {@link BossAttack} that reuses the room hazard {@link FallingRock}.</p>
 */
public final class DebrisRainAttack implements BossAttack {
    private final int count;
    private float time;
    private int dropped;

    public DebrisRainAttack(int count) {
        this.count = count;
    }

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        dropped = 0;
        world.shake(0.5f);
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        boss.setVelocityX(0);
        boss.stepPhysics(world, dt);
        float interval = 0.22f;
        if (dropped < count && time > 0.4f + dropped * interval) {
            float px = world.getPlayer().centerX();
            float x = px + (world.getRandom().nextFloat() - 0.5f) * 420f;
            x = Math.max(48, Math.min(world.getTileMap().widthPx() - 80, x));
            world.spawn(FallingRock.debris(world.getTileMap(), x, world.getPlayer().getY()));
            dropped++;
        }
        return dropped >= count && time > 0.4f + count * interval + 0.6f;
    }

    @Override
    public String getPose() {
        return "roar";
    }

    @Override
    public boolean isTelegraphing() {
        return time < 0.4f;
    }
}
