package phantom.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads room files from a directory on disk (used by tests and tools).
 * <p>OOP concept: one implementation of the {@link RoomSource} interface.</p>
 */
public final class DirectoryRoomSource implements RoomSource {
    private final Path root;

    public DirectoryRoomSource(Path root) {
        this.root = root;
    }

    @Override
    public String read(String path) throws IOException {
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }
}
