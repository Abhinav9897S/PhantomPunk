package phantom.save;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import phantom.TestAssets;
import phantom.abilities.AbilityType;
import phantom.components.Upgrade;
import phantom.events.EventBus;
import phantom.input.ScriptedInput;
import phantom.world.GameWorld;
import phantom.world.WorldData;

/** Save/load round trip and recovery from missing, corrupted and incompatible files. */
class SaveSystemTest {
    @TempDir
    Path dir;

    @Test
    void roundTripKeepsEveryPieceOfProgress() throws Exception {
        WorldData world = TestAssets.loadWorld();
        GameWorld game = new GameWorld(world, new EventBus(), new ScriptedInput(), null);
        game.start();
        game.getPlayer().getAbilities().unlock(AbilityType.DASH);
        game.getPlayer().getAbilities().unlock(AbilityType.WALL_CLING);
        game.getPlayer().getInventory().addShards(321);
        game.getPlayer().getInventory().addRelic("relic_roots");
        game.getPlayer().getInventory().addUpgrade(Upgrade.MASK_SHARD);
        game.getProgress().markBossDefeated("gnarlwarden");
        game.getProgress().markWallBroken("r1_shrine:wall:33:2");
        game.getProgress().setBench("r1_thorn", "bench_a");
        game.getProgress().leaveShade("r1_hollow", 120f, 64f, 55);
        game.getProgress().addDeath();
        game.update(1.5f);

        SaveManager saves = new SaveManager(dir);
        saves.save(game.snapshot());
        assertTrue(saves.hasSave());
        SaveData loaded = saves.load();

        GameWorld restored = new GameWorld(world, new EventBus(), new ScriptedInput(), loaded);
        assertTrue(restored.getPlayer().getAbilities().has(AbilityType.DASH));
        assertTrue(restored.getPlayer().getAbilities().has(AbilityType.WALL_CLING));
        assertFalse(restored.getPlayer().getAbilities().has(AbilityType.DOUBLE_JUMP));
        assertEquals(321, restored.getPlayer().getInventory().getShards());
        assertEquals(1, restored.getPlayer().getInventory().getRelicCount());
        assertEquals(6, restored.getPlayer().getHealth().getMax(), "mask upgrade re-applied");
        assertTrue(restored.getProgress().isBossDefeated("gnarlwarden"));
        assertTrue(restored.getProgress().isWallBroken("r1_shrine:wall:33:2"));
        assertTrue(restored.getProgress().isDiscovered("r1_gate"));
        assertTrue(restored.getProgress().hasShadeIn("r1_hollow"));
        assertEquals(55, restored.getProgress().getShadeShards());
        assertEquals("r1_thorn", restored.getProgress().getBenchRoom());
        assertEquals(1, restored.getProgress().getDeaths());
        assertTrue(restored.getProgress().getPlayTime() >= 1.5f);
    }

    @Test
    void missingSaveIsReportedNotCrashed() {
        SaveManager saves = new SaveManager(dir);
        assertFalse(saves.hasSave());
        assertThrows(SaveException.class, saves::load);
        assertNull(saves.loadOrNull(msg -> { }));
    }

    @Test
    void corruptedSaveFallsBackToDefaultsAndIsKeptAside() throws Exception {
        Files.writeString(dir.resolve("save.json"), "{ this is not : json ]]", StandardCharsets.UTF_8);
        SaveManager saves = new SaveManager(dir);
        assertThrows(SaveException.class, saves::load);
        List<String> warnings = new ArrayList<>();
        assertNull(saves.loadOrNull(warnings::add));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("corrupted"));
        assertTrue(Files.exists(dir.resolve("save.json.bad")), "bad file kept for inspection");
        assertFalse(saves.hasSave());
    }

    @Test
    void wrongTypesAreTreatedAsCorruption() throws Exception {
        Files.writeString(dir.resolve("save.json"), "{\"version\":3,\"shards\":\"lots\"}", StandardCharsets.UTF_8);
        assertThrows(SaveException.class, () -> new SaveManager(dir).load());
    }

    @Test
    void newerAndOlderVersionsAreRejectedGracefully() throws Exception {
        SaveManager saves = new SaveManager(dir);
        Files.writeString(dir.resolve("save.json"), "{\"version\":99}", StandardCharsets.UTF_8);
        SaveException newer = assertThrows(SaveException.class, saves::load);
        assertTrue(newer.getMessage().contains("newer"));
        Files.writeString(dir.resolve("save.json"), "{\"version\":1}", StandardCharsets.UTF_8);
        SaveException older = assertThrows(SaveException.class, saves::load);
        assertTrue(older.getMessage().contains("old"));
    }

    @Test
    void partialSaveIsRepairedWithDefaults() throws Exception {
        Files.writeString(dir.resolve("save.json"), "{\"version\":2,\"shards\":-40,\"abilities\":[\"DASH\",\"FLYING\"]}",
                StandardCharsets.UTF_8);
        SaveData data = new SaveManager(dir).load();
        assertNotNull(data.getDefeatedBosses());
        assertEquals(0, data.getShards(), "negative shards clamped");
        assertEquals(SaveData.CURRENT_VERSION, data.getVersion(), "migrated");
        phantom.components.AbilitySet set = new phantom.components.AbilitySet();
        set.readFrom(data);
        assertTrue(set.has(AbilityType.DASH), "known ability kept, unknown one ignored");
    }

    @Test
    void settingsSurviveGarbageAndRoundTrip() throws Exception {
        SaveManager saves = new SaveManager(dir);
        Files.writeString(dir.resolve("settings.json"), "garbage!", StandardCharsets.UTF_8);
        Settings defaults = saves.loadSettings();
        assertEquals(new Settings().getMusicVolume(), defaults.getMusicVolume());
        defaults.setMusicVolume(0.25f);
        defaults.setScreenShake(false);
        saves.saveSettings(defaults);
        Settings loaded = saves.loadSettings();
        assertEquals(0.25f, loaded.getMusicVolume(), 1e-6);
        assertFalse(loaded.isScreenShake());
    }
}
