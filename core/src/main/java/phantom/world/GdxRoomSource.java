package phantom.world;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Reads room files through libGDX internal files (working directory or classpath).
 * <p>OOP concept: second implementation of {@link RoomSource}; polymorphism at load time.</p>
 */
public final class GdxRoomSource implements RoomSource {
    @Override
    public String read(String path) throws IOException {
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) {
            file = Gdx.files.classpath(path);
        }
        if (!file.exists()) {
            throw new IOException("Asset not found: " + path + " (check the working directory, see TROUBLESHOOTING.md)");
        }
        return file.readString("UTF-8");
    }
}
