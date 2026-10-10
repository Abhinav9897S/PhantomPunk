package phantom.events;

import java.util.List;
import java.util.function.IntConsumer;

import phantom.abilities.AbilityType;
import phantom.audio.SoundId;
import phantom.world.Region;

/**
 * All event types used in the game, as immutable records.
 * <p>OOP concept: records give immutable value objects; each implements {@link GameEvent}
 * so the {@link EventBus} can route them by type.</p>
 */
public final class Events {
    private Events() { }

    public record EnemyKilled(String enemyName, float x, float y) implements GameEvent { }

    public record BossEncounter(String bossName, String subtitle) implements GameEvent { }

    public record BossDefeated(String bossId, String bossName, boolean finalBoss) implements GameEvent { }

    public record RoomEntered(String roomId, String roomName, Region region,
                              boolean firstVisit, boolean regionChanged) implements GameEvent { }

    public record PlayerDamaged(int remainingHealth) implements GameEvent { }

    public record PlayerHealed(int health) implements GameEvent { }

    public record PlayerDied(int shardsLost) implements GameEvent { }

    public record AbilityUnlocked(AbilityType ability) implements GameEvent { }

    public record ItemCollected(String itemId, String displayName) implements GameEvent { }

    public record ShardsCollected(int amount, int total) implements GameEvent { }

    public record BenchRested(String roomId, String benchId) implements GameEvent { }

    public record ShadeRecovered(int shards) implements GameEvent { }

    /** Asks the audio system to play a sound effect. */
    public record Sfx(SoundId sound) implements GameEvent { }

    /** Short notification shown by the HUD. */
    public record Toast(String text) implements GameEvent { }

    /** Asks the UI to open a dialogue box; {@code onChoice} receives the chosen option index (or -1). */
    public record DialogueRequest(String speaker, List<String> lines, List<String> choices,
                                  IntConsumer onChoice) implements GameEvent {
        public DialogueRequest {
            lines = List.copyOf(lines);
            choices = List.copyOf(choices);
        }
    }

    /** Raised when the story is over and an ending has been chosen. */
    public record EndingReached(String endingId) implements GameEvent { }

    public record AchievementUnlocked(String title) implements GameEvent { }
}
