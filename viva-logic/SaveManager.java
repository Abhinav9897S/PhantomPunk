import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes and reads the save file (simple "key=value" lines).
 *
 * OOP: FILE HANDLING with java.nio.file (Path, Files.writeString, Files.readAllLines).
 * OOP: EXCEPTION HANDLING - low-level IOException / bad numbers are caught and wrapped
 *      in our own SaveException with a friendly message (exception chaining).
 */
public class SaveManager {
    private final Path file;

    public SaveManager(Path file) {
        this.file = file;
    }

    public boolean hasSave() {
        return Files.isRegularFile(file);
    }

    public void save(SaveData data) throws SaveException {
        String text = "respawnRoom=" + data.respawnRoom() + "\n"
                + "health=" + data.health() + "\n"
                + "soul=" + data.soul() + "\n"
                + "shards=" + data.shards() + "\n"
                + "abilities=" + String.join(",", data.abilities().stream().map(Enum::name).toList()) + "\n"
                + "bosses=" + String.join(",", data.defeatedBosses()) + "\n";
        try {
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SaveException("Could not write the save file.", e);
        }
    }

    public SaveData load() throws SaveException {
        if (!hasSave()) throw new SaveException("No save file found.");
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SaveException("The save file could not be read.", e);
        }
        Map<String, String> values = new HashMap<>();
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq > 0) values.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        try {
            Set<AbilityType> abilities = EnumSet.noneOf(AbilityType.class);
            for (String a : values.getOrDefault("abilities", "").split(",")) {
                if (!a.isBlank()) abilities.add(AbilityType.valueOf(a));
            }
            Set<String> bosses = new LinkedHashSet<>();
            for (String b : values.getOrDefault("bosses", "").split(",")) {
                if (!b.isBlank()) bosses.add(b);
            }
            return new SaveData(Integer.parseInt(values.get("respawnRoom")), Integer.parseInt(values.get("health")),
                    Integer.parseInt(values.get("soul")), Integer.parseInt(values.get("shards")), abilities, bosses);
        } catch (RuntimeException e) {   // NumberFormatException, NullPointerException, IllegalArgumentException
            throw new SaveException("The save file is corrupted.", e);
        }
    }
}
