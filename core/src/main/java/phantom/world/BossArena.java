package phantom.world;

import java.util.List;

import phantom.abilities.AbilityType;
import phantom.entities.bosses.Boss;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.util.Constants;

/**
 * Locks a boss room when the player walks in, wakes the boss after its title card,
 * and reopens the gates (granting any reward ability) once it falls.
 * <p>OOP concept: a focused helper owned by {@link GameWorld} (composition / single responsibility).</p>
 */
public final class BossArena {
    private static final float INTRO_TIME = 1.6f;
    private final Boss boss;
    private final AbilityType reward;
    private boolean engaged;
    private boolean finished;
    private float introTimer;

    public BossArena(Boss boss, AbilityType reward) {
        this.boss = boss;
        this.reward = reward;
    }

    public void update(WorldContext world, float dt) {
        if (finished) return;
        Player p = world.getPlayer();
        TileMap map = world.getTileMap();
        if (!engaged) {
            float margin = 6 * Constants.TILE;
            if (p.getX() > margin && p.getX() < map.widthPx() - margin && p.isOnGround()) engage(world);
            return;
        }
        if (!boss.isAwake()) {
            introTimer -= dt;
            if (introTimer <= 0) boss.awaken(world);
        }
    }

    private void engage(WorldContext world) {
        engaged = true;
        introTimer = INTRO_TIME;
        world.getTileMap().setGatesClosed(true);
        world.shake(0.3f);
        world.getEvents().publish(new Events.BossEncounter(boss.getTitle(), boss.getSubtitle()));
    }

    /** Called when the boss's defeat event arrives. */
    public void onDefeated(WorldContext world) {
        if (finished) return;
        finished = true;
        world.getTileMap().setGatesClosed(false);
        if (reward != null && world.getPlayer().getAbilities().unlock(reward)) {
            world.getEvents().publish(new Events.AbilityUnlocked(reward));
            world.getEvents().publish(new Events.DialogueRequest(reward.getDisplayName(),
                    List.of("As the guardian falls, its stolen light flows back into your lantern.", reward.getHint()),
                    List.of(), null));
        }
    }

    /** The boss whose health bar should be shown, or null. */
    public Boss getActiveBoss() {
        return engaged && !finished && boss.isAwake() ? boss : null;
    }

    public Boss getBoss() { return boss; }
    public boolean isEngaged() { return engaged; }
    public boolean isFinished() { return finished; }
}
