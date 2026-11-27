import java.util.Set;

/** A snapshot of the progress that gets written to disk.   OOP: an immutable RECORD. */
public record SaveData(int respawnRoom, int health, int soul, int shards,
                       Set<AbilityType> abilities, Set<String> defeatedBosses) {
}
