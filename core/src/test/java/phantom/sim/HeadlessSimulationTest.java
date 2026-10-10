package phantom.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.abilities.AbilityType;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.save.SaveData;
import phantom.systems.CollisionSystem;
import phantom.util.Constants;
import phantom.world.GameWorld;
import phantom.world.RoomData;
import phantom.world.TileMap;
import phantom.world.WorldData;

/**
 * Runs the real world for many thousands of ticks with scripted, pseudo-random input
 * (moving, jumping, attacking, dashing, casting) in every room, and checks that nothing throws,
 * the player never leaves the map and never ends up inside a wall.
 */
class HeadlessSimulationTest {
    private static final GameAction[] MOVES = {GameAction.LEFT, GameAction.RIGHT, GameAction.JUMP, GameAction.ATTACK,
        GameAction.DASH, GameAction.BOLT, GameAction.DOWN, GameAction.UP, GameAction.FOCUS};

    private final CollisionSystem collision = new CollisionSystem();

    private static SaveData allAbilities() {
        SaveData data = new SaveData();
        List<String> names = new ArrayList<>();
        for (AbilityType t : AbilityType.values()) names.add(t.name());
        data.setAbilities(names);
        return data;
    }

    @Test
    void longScriptedRunFromTheStart() throws Exception {
        WorldData data = TestAssets.loadWorld();
        int ticks = run(data, null, null, 6000, 7L);
        assertTrue(ticks >= 6000);
    }

    @Test
    void everyRoomSurvivesChaos() throws Exception {
        WorldData data = TestAssets.loadWorld();
        for (RoomData room : data.all()) {
            run(data, allAbilities(), room.getId(), 1500, room.getId().hashCode());
        }
    }

    /** Runs the simulation; returns the number of ticks executed. */
    private int run(WorldData data, SaveData save, String roomId, int ticks, long seed) {
        ScriptedInput input = new ScriptedInput();
        EventBus bus = new EventBus();
        GameWorld world = new GameWorld(data, bus, input, save);
        boolean[] died = {false};
        bus.subscribe(Events.PlayerDied.class, e -> died[0] = true);
        world.start();
        if (roomId != null) {
            RoomData room = data.get(roomId);
            world.enterRoom(roomId, room.getDoors().get(0).id());
        }
        Random rnd = new Random(seed);
        int hold = 0;
        int done = 0;
        for (int i = 0; i < ticks; i++) {
            if (hold-- <= 0) {
                input.releaseAll();
                int presses = 1 + rnd.nextInt(3);
                for (int k = 0; k < presses; k++) input.press(MOVES[rnd.nextInt(MOVES.length)]);
                hold = 5 + rnd.nextInt(50);
            }
            input.poll();
            world.update(Constants.STEP);
            if (died[0]) {
                died[0] = false;
                world.respawn();
            }
            checkPlayer(world, roomId == null ? "start run" : roomId, i);
            done++;
        }
        assertTrue(world.getParticles().getLiveCount() <= phantom.systems.ParticleSystem.MAX_PARTICLES);
        assertTrue(world.getProjectilePool().getCreatedCount() < 200, "projectiles are recycled");
        return done;
    }

    private void checkPlayer(GameWorld world, String where, int tick) {
        Player p = world.getPlayer();
        TileMap map = world.getTileMap();
        String ctx = where + " / " + world.getRoomId() + " tick " + tick + " at " + p.getHitbox();
        assertTrue(Float.isFinite(p.getX()) && Float.isFinite(p.getY()), "position is a number: " + ctx);
        assertTrue(p.getX() >= 0 && p.getX() + p.getWidth() <= map.widthPx(), "inside horizontally: " + ctx);
        assertTrue(p.getY() >= 0 && p.getY() + p.getHeight() <= map.heightPx(), "inside vertically: " + ctx);
        assertFalse(collision.overlapsSolid(map, p.getX() + 0.5f, p.getY() + 0.5f, p.getWidth() - 1f, p.getHeight() - 1f),
                "not inside a wall: " + ctx);
    }
}
