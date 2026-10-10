package phantom.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import phantom.TestAssets;
import phantom.abilities.AbilityType;
import phantom.world.validation.LevelValidator;

/** The shipped world must be completable: if this fails, the build fails. */
class LevelValidationTest {
    private static WorldData world;
    private final LevelValidator validator = new LevelValidator();

    @BeforeAll
    static void load() throws LevelFormatException {
        world = TestAssets.loadWorld();
    }

    @Test
    void worldHasEnoughRooms() {
        assertTrue(world.size() >= 12, "at least 12 rooms required, found " + world.size());
    }

    @Test
    void wholeWorldIsValidAndCompletable() {
        LevelValidator.Report report = validator.validate(world);
        assertTrue(report.isValid(), () -> "Level problems:\n  " + String.join("\n  ", report.errors()));
        assertEquals(world.size(), report.reachableRooms().size());
        assertTrue(report.abilities().containsAll(EnumSet.of(AbilityType.DASH, AbilityType.WALL_CLING,
                AbilityType.DOUBLE_JUMP, AbilityType.LIGHT_BOLT)));
    }

    @Test
    void cisternNeedsTheDash() {
        Set<String> rooms = validator.reachableRooms(world, EnumSet.of(AbilityType.FOCUS, AbilityType.LIGHT_BOLT));
        assertTrue(rooms.contains("r1_boss"));
        assertTrue(rooms.contains("r2_steps"), "the first Cistern room is visible from the boss room");
        assertFalse(rooms.contains("r2_aqueduct"), "without Dash the Cistern must stay closed");
    }

    @Test
    void gearworksNeedsWallCling() {
        Set<String> rooms = validator.reachableRooms(world, EnumSet.of(AbilityType.FOCUS, AbilityType.DASH));
        assertTrue(rooms.contains("r2_chapel"));
        assertFalse(rooms.contains("r2_gears"), "the Gearworks Shaft must require Moss Grip");
    }

    @Test
    void umbralHeartNeedsDoubleJump() {
        Set<String> rooms = validator.reachableRooms(world,
                EnumSet.of(AbilityType.FOCUS, AbilityType.DASH, AbilityType.WALL_CLING));
        assertTrue(rooms.contains("r3_verge"));
        assertFalse(rooms.contains("r3_spire"), "the Umbral Heart must require the Wisp Leap");
    }

    @Test
    void secretsAreBehindBreakableWalls() {
        assertFalse(world.get("r1_shrine").getBreakables().isEmpty());
        assertFalse(world.get("r2_chapel").getBreakables().isEmpty());
        assertFalse(world.get("r3_spire").getBreakables().isEmpty());
    }
}
