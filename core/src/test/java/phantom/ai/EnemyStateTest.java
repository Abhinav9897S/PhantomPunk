package phantom.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.entities.Projectile;
import phantom.entities.enemies.Brute;
import phantom.entities.enemies.Crawler;
import phantom.entities.enemies.Flutterer;
import phantom.entities.enemies.Sentinel;
import phantom.sim.TestWorlds;
import phantom.sim.TestWorlds.Harness;
import phantom.util.Constants;

/** Enemy state machines change state for the right reasons. */
class EnemyStateTest {
    private static final int T = Constants.TILE;

    @Test
    void stateMachineTracksTimeAndPreviousState() {
        StateMachine<String> sm = new StateMachine<>("owner", EnemyState.IDLE);
        int[] entered = {0};
        sm.on(EnemyState.ATTACK, new EnemyBehaviour<>() {
            @Override public void update(String o, phantom.world.WorldContext w, float dt) { }
            @Override public void enter(String o, phantom.world.WorldContext w) { entered[0]++; }
        });
        sm.update(null, 0.5f);
        assertEquals(0.5f, sm.getTimeInState(), 1e-6);
        sm.change(EnemyState.ATTACK);
        assertTrue(sm.is(EnemyState.ATTACK));
        assertEquals(EnemyState.IDLE, sm.getPrevious());
        sm.update(null, 0.1f);
        sm.update(null, 0.1f);
        assertEquals(1, entered[0], "enter runs once");
        assertEquals(0.2f, sm.getTimeInState(), 1e-6);
    }

    @Test
    void crawlerPatrolsWithoutFallingOffItsPlatform() {
        Harness h = TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#.......c....................#",
                "#.....#######................#",
                "#............................#",
                "#............................#",
                "#..........................P.#",
                "##############################");
        Crawler c = h.world.getEntities().first(Crawler.class);
        h.tick(1);
        c = h.world.getEntities().first(Crawler.class);
        float y = c.getY();
        boolean turned = false;
        phantom.util.Direction start = c.getFacing();
        for (int i = 0; i < 600; i++) {
            h.tick(1);
            if (c.getFacing() != start) turned = true;
        }
        assertTrue(turned, "turned at a ledge");
        assertEquals(y, c.getY(), 0.5f, "still on the platform");
        assertEquals(EnemyState.PATROL, c.getAi().getState());
    }

    @Test
    void duskmothNoticesThenDivesAtThePlayer() {
        Harness h = TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#..........f.................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#..............P.............#",
                "##############################");
        h.world.getPlayer().getHealth().makeInvincible(100f);
        boolean alerted = false;
        boolean dived = false;
        for (int i = 0; i < 240; i++) {
            h.tick(1);
            Flutterer f = h.world.getEntities().first(Flutterer.class);
            if (f.getAi().is(EnemyState.ALERT)) alerted = true;
            if (f.getAi().is(EnemyState.ATTACK)) dived = true;
        }
        assertTrue(alerted && dived);
    }

    @Test
    void sentinelChargesAndFires() {
        Harness h = TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#..s..........P..............#",
                "##############################");
        h.world.getPlayer().getHealth().makeInvincible(100f);
        boolean fired = false;
        for (int i = 0; i < 120 && !fired; i++) {
            h.tick(1);
            fired = !h.world.getEntities().getAll(Projectile.class).isEmpty();
        }
        assertTrue(fired, "an orb was fired");
        assertEquals(EnemyState.RECOVER, h.world.getEntities().first(Sentinel.class).getAi().getState());
    }

    @Test
    void sentinelIgnoresPlayerBehindWalls() {
        Harness h = TestWorlds.harness(
                "##############################",
                "#............................#",
                "#..........#.................#",
                "#..........#.................#",
                "#..........#.................#",
                "#..........#.................#",
                "#..........#.................#",
                "#..s.......#......P..........#",
                "##############################");
        h.seconds(2f);
        assertTrue(h.world.getEntities().getAll(Projectile.class).isEmpty());
        assertEquals(EnemyState.IDLE, h.world.getEntities().first(Sentinel.class).getAi().getState());
    }

    @Test
    void bruteChargesIntoTheWallAndIsStaggered() {
        Harness h = TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#.............P.......b......#",
                "##############################");
        h.world.getPlayer().getHealth().makeInvincible(100f);
        boolean charged = false;
        boolean staggered = false;
        for (int i = 0; i < 400; i++) {
            h.tick(1);
            Brute b = h.world.getEntities().first(Brute.class);
            if (b.getAi().is(EnemyState.ATTACK)) charged = true;
            if (charged && b.isStaggered()) staggered = true;
        }
        assertTrue(charged, "brute charged");
        assertTrue(staggered, "brute was staggered by the wall");
        assertFalse(h.world.getPlayer().isDead());
    }
}
