package phantom.save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.SerializationException;

/**
 * Reads and writes the JSON save file and the settings file in a per-user folder
 * ({@code ~/.phantom-descent}). Missing, corrupted or incompatible files never crash the
 * game: they raise {@link SaveException}, and the {@code ...OrDefault} helpers fall back.
 * <p>OOP concepts: <b>file handling</b>, <b>exception handling</b> (checked custom exception,
 * wrapping low-level IO and parse errors), atomic writes so a crash cannot corrupt a save.</p>
 */
public final class SaveManager {
    /** Oldest save format we can still migrate. */
    public static final int MIN_SUPPORTED_VERSION = 2;
    private static final String SAVE_FILE = "save.json";
    private static final String SETTINGS_FILE = "settings.json";

    private final Path directory;
    private final Json json = new Json(JsonWriter.OutputType.json);

    public SaveManager(Path directory) {
        this.directory = directory;
        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
    }

    /** {@code ~/.phantom-descent} - no machine-specific absolute paths are baked in. */
    public static Path defaultDirectory() {
        return Paths.get(System.getProperty("user.home"), ".phantom-descent");
    }

    public boolean hasSave() {
        return Files.isRegularFile(directory.resolve(SAVE_FILE));
    }

    public void save(SaveData data) throws SaveException {
        data.setVersion(SaveData.CURRENT_VERSION);
        write(SAVE_FILE, json.prettyPrint(data));
    }

    public SaveData load() throws SaveException {
        Path file = directory.resolve(SAVE_FILE);
        if (!Files.isRegularFile(file)) throw new SaveException("No save file found.");
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SaveException("The save file could not be read.", e);
        }
        SaveData data;
        try {
            data = json.fromJson(SaveData.class, text);
        } catch (SerializationException | IllegalArgumentException | ClassCastException e) {
            throw new SaveException("The save file is corrupted.", e);
        }
        if (data == null) throw new SaveException("The save file is empty.");
        if (data.getVersion() > SaveData.CURRENT_VERSION) {
            throw new SaveException("The save file comes from a newer version of the game.");
        }
        if (data.getVersion() < MIN_SUPPORTED_VERSION) {
            throw new SaveException("The save file is from an old version and cannot be used.");
        }
        data.sanitize();
        data.setVersion(SaveData.CURRENT_VERSION);
        return data;
    }

    /**
     * Loads the save, or returns null and reports why (bad files are kept as {@code save.json.bad}
     * so nothing is silently destroyed).
     */
    public SaveData loadOrNull(java.util.function.Consumer<String> warning) {
        if (!hasSave()) return null;
        try {
            return load();
        } catch (SaveException e) {
            warning.accept(e.getMessage() + " Starting fresh.");
            quarantine();
            return null;
        }
    }

    public void deleteSave() {
        try {
            Files.deleteIfExists(directory.resolve(SAVE_FILE));
        } catch (IOException ignored) {
            // nothing useful to do; a stale save simply remains
        }
    }

    public Settings loadSettings() {
        Path file = directory.resolve(SETTINGS_FILE);
        try {
            if (Files.isRegularFile(file)) {
                Settings s = json.fromJson(Settings.class, Files.readString(file, StandardCharsets.UTF_8));
                if (s != null) {
                    s.sanitize();
                    return s;
                }
            }
        } catch (IOException | SerializationException | IllegalArgumentException e) {
            // fall through to defaults
        }
        return new Settings();
    }

    public void saveSettings(Settings settings) throws SaveException {
        write(SETTINGS_FILE, json.prettyPrint(settings));
    }

    private void write(String name, String text) throws SaveException {
        try {
            Files.createDirectories(directory);
            Path tmp = directory.resolve(name + ".tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new SaveException("Could not write " + name + ": " + e.getMessage(), e);
        }
    }

    private void quarantine() {
        try {
            Files.move(directory.resolve(SAVE_FILE), directory.resolve(SAVE_FILE + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // leave it in place
        }
    }

    public Path getDirectory() { return directory; }
}
