package phantom.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.abilities.AbilityType;
import phantom.entities.enemies.Enemy;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.save.SaveData;
import phantom.util.Constants;
import phantom.world.GameWorld;

/**
 * Hops across every floating platform of the Mist Verge and up the Hollow Spire with the
 * Wisp Leap, using real physics on the real room files, and walks out of the far door.
 */
class UmbralTraversalTest {
    private static final int T = Constants.TILE;

    private final ScriptedInput in = new ScriptedInput();
    private final List<String> rooms = new ArrayList<>();
    private GameWorld world;

    private void setUp(String room, String door) throws Exception {
        SaveData save = new SaveData();
        save.setAbilities(List.of("FOCUS", "DASH", "WALL_CLING", "DOUBLE_JUMP"));
        EventBus bus = new EventBus();
        bus.subscribe(Events.RoomEntered.class, e -> rooms.add(e.roomId()));
        world = new GameWorld(TestAssets.loadWorld(), bus, in, save);
        world.start();
        world.enterRoom(room, door);
        for (Enemy e : world.getEntities().getAll(Enemy.class)) e.remove();
        tick(5);
    }

    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            in.poll();
            world.update(Constants.STEP);
            world.getPlayer().getHealth().makeInvincible(100f);
        }
    }

    /** Jump (plus Wisp Leap at the apex) onto the platform spanning columns x0..x1 whose top is feetRow. */
    private void hop(int x0, int x1, int feetRow) {
        Player p = world.getPlayer();
        float target = (x0 + x1 + 1) / 2f * T;
        GameAction dir = target > p.centerX() ? GameAction.RIGHT : GameAction.LEFT;
        boolean rising = feetRow * T > p.getY() + 3 * T;
        in.releaseAll();
        in.press(GameAction.JUMP);
        boolean second = false;
        int guard = 0;
        do {
            float dx = target - p.centerX();
            boolean above = p.getY() > feetRow * T + 4;
            boolean steer = !rising || above || Math.abs(dx) > 3 * T;
            if (steer && Math.abs(dx) > 10) {
                in.press(dx > 0 ? GameAction.RIGHT : GameAction.LEFT);
                in.release(dx > 0 ? GameAction.LEFT : GameAction.RIGHT);
            } else {
                in.release(GameAction.LEFT);
                in.release(GameAction.RIGHT);
            }
            if (!second && p.getVelocityY() < 80f && guard > 5) {
                in.release(GameAction.JUMP);
                tick(1);
                in.press(GameAction.JUMP);
                second = true;
            }
            tick(1);
        } while (!(p.isOnGround() && guard > 8) && guard++ < 200);
        in.releaseAll();
        tick(4);
        assertEquals(feetRow * T, p.getY(), 1f, "landed on the platform at row " + feetRow + " (x " + p.getX() / T + ") dir " + dir);
    }

    @Test
    void crossTheMistVerge() throws Exception {
        setUp("r3_verge", "west");
        int[][] path = {{5, 8, 5}, {12, 15, 10}, {19, 22, 15}, {28, 31, 13}, {35, 38, 18}, {44, 47, 14}, {51, 54, 19}, {57, 62, 19}};
        Player p = world.getPlayer();
        in.press(GameAction.RIGHT);
        while (p.getX() < 6 * T) tick(1);
        in.releaseAll();
        tick(10);
        for (int i = 1; i < path.length; i++) hop(path[i][0], path[i][1], path[i][2]);
        in.press(GameAction.RIGHT);
        tick(120);
        assertTrue(rooms.contains("r3_spire"), "walked into the Hollow Spire; rooms " + rooms);
    }

    @Test
    void climbTheHollowSpire() throws Exception {
        setUp("r3_spire", "west");
        int[][] path = {{8, 12, 6}, {16, 20, 11}, {6, 10, 16}, {14, 18, 21}, {22, 26, 26}, {12, 16, 31}, {20, 24, 36}, {28, 34, 38}};
        for (int[] step : path) hop(step[0], step[1], step[2]);
        in.press(GameAction.RIGHT);
        tick(120);
        assertTrue(rooms.contains("r3_lantern"), "reached the Last Lantern; rooms " + rooms);
    }
}
