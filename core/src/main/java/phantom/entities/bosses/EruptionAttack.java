package phantom.entities.bosses;

import phantom.audio.SoundId;
import phantom.entities.props.GroundEruption;
import phantom.events.Events;
import phantom.world.WorldContext;

/**
 * Marks spots on the floor around the player that erupt a moment later (roots, steam or mist).
 * <p>OOP concept: {@link BossAttack} that creates other objects ({@link GroundEruption}).</p>
 */
public final class EruptionAttack implements BossAttack {
    private final int count;
    private final float spacing;
    private float time;

    public EruptionAttack(int count, float spacing) {
        this.count = count;
        this.spacing = spacing;
    }

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        float px = world.getPlayer().centerX();
        float floorY = world.getPlayer().isOnGround() ? world.getPlayer().getY() : boss.getY();
        float start = px - spacing * (count - 1) / 2f;
        for (int i = 0; i < count; i++) {
            float x = start + spacing * i;
            if (x < 40 || x > world.getTileMap().widthPx() - 40) continue;
            world.spawn(new GroundEruption(x, GroundEruption.findFloor(world.getTileMap(), x, floorY + 8f), 0.75f + i * 0.06f));
        }
        world.getEvents().publish(new Events.Sfx(SoundId.BOSS_ROAR));
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        boss.setVelocityX(0);
        boss.stepPhysics(world, dt);
        return time > 1.2f;
    }

    @Override
    public String getPose() {
        return "cast";
    }

    @Override
    public boolean isTelegraphing() {
        return time < 0.75f;
    }
}
