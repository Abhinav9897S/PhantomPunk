package phantom.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.components.HealthComponent;
import phantom.entities.enemies.Crawler;
import phantom.entities.player.Player;
import phantom.entities.props.BreakableWall;
import phantom.input.GameAction;
import phantom.sim.TestWorlds;
import phantom.sim.TestWorlds.Harness;
import phantom.util.Constants;
import phantom.world.TileType;

/** Damage, invincibility frames, slashing, soul gain, shard drops, pogo and breakable walls. */
class CombatTest {
    private static final int T = Constants.TILE;

    @Test
    void healthComponentRespectsInvincibility() {
        HealthComponent h = new HealthComponent(5);
        assertTrue(h.damage(1, 1f));
        assertEquals(4, h.getCurrent());
        assertFalse(h.damage(1, 1f), "invincible right after a hit");
        h.update(1.01f);
        assertTrue(h.damage(2, 0f));
        assertEquals(2, h.getCurrent());
        h.heal(10);
        assertEquals(5, h.getCurrent(), "healing is capped");
        assertTrue(h.damage(9, 0f));
        assertTrue(h.isDead());
        h.heal(3);
        assertTrue(h.isDead(), "the dead are not healed");
    }

    private static Harness arena(String spawnRow) {
        return TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                spawnRow,
                "##############################");
    }

    @Test
    void touchingAnEnemyHurtsOnceThenGrantsInvincibility() {
        Harness h = arena(TestWorlds.line(".....c.P"));
        Player p = h.world.getPlayer();
        h.seconds(1.5f);
        assertEquals(p.getHealth().getMax() - 1, p.getHealth().getCurrent(), "one mask lost");
        assertTrue(p.getHealth().isInvincible() || p.getHealth().getCurrent() == p.getHealth().getMax() - 2);
    }

    @Test
    void threeSlashesKillACrawlerAndPayOut() {
        Harness h = arena(TestWorlds.line("....P..c"));
        Player p = h.world.getPlayer();
        Crawler crawler = h.world.getEntities().first(Crawler.class);
        h.tick(1);
        crawler = h.world.getEntities().first(Crawler.class);
        p.getHealth().makeInvincible(100f);
        for (int i = 0; i < 12 && crawler.isAlive(); i++) {
            p.setFacing(crawler.centerX() > p.centerX() ? phantom.util.Direction.RIGHT : phantom.util.Direction.LEFT);
            h.input.press(GameAction.ATTACK);
            h.tick(1);
            h.input.release(GameAction.ATTACK);
            h.seconds(Constants.ATTACK_COOLDOWN + 0.02f);
            p.teleport(crawler.centerX() - 50f, p.getY());
        }
        assertFalse(crawler.isAlive(), "crawler died");
        assertEquals(3 * Constants.SOUL_PER_HIT, p.getSoul().getAmount(), "light gained per hit");
        h.seconds(0.1f);
        assertFalse(h.world.getEntities().getAll(ShardPickup.class).isEmpty(), "shards dropped");
        assertEquals(1, h.world.getProgress().getEnemiesKilled());
    }

    @Test
    void downSlashOnSpikesBouncesThePlayer() {
        Harness h = TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#..P.........................#",
                "#^^^^^^^^^^^^^^^^^^^^^^^^^^^^#",
                "##############################");
        Player p = h.world.getPlayer();
        p.enterAt(10 * T, 5 * T);
        h.input.press(GameAction.DOWN);
        boolean bounced = false;
        for (int i = 0; i < 60 && !bounced; i++) {
            if (p.getY() < 3 * T) h.input.press(GameAction.ATTACK);
            h.tick(1);
            h.input.release(GameAction.ATTACK);
            if (p.getVelocityY() > Constants.POGO_VELOCITY * 0.8f) bounced = true;
        }
        assertTrue(bounced, "pogo bounce");
        assertEquals(p.getHealth().getMax(), p.getHealth().getCurrent(), "no spike damage");
    }

    @Test
    void breakableWallsCrumbleAfterThreeHits() {
        Harness h = arena(TestWorlds.line("......P.BB"));
        Player p = h.world.getPlayer();
        BreakableWall wall = h.world.getEntities().first(BreakableWall.class);
        h.tick(1);
        wall = h.world.getEntities().first(BreakableWall.class);
        assertTrue(wall != null);
        for (int i = 0; i < 3; i++) {
            p.setFacing(phantom.util.Direction.RIGHT);
            h.input.press(GameAction.ATTACK);
            h.tick(1);
            h.input.release(GameAction.ATTACK);
            h.seconds(0.5f); // cooldown plus the hit-stop pause
        }
        assertFalse(wall.isAlive());
        assertEquals(TileType.EMPTY, h.world.getTileMap().get(9, 1));
        assertTrue(h.world.getProgress().isWallBroken("test:wall:9:1"));
        assertEquals(0, p.getSoul().getAmount(), "walls give no light");
    }

    @Test
    void lightBoltCostsSoulAndHitsEnemies() {
        Harness h = arena(TestWorlds.line("..P...............c"));
        Player p = h.world.getPlayer();
        p.getAbilities().unlock(phantom.abilities.AbilityType.LIGHT_BOLT);
        p.getSoul().fill();
        p.setFacing(phantom.util.Direction.RIGHT);
        int before = p.getSoul().getAmount();
        h.input.press(GameAction.BOLT);
        h.tick(1);
        h.input.release(GameAction.BOLT);
        assertEquals(before - Constants.SOUL_COST, p.getSoul().getAmount());
        assertEquals(1, h.world.getEntities().getAll(Projectile.class).size());
        h.seconds(1.2f);
        Crawler c = h.world.getEntities().first(Crawler.class);
        assertTrue(c == null || c.getHealth().getCurrent() < c.getHealth().getMax(), "bolt connected");
    }
}
