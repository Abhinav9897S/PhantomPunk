package phantom.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.abilities.AbilityType;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.save.SaveData;
import phantom.util.Constants;
import phantom.world.GameWorld;

/**
 * Plays the three ability gates of the real map with scripted input and real physics:
 * the dash gap in Cistern Steps, the wall-climb shaft in the Drowned Chapel and the
 * first floating platform of the Mist Verge.
 */
class GatingTraversalTest {
    private static final int T = Constants.TILE;

    private static final class Run {
        final ScriptedInput in = new ScriptedInput();
        final GameWorld world;
        final List<String> rooms = new ArrayList<>();

        Run(AbilityType... abilities) throws Exception {
            SaveData save = new SaveData();
            List<String> names = new ArrayList<>();
            for (AbilityType a : abilities) names.add(a.name());
            save.setAbilities(names);
            EventBus bus = new EventBus();
            bus.subscribe(Events.RoomEntered.class, e -> rooms.add(e.roomId()));
            world = new GameWorld(TestAssets.loadWorld(), bus, in, save);
            world.start();
        }

        void tick(int n) {
            for (int i = 0; i < n; i++) {
                in.poll();
                world.update(Constants.STEP);
            }
        }

        Player p() { return world.getPlayer(); }
    }

    /** Runs right, jumps at the ledge and dashes at the top of the arc. */
    private static void dashJumpRight(Run run, float edgeX, boolean dash) {
        run.in.press(GameAction.RIGHT);
        int guard = 0;
        while (run.p().getX() + run.p().getWidth() < edgeX - 2 && guard++ < 600) run.tick(1);
        run.in.press(GameAction.JUMP);
        boolean dashed = false;
        for (int i = 0; i < 70; i++) {
            run.tick(1);
            if (dash && !dashed && run.p().getVelocityY() < 120f) {
                run.in.press(GameAction.DASH);
                dashed = true;
            } else {
                run.in.release(GameAction.DASH);
            }
        }
        run.in.releaseAll();
        run.tick(30);
    }

    @Test
    void dashCrossesTheCisternGapButAPlainJumpDoesNot() throws Exception {
        Run without = new Run(AbilityType.FOCUS);
        without.world.enterRoom("r2_steps", "west");
        dashJumpRight(without, 9 * T, false);
        assertTrue(without.p().getX() < 9 * T, "fell back to the near side (hazard respawn)");
        assertTrue(without.p().getHealth().getCurrent() < without.p().getHealth().getMax(), "the spikes hurt");

        Run with = new Run(AbilityType.FOCUS, AbilityType.DASH);
        with.world.enterRoom("r2_steps", "west");
        dashJumpRight(with, 9 * T, true);
        assertTrue(with.p().getX() > 18 * T, "landed across the gap at x=" + with.p().getX() / T);
        assertEquals(with.p().getHealth().getMax(), with.p().getHealth().getCurrent());
    }

    /** Zig-zags up between two walls: cling, wall jump towards the other wall, cling, repeat. */
    private static float zigZagClimb(Run run, float targetY, int maxJumps) {
        Player p = run.p();
        GameAction toward = GameAction.RIGHT;
        float best = p.getY();
        for (int jump = 0; jump < maxJumps; jump++) {
            GameAction away = toward == GameAction.RIGHT ? GameAction.LEFT : GameAction.RIGHT;
            int guard = 0;
            run.in.release(away);
            run.in.press(toward);
            while (!p.isClinging() && !(p.isOnGround() && guard > 3) && guard++ < 45) run.tick(1);
            if (!p.isClinging() && !p.isOnGround()) break; // landed nowhere useful: give up
            if (toward == GameAction.LEFT && p.getY() >= targetY - 2 * T) {
                // high enough on the left wall: leap right towards the upper exit ledge
                run.in.release(GameAction.JUMP);
                run.tick(2);
                run.in.press(GameAction.JUMP);
                run.tick(1);
                run.in.release(GameAction.LEFT);
                run.in.press(GameAction.RIGHT);
                run.tick(40);
                best = Math.max(best, p.getY());
                run.in.release(GameAction.JUMP);
                return best;
            }
            run.in.release(GameAction.JUMP);
            run.tick(2);
            run.in.press(GameAction.JUMP);
            run.tick(1);
            run.in.release(toward);
            run.in.press(away);
            guard = 0;
            while (p.getVelocityY() > 0 && guard++ < 60) run.tick(1);
            best = Math.max(best, p.getY());
            toward = away;
            p.getHealth().makeInvincible(1000f);
        }
        run.in.releaseAll();
        return best;
    }

    @Test
    void mossGripClimbsTheChapelShaft() throws Exception {
        Run run = new Run(AbilityType.FOCUS, AbilityType.DASH, AbilityType.WALL_CLING);
        run.world.enterRoom("r2_chapel", "west");
        Player p = run.p();
        p.enterAt(33 * T, T); // bottom of the shaft, beside the step platform
        p.getHealth().makeInvincible(1000f);
        run.tick(10);
        run.in.press(GameAction.JUMP);
        run.in.press(GameAction.RIGHT);
        run.tick(20);
        float best = zigZagClimb(run, 26 * T, 30);
        assertTrue(best >= 25 * T, "climbed to row " + best / T);
        run.in.press(GameAction.RIGHT); // walk along the upper ledge to the door
        run.tick(150);
        assertTrue(run.rooms.contains("r2_gears"), "reached the Gearworks Shaft; rooms " + run.rooms + ", at " + p.getHitbox());
    }

    @Test
    void mossGripClimbsTheGearworksShaft() throws Exception {
        Run run = new Run(AbilityType.FOCUS, AbilityType.DASH, AbilityType.WALL_CLING);
        run.world.enterRoom("r2_gears", "west");
        for (phantom.entities.enemies.Enemy e : run.world.getEntities().getAll(phantom.entities.enemies.Enemy.class)) e.remove();
        Player p = run.p();
        p.enterAt(15 * T, T);
        p.getHealth().makeInvincible(1000f);
        run.tick(10);
        run.in.press(GameAction.JUMP);
        run.in.press(GameAction.RIGHT);
        run.tick(20);
        float best = zigZagClimb(run, 35 * T, 40);
        assertTrue(best >= 34 * T, "climbed to row " + best / T);
        run.in.press(GameAction.RIGHT);
        run.tick(240);
        assertTrue(run.rooms.contains("r2_boss"), "reached the Warden's Gallery; rooms " + run.rooms + ", at " + p.getHitbox());
    }

    @Test
    void withoutMossGripTheShaftCannotBeClimbed() throws Exception {
        Run run = new Run(AbilityType.FOCUS, AbilityType.DASH);
        run.world.enterRoom("r2_chapel", "west");
        Player p = run.p();
        p.enterAt(33 * T, T);
        run.tick(10);
        run.in.press(GameAction.JUMP);
        run.in.press(GameAction.RIGHT);
        run.tick(20);
        float best = zigZagClimb(run, 31 * T, 30);
        assertTrue(best < 10 * T, "no climbing without the ability, best row " + best / T);
    }

    @Test
    void wispLeapReachesTheFirstVergePlatform() throws Exception {
        for (boolean doubleJump : new boolean[] {false, true}) {
            Run run = doubleJump ? new Run(AbilityType.FOCUS, AbilityType.DOUBLE_JUMP) : new Run(AbilityType.FOCUS);
            run.world.enterRoom("r3_verge", "west");
            Player p = run.p();
            p.getHealth().makeInvincible(1000f);
            run.in.press(GameAction.RIGHT);
            int guard = 0;
            while (p.getX() + p.getWidth() < 9 * T - 4 && guard++ < 600) run.tick(1);
            run.in.press(GameAction.JUMP);
            boolean second = false;
            float best = 0;
            for (int i = 0; i < 80; i++) {
                run.tick(1);
                if (p.getX() > 13 * T) run.in.release(GameAction.RIGHT);
                if (doubleJump && !second && p.getVelocityY() < 60f) {
                    run.in.release(GameAction.JUMP);
                    run.tick(1);
                    run.in.press(GameAction.JUMP);
                    second = true;
                }
                if (p.isOnGround()) best = Math.max(best, p.getY());
            }
            boolean onPlatform = best >= 10 * T - 1;
            assertEquals(doubleJump, onPlatform, (doubleJump ? "with" : "without") + " the Wisp Leap, best floor " + best / T);
        }
    }
}
