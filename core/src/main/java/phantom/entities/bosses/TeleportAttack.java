package phantom.entities.bosses;

import phantom.world.WorldContext;

/**
 * The boss dissolves and reappears on the far side of the player.
 * <p>OOP concept: {@link BossAttack} that manipulates the boss's fade value.</p>
 */
public final class TeleportAttack implements BossAttack {
    private static final float OUT = 0.45f;
    private static final float IN = 0.45f;
    private float time;
    private boolean moved;

    @Override
    public void begin(Boss boss, WorldContext world) {
        time = 0;
        moved = false;
    }

    @Override
    public boolean update(Boss boss, WorldContext world, float dt) {
        time += dt;
        boss.setVelocity(0, 0);
        if (time < OUT) {
            boss.setFade(1f - time / OUT);
            return false;
        }
        if (!moved) {
            moved = true;
            float roomW = world.getTileMap().widthPx();
            float px = world.getPlayer().centerX();
            float targetX = px < roomW / 2 ? Math.min(roomW - 220, px + 360) : Math.max(220, px - 360);
            boss.teleport(targetX - boss.getWidth() / 2, boss.getY());
            world.getEffects().lightBurst(boss.centerX(), boss.centerY(), com.badlogic.gdx.graphics.Color.WHITE, 20);
        }
        boss.setFade((time - OUT) / IN);
        if (time >= OUT + IN) {
            boss.setFade(1f);
            return true;
        }
        return false;
    }

    @Override
    public String getPose() {
        return "cast";
    }

    @Override
    public boolean isTelegraphing() {
        return true;
    }
}
