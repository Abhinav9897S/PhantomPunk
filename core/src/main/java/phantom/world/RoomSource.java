package phantom.world;

import java.io.IOException;

/**
 * Where room text files come from: the libGDX asset system in the game, a folder in tests.
 * <p>OOP concept: <b>interface</b> enabling dependency injection (the loader does not care).</p>
 */
public interface RoomSource {
    String read(String path) throws IOException;
}
