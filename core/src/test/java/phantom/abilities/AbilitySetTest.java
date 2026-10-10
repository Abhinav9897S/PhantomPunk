package phantom.abilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.components.AbilitySet;
import phantom.entities.player.Player;
import phantom.input.GameAction;
import phantom.sim.TestWorlds;
import phantom.sim.TestWorlds.Harness;
import phantom.util.Constants;

/** Abilities are gated: they do nothing until unlocked, then work through the strategy objects. */
class AbilitySetTest {
    private static Harness room() {
        return TestWorlds.harness(
                "##############################",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#............................#",
                "#..P.........................#",
                "##############################");
    }

    @Test
    void onlyFocusIsKnownAtTheStart() {
        AbilitySet set = new AbilitySet();
        assertTrue(set.has(AbilityType.FOCUS));
        for (AbilityType t : AbilityType.values()) {
            if (t != AbilityType.FOCUS) assertFalse(set.has(t), t.name());
        }
        assertTrue(set.unlock(AbilityType.DASH));
        assertFalse(set.unlock(AbilityType.DASH), "second unlock is not new");
        assertEquals(2, set.active().size());
    }

    @Test
    void dashDoesNothingUntilUnlocked() {
        Harness h = room();
        Player p = h.world.getPlayer();
        h.seconds(0.2f);
        h.input.press(GameAction.DASH);
        h.tick(2);
        assertTrue(Math.abs(p.getVelocityX()) < 1f, "locked dash ignored");
        h.input.release(GameAction.DASH);
        p.getAbilities().unlock(AbilityType.DASH);
        h.tick(2);
        h.input.press(GameAction.DASH);
        h.tick(2);
        assertEquals(Constants.DASH_SPEED, p.getVelocityX(), 1f);
    }

    @Test
    void focusTurnsLightIntoHealth() {
        Harness h = room();
        Player p = h.world.getPlayer();
        h.seconds(0.2f);
        p.getHealth().damage(2, 0f);
        p.getSoul().fill();
        h.input.press(GameAction.FOCUS);
        h.seconds(Constants.FOCUS_TIME + 0.1f);
        assertEquals(p.getHealth().getMax() - 1, p.getHealth().getCurrent());
        assertEquals(Constants.SOUL_MAX - Constants.SOUL_COST, p.getSoul().getAmount());
    }

    @Test
    void doubleJumpWorksOncePerAirtime() {
        Harness h = room();
        Player p = h.world.getPlayer();
        p.getAbilities().unlock(AbilityType.DOUBLE_JUMP);
        h.seconds(0.2f);
        h.input.press(GameAction.JUMP);
        h.tick(10);
        h.input.release(GameAction.JUMP);
        h.tick(1);
        h.input.press(GameAction.JUMP);
        h.tick(1);
        assertEquals(Constants.DOUBLE_JUMP_VELOCITY, p.getVelocityY(), Constants.GRAVITY / 60f + 1f);
        h.input.release(GameAction.JUMP);
        h.tick(1);
        h.input.press(GameAction.JUMP);
        h.tick(1);
        assertTrue(p.getVelocityY() < Constants.DOUBLE_JUMP_VELOCITY - 30f, "no third jump");
    }
}
