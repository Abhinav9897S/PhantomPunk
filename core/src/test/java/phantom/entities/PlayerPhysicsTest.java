package phantom.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.abilities.AbilityType;
import phantom.entities.player.Player;
import phantom.input.GameAction;
import phantom.sim.TestWorlds;
import phantom.sim.TestWorlds.Harness;
import phantom.util.Constants;

/**
 * Jump physics: variable height, coyote time, buffering, and the jump/gap sizes the
 * level validator assumes (3-tile ledges yes, 5-tile no; 6-tile gaps yes, 9-tile gaps need Dash).
 */
class PlayerPhysicsTest {
    private static final int T = Constants.TILE;

    private static String row(int width, char fill, char edge) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < width; i++) b.append(i == 0 || i == width - 1 ? edge : fill);
        return b.toString();
    }

    /** Flat room 40x16 with the player on the floor at column 3. */
    private static Harness flat() {
        String[] rows = new String[16];
        rows[0] = row(40, '#', '#');
        for (int i = 1; i < 15; i++) rows[i] = row(40, '.', '#');
        rows[14] = "#..P" + row(36, '.', '#').substring(0, 35) + "#";
        rows[15] = row(40, '#', '#');
        return TestWorlds.harness(rows);
    }

    private static float peakHeight(Harness h, int holdTicks) {
        Player p = h.world.getPlayer();
        h.seconds(0.3f);
        float ground = p.getY();
        h.input.press(GameAction.JUMP);
        float peak = ground;
        for (int i = 0; i < 90; i++) {
            if (i == holdTicks) h.input.release(GameAction.JUMP);
            h.tick(1);
            peak = Math.max(peak, p.getY());
        }
        h.input.releaseAll();
        return peak - ground;
    }

    @Test
    void fullJumpReachesJustUnderFourTiles() {
        float height = peakHeight(flat(), 60);
        float expected = Constants.JUMP_VELOCITY * Constants.JUMP_VELOCITY / (2 * Constants.GRAVITY);
        assertEquals(expected, height, 12f);
        assertTrue(height > 3 * T && height < 4 * T, "height " + height);
    }

    @Test
    void releasingEarlyGivesALowerJump() {
        float tap = peakHeight(flat(), 3);
        float full = peakHeight(flat(), 60);
        assertTrue(tap < full * 0.5f, "tap " + tap + " vs full " + full);
    }

    @Test
    void coyoteTimeAllowsAJumpJustAfterLeavingALedge() {
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
                "#######......................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "##############################");
        Player p = h.world.getPlayer();
        h.seconds(0.2f);
        h.input.press(GameAction.RIGHT);
        int ticks = 0;
        while (p.isOnGround() && ticks++ < 200) h.tick(1);
        h.tick(3); // already off the ledge (about 0.05 s)
        assertFalse(p.isOnGround());
        h.input.press(GameAction.JUMP);
        h.tick(1);
        assertTrue(p.getVelocityY() > Constants.JUMP_VELOCITY * 0.9f, "coyote jump fired");
    }

    @Test
    void jumpPressedJustBeforeLandingIsBuffered() {
        Harness h = flat();
        Player p = h.world.getPlayer();
        h.seconds(0.3f);
        float ground = p.getY();
        h.input.press(GameAction.JUMP);
        int guard = 0;
        do {
            h.tick(1);
        } while (p.getVelocityY() > 0 && guard++ < 300); // hold until the apex: no jump cut
        h.input.release(GameAction.JUMP);
        while (p.getY() > ground + 40f && guard++ < 300) h.tick(1);
        assertFalse(p.isOnGround(), "still airborne when the button is pressed");
        h.input.press(GameAction.JUMP);
        boolean jumpedAgain = false;
        for (int i = 0; i < 12; i++) {
            h.tick(1);
            if (p.getVelocityY() > Constants.JUMP_VELOCITY * 0.8f) jumpedAgain = true;
        }
        assertTrue(jumpedAgain, "buffered jump fired on landing");
    }

    /** Runs right from column 3 holding jump at the edge; returns the furthest column landed on. */
    private static float tryGap(int gapTiles, boolean dash) {
        String[] rows = new String[16];
        rows[15] = "#".repeat(60);
        for (int i = 0; i < 15; i++) rows[i] = "#" + ".".repeat(58) + "#";
        rows[0] = "#".repeat(60);
        String floor = "#" + "#".repeat(12) + "^".repeat(gapTiles) + "#".repeat(60 - 13 - gapTiles - 1) + "#";
        rows[13] = floor;
        rows[14] = "#".repeat(60);
        rows[12] = "#..P" + ".".repeat(55) + "#";
        Harness h = TestWorlds.harness(rows);
        Player p = h.world.getPlayer();
        if (dash) p.getAbilities().unlock(AbilityType.DASH);
        h.seconds(0.2f);
        h.input.press(GameAction.RIGHT);
        int edge = 13 * T;
        int guard = 0;
        while (p.getX() + p.getWidth() < edge - 1 && guard++ < 400) h.tick(1);
        h.input.press(GameAction.JUMP);
        boolean dashed = false;
        for (int i = 0; i < 90; i++) {
            h.tick(1);
            if (dash && !dashed && p.getVelocityY() < 50f) {
                h.input.press(GameAction.DASH);
                dashed = true;
            } else if (dashed) {
                h.input.release(GameAction.DASH);
            }
            if (p.getHealth().getCurrent() < p.getHealth().getMax()) return -1f;
            if (p.isOnGround() && i > 5) break;
        }
        return p.getX() / T;
    }

    @Test
    void sixTileGapIsJumpable() {
        assertTrue(tryGap(6, false) > 18, "landed beyond a 6-tile gap");
    }

    @Test
    void nineTileGapNeedsTheDash() {
        assertEquals(-1f, tryGap(9, false), "a plain jump falls into the spikes");
        assertTrue(tryGap(9, true) > 21, "jump + dash clears 9 tiles");
    }

    private static boolean canClimbLedge(int height, boolean doubleJump) {
        String[] rows = new String[18];
        rows[0] = "#".repeat(30);
        for (int i = 1; i < 18; i++) rows[i] = "#" + ".".repeat(28) + "#";
        rows[17] = "#".repeat(30);
        // floor at the bottom; ledge of `height` tiles starting at column 12
        for (int k = 0; k < height; k++) {
            rows[16 - k] = "#" + ".".repeat(11) + "#".repeat(17) + "#";
        }
        rows[16 - height] = "#..P" + ".".repeat(25) + "#";
        if (height == 0) return true;
        Harness h = TestWorlds.harness(rows);
        Player p = h.world.getPlayer();
        if (doubleJump) p.getAbilities().unlock(AbilityType.DOUBLE_JUMP);
        p.enterAt(4 * T, T);
        h.seconds(0.3f);
        h.input.press(GameAction.RIGHT);
        int guard = 0;
        while (p.getX() + p.getWidth() < 12 * T - 24 && guard++ < 300) h.tick(1);
        h.input.press(GameAction.JUMP);
        boolean second = false;
        for (int i = 0; i < 120; i++) {
            h.tick(1);
            if (doubleJump && !second && p.getVelocityY() <= 0) {
                h.input.release(GameAction.JUMP);
                h.tick(1);
                h.input.press(GameAction.JUMP);
                second = true;
            }
        }
        return p.getY() >= (1 + height) * T - 1 && p.getX() > 12 * T;
    }

    @Test
    void threeTileLedgeYesFiveTileLedgeNo() {
        assertTrue(canClimbLedge(3, false));
        assertFalse(canClimbLedge(5, false));
    }

    @Test
    void wispLeapClimbsSixTileLedges() {
        assertFalse(canClimbLedge(6, false));
        assertTrue(canClimbLedge(6, true));
    }

    @Test
    void wallClingSlowsTheFallAndWallJumpLeapsAway() {
        Harness h = TestWorlds.harness(
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
                "#............................#",
                "#..........P.................#",
                "##############################");
        Player p = h.world.getPlayer();
        p.getAbilities().unlock(AbilityType.WALL_CLING);
        p.enterAt(2 * T, 9 * T);
        h.input.press(GameAction.LEFT);
        h.tick(20);
        assertTrue(p.isClinging(), "clinging to the left wall");
        assertTrue(p.getVelocityY() >= -Constants.WALL_SLIDE_SPEED - 1f, "sliding slowly");
        h.input.press(GameAction.JUMP);
        h.tick(1);
        assertTrue(p.getVelocityX() > 0 && p.getVelocityY() > 500f, "wall jump pushes right and up");
    }
}
