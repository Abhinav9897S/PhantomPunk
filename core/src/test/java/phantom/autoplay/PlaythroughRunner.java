package phantom.autoplay;

import java.util.ArrayList;
import java.util.List;

import phantom.TestAssets;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.story.EndingDirector;
import phantom.util.Constants;
import phantom.world.GameWorld;
import phantom.world.WorldData;

/**
 * Plays a whole game headlessly with the {@link AutoPilot} at the controls, standing in for the
 * screens: dialogues are answered at once and death respawns at the last bench after the usual delay.
 */
final class PlaythroughRunner {
    final List<String> log = new ArrayList<>();
    final GameWorld world;
    final AutoPilot pilot;
    String ending;
    int deaths;
    int ticks;
    private final ScriptedInput input = new ScriptedInput();
    private final EventBus bus = new EventBus();
    private final EndingDirector endings;
    private float respawnIn = -1f;

    PlaythroughRunner(long seed) throws Exception {
        WorldData data = TestAssets.loadWorld();
        world = new GameWorld(data, bus, input, null);
        world.getRandom().setSeed(seed);
        pilot = new AutoPilot(data);
        endings = new EndingDirector(bus, world);
        bus.subscribe(Events.DialogueRequest.class, e -> {
            int choice = pilot.chooseOption(e.choices());
            if (e.onChoice() != null) e.onChoice().accept(choice);
        });
        bus.subscribe(Events.PlayerDied.class, e -> {
            deaths++;
            respawnIn = 3f;
            note("died in " + world.getRoomId() + " while: " + pilot.status());
        });
        bus.subscribe(Events.RoomEntered.class, e -> note("room " + e.roomId()));
        bus.subscribe(Events.BossDefeated.class, e -> note("defeated " + e.bossId()));
        bus.subscribe(Events.AbilityUnlocked.class, e -> note("ability " + e.ability()));
        bus.subscribe(Events.ItemCollected.class, e -> note("item " + e.itemId()));
        bus.subscribe(Events.EndingReached.class, e -> ending = e.endingId());
        world.start();
    }

    private void note(String s) {
        log.add(String.format("[%6.1fs] %s", ticks * Constants.STEP, s));
    }

    /** Plays until an ending is chosen or {@code seconds} of game time have passed. */
    void play(float seconds) {
        int limit = Math.round(seconds / Constants.STEP);
        while (ending == null && ticks < limit) {
            pilot.update(world);
            input.releaseAll();
            for (GameAction a : pilot.buttons()) input.press(a);
            input.poll();
            world.update(Constants.STEP);
            endings.update(Constants.STEP);
            if (respawnIn > 0 && (respawnIn -= Constants.STEP) <= 0) world.respawn();
            ticks++;
        }
    }

    String summary() {
        return "ending " + ending + " after " + (int) (ticks * Constants.STEP) + " s, " + deaths + " deaths, in "
                + world.getRoomId() + " doing '" + pilot.status() + "'\n" + String.join("\n", log);
    }
}
