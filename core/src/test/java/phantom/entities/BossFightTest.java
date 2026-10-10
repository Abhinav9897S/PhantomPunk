package phantom.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.abilities.AbilityType;
import phantom.entities.bosses.Boss;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.util.Constants;
import phantom.world.GameWorld;

/** Boss arenas lock, bosses change phase, and defeat reopens the gates and grants the reward. */
class BossFightTest {
    @Test
    void gnarlwardenArenaLocksPhasesAndRewardsDash() throws Exception {
        ScriptedInput input = new ScriptedInput();
        EventBus bus = new EventBus();
        List<String> defeated = new ArrayList<>();
        bus.subscribe(Events.BossDefeated.class, e -> defeated.add(e.bossId()));
        GameWorld world = new GameWorld(TestAssets.loadWorld(), bus, input, null);
        world.start();
        world.enterRoom("r1_boss", "west");
        world.getPlayer().getHealth().makeInvincible(1000f);
        input.press(GameAction.RIGHT);
        for (int i = 0; i < 120; i++) { input.poll(); world.update(Constants.STEP); }
        input.releaseAll();
        assertTrue(world.getTileMap().areGatesClosed(), "arena locked");
        for (int i = 0; i < 200; i++) { input.poll(); world.update(Constants.STEP); }
        Boss boss = world.getActiveBoss();
        assertNotNull(boss, "boss awake after its title card");

        boolean phaseTwo = false;
        for (int i = 0; i < 4000 && boss.isAlive(); i++) {
            boss.takeDamage(new DamageInfo(5, boss.centerX() - 10, boss.centerY(), DamageKind.SLASH), world);
            input.poll();
            world.update(Constants.STEP);
            world.getPlayer().getHealth().makeInvincible(1000f);
            phaseTwo |= boss.getPhase() == 1;
        }
        assertTrue(phaseTwo, "entered phase two");
        assertFalse(boss.isAlive());
        for (int i = 0; i < 30; i++) { input.poll(); world.update(Constants.STEP); }
        assertEquals(List.of("gnarlwarden"), defeated);
        assertFalse(world.getTileMap().areGatesClosed(), "gates reopened");
        assertTrue(world.getPlayer().getAbilities().has(AbilityType.DASH), "Dash granted");
        assertTrue(world.getProgress().isBossDefeated("gnarlwarden"));

        world.enterRoom("r1_boss", "west");
        assertEquals(null, world.getActiveBoss(), "a defeated boss does not return");
    }
}
