package phantom;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import phantom.world.DirectoryRoomSource;
import phantom.world.LevelFormatException;
import phantom.world.RoomLoader;
import phantom.world.WorldData;

/** Locates the real assets folder for tests (set by Gradle, with a fallback for IDE runs). */
public final class TestAssets {
    private TestAssets() { }

    public static Path assetsDir() {
        String prop = System.getProperty("phantom.assets");
        if (prop != null) return Paths.get(prop);
        for (Path p : new Path[] {Paths.get("assets"), Paths.get("../assets")}) {
            if (Files.isDirectory(p.resolve("rooms"))) return p;
        }
        throw new IllegalStateException("assets folder not found; run tests from the project root");
    }

    public static WorldData loadWorld() throws LevelFormatException {
        return new RoomLoader().loadWorld(new DirectoryRoomSource(assetsDir()));
    }
}
