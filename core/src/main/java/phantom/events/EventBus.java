package phantom.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * A tiny publish/subscribe hub. Gameplay code publishes events; the HUD, audio,
 * achievements and save logic subscribe without the publisher knowing about them.
 * <p>OOP concept: <b>Observer pattern</b>, plus a {@link Queue} so events raised during
 * an update are delivered at a safe point.</p>
 */
public final class EventBus {
    private static final int MAX_EVENTS_PER_DISPATCH = 512;

    private final Map<Class<? extends GameEvent>, List<Consumer<GameEvent>>> listeners = new HashMap<>();
    private final Queue<GameEvent> queue = new ArrayDeque<>();

    /** Registers a listener for one event type. Returns a handle that can unsubscribe it. */
    public <T extends GameEvent> Runnable subscribe(Class<T> type, Consumer<T> listener) {
        Consumer<GameEvent> wrapper = event -> listener.accept(type.cast(event));
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(wrapper);
        return () -> listeners.getOrDefault(type, new ArrayList<>()).remove(wrapper);
    }

    /** Queues an event; it is delivered on the next {@link #dispatch()}. */
    public void publish(GameEvent event) {
        queue.add(event);
    }

    /** Delivers an event immediately (used for UI requests that must not wait a tick). */
    public void publishNow(GameEvent event) {
        deliver(event);
    }

    /** Delivers all queued events, including ones raised by listeners while dispatching. */
    public void dispatch() {
        int delivered = 0;
        while (!queue.isEmpty() && delivered < MAX_EVENTS_PER_DISPATCH) {
            deliver(queue.poll());
            delivered++;
        }
    }

    public int pendingCount() {
        return queue.size();
    }

    public void clear() {
        listeners.clear();
        queue.clear();
    }

    private void deliver(GameEvent event) {
        List<Consumer<GameEvent>> list = listeners.get(event.getClass());
        if (list == null) return;
        // Copy so listeners may subscribe or unsubscribe while being notified.
        for (Consumer<GameEvent> listener : new ArrayList<>(list)) {
            listener.accept(event);
        }
    }
}
