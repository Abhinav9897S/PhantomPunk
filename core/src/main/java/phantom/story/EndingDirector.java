package phantom.story;

import java.util.ArrayList;
import java.util.List;

import phantom.events.EventBus;
import phantom.events.Events;
import phantom.world.WorldContext;

/**
 * After the final boss falls, waits for its death sequence, then offers the ending choice
 * (a third option appears if all Ember Relics were found) and announces the result.
 * <p>OOP concept: Observer of {@code BossDefeated}; uses a lambda callback for the choice.</p>
 */
public final class EndingDirector {
    private static final float DELAY = 2.6f;
    private final EventBus bus;
    private final WorldContext world;
    private final Runnable subscription;
    private float countdown = -1f;
    private boolean offered;

    public EndingDirector(EventBus bus, WorldContext world) {
        this.bus = bus;
        this.world = world;
        this.subscription = bus.subscribe(Events.BossDefeated.class, e -> {
            if (e.finalBoss() && !offered) countdown = DELAY;
        });
    }

    public void update(float dt) {
        if (countdown < 0) return;
        countdown -= dt;
        if (countdown <= 0) {
            countdown = -1f;
            offer();
        }
    }

    private void offer() {
        offered = true;
        List<Ending> options = Ending.choicesFor(world.getPlayer().getInventory().hasAllRelics());
        List<String> labels = new ArrayList<>();
        for (Ending e : options) labels.add(label(e));
        List<String> lines = new ArrayList<>(List.of(
                "The Heart lies broken. Its Mist coils around your lantern, waiting.",
                "It will go wherever you lead it."));
        if (options.contains(Ending.DAWN)) lines.add("The three Embers in your cloak begin to burn...");
        bus.publish(new Events.DialogueRequest("The Hollow Heart", lines, labels, choice -> {
            Ending chosen = options.get(Math.max(0, Math.min(options.size() - 1, choice)));
            world.getProgress().setEnding(chosen.name());
            bus.publish(new Events.EndingReached(chosen.name()));
        }));
    }

    private static String label(Ending e) {
        switch (e) {
            case SEALED: return "Seal the Mist within your lantern";
            case UNBOUND: return "Shatter the Heart and set the Mist free";
            default: return "Kindle the three Embers";
        }
    }

    public void detach() {
        subscription.run();
    }
}
