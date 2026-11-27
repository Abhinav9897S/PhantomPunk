import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A tiny publish/subscribe hub.
 *
 * OOP: OBSERVER PATTERN - whoever publishes an event does not know who listens.
 *      The Bench publishes "BenchRested"; the Game prints a message and the
 *      Achievements class could count it, without the Bench knowing either exists.
 * OOP: GENERICS - subscribe(Class<T>, Consumer<T>) is type-safe for every event type.
 * OOP: COLLECTIONS - a HashMap from event class to a List of listeners.
 * Java: LAMBDAS - listeners are written as lambdas (e -> ...).
 */
public class EventBus {
    private final Map<Class<?>, List<Consumer<GameEvent>>> listeners = new HashMap<>();

    public <T extends GameEvent> void subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(event -> listener.accept(type.cast(event)));
    }

    public void publish(GameEvent event) {
        for (Consumer<GameEvent> listener : listeners.getOrDefault(event.getClass(), List.of())) {
            listener.accept(event);
        }
    }
}
