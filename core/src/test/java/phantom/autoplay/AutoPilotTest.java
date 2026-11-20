package phantom.autoplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.events.EventBus;
import phantom.input.ScriptedInput;
import phantom.save.SaveData;
import phantom.util.Constants;
import phantom.world.DoorData;
import phantom.world.GameWorld;

/** The parts of the autoplay AI: prediction in the sandbox, route planning, the tour and dialogue choices. */
class AutoPilotTest {
    private static final int T = Constants.TILE;

    private static GameWorld world(String room, String... abilities) throws Exception {
        SaveData save = new SaveData();
        save.setAbilities(List.of(abilities));
        GameWorld w = new GameWorld(TestAssets.loadWorld(), new EventBus(), new ScriptedInput(), save);
        w.start();
        w.enterRoom(room, w.getWorldData().get(room).getDoors().get(0).id());
        return w;
    }

    private static Sandbox.Outcome simulate(GameWorld w, int col, int row, Maneuver m) {
        Sandbox sb = new Sandbox();
        sb.setRoom(w.getTileMap(), w.getRegion(), w.getRoomId());
        sb.resetStanding(w.getPlayer(), col * T + 5, row * T);
        return sb.run(m, 900);
    }

    @Test
    void theSandboxPredictsEachAbilityGate() throws Exception {
        // Cistern Steps: only jump + dash clears the spike pit
        GameWorld steps = world("r2_steps", "FOCUS", "DASH");
        Sandbox.Outcome dash = simulate(steps, 8, 7, Maneuver.jump(19 * T + 16, 7 * T, 0, false, true));
        assertTrue(dash.ok() && dash.centerX() > 18 * T, "dash jump lands across the pit");
        assertFalse(simulate(steps, 8, 7, Maneuver.jump(19 * T + 16, 7 * T, 0, false, false)).ok(), "a plain jump falls short");

        // Drowned Chapel: Moss Grip zig-zags up the shaft to the upper ledge
        GameWorld chapel = world("r2_chapel", "FOCUS", "DASH", "WALL_CLING");
        Sandbox.Outcome climb = simulate(chapel, 33, 1, Maneuver.climb(38 * T + 16, 27 * T, -1, 2 * T));
        assertTrue(climb.ok(), "climbed out of the shaft");
        assertEquals(27 * T, climb.y(), 1f);

        // Mist Verge: Wisp Leap reaches the first floating ledge
        GameWorld verge = world("r3_verge", "FOCUS", "DASH", "WALL_CLING", "DOUBLE_JUMP");
        Sandbox.Outcome leap = simulate(verge, 6, 5, Maneuver.jump(13 * T + 16, 10 * T, 0, true, false));
        assertTrue(leap.ok(), "landed on the ledge");
        assertEquals(10 * T, leap.y(), 1f);
    }

    private static List<RoomNav.Step> route(String room, String fromDoor, String toDoor, String... abilities) throws Exception {
        GameWorld w = world(room, abilities);
        w.enterRoom(room, fromDoor);
        Sandbox sb = new Sandbox();
        sb.setRoom(w.getTileMap(), w.getRegion(), w.getRoomId());
        RoomNav nav = new RoomNav(w.getTileMap(), sb, w.getPlayer());
        DoorData from = w.getWorldData().get(room).findDoor(fromDoor);
        DoorData to = w.getWorldData().get(room).findDoor(toDoor);
        List<int[]> cells = new ArrayList<>();
        for (int r = to.row() - 1; r <= to.row() + 2; r++) cells.add(new int[] {to.col(), r});
        RoomNav.Segment start = nav.locate(w.getPlayer().centerX(), from.row() * T, Constants.PLAYER_WIDTH);
        assertNotNull(start, "the arrival point of " + room + "." + fromDoor + " is on a known floor");
        return nav.route(start, w.getPlayer().centerX(), RoomNav.cells(cells), 6000);
    }

    @Test
    void routesRespectTheAbilityGates() throws Exception {
        assertNull(route("r2_steps", "west", "east", "FOCUS"), "no way across the pit without the dash");
        assertNotNull(route("r2_steps", "west", "east", "FOCUS", "DASH"));
        assertNull(route("r2_chapel", "west", "upper", "FOCUS", "DASH"), "the shaft needs Moss Grip");
        assertNotNull(route("r2_chapel", "west", "upper", "FOCUS", "DASH", "WALL_CLING"));
        assertNull(route("r3_verge", "west", "east", "FOCUS", "DASH", "WALL_CLING"), "the ledges need Wisp Leap");
        assertNotNull(route("r3_verge", "west", "east", "FOCUS", "DASH", "WALL_CLING", "DOUBLE_JUMP"));
        assertNotNull(route("r1_hollow", "west", "east", "FOCUS"), "the first rooms need no abilities");
    }

    @Test
    void theTourExploresSideBranchesBeforeEachGuardian() throws Exception {
        Quest quest = new Quest(TestAssets.loadWorld());
        List<String> keys = new ArrayList<>();
        for (Quest.Objective o : quest.objectives()) keys.add(o.key());
        assertTrue(keys.indexOf("shrine_bolt") < keys.indexOf("gnarlwarden"));
        assertTrue(keys.indexOf("relic_roots") < keys.indexOf("gnarlwarden"));
        assertTrue(keys.indexOf("shrine_grip") < keys.indexOf("relic_cistern"));
        assertTrue(keys.indexOf("relic_cistern") < keys.indexOf("gilded_warden"));
        assertTrue(keys.indexOf("relic_heart") < keys.indexOf("hollow_heart"));
        assertEquals("hollow_heart", keys.get(keys.size() - 1), "the final guardian comes last");
        DoorData door = quest.nextDoor("r1_gate", "r3_heart");
        assertEquals("r1_hollow", door.targetRoom());
    }

    @Test
    void choosesTheHiddenEndingAndSensibleAnswers() throws Exception {
        AutoPilot pilot = new AutoPilot(TestAssets.loadWorld());
        assertEquals(2, pilot.chooseOption(List.of("Seal the Mist within your lantern",
                "Shatter the Heart and set the Mist free", "Kindle the three Embers")));
        assertEquals(0, pilot.chooseOption(List.of("Seal the Mist within your lantern", "Shatter the Heart and set the Mist free")));
        assertEquals(1, pilot.chooseOption(List.of("Hollow Mask  -  120 shards", "Leave")), "too poor to buy: leave");
        assertEquals(-1, pilot.chooseOption(List.of()));
    }
}
