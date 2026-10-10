package phantom.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Observer pattern: queued delivery, per-type routing, unsubscribe. */
class EventBusTest {
    @Test
    void deliversQueuedEventsToTheRightListeners() {
        EventBus bus = new EventBus();
        List<String> got = new ArrayList<>();
        bus.subscribe(Events.Toast.class, e -> got.add("toast:" + e.text()));
        bus.subscribe(Events.EnemyKilled.class, e -> got.add("kill:" + e.enemyName()));
        bus.publish(new Events.Toast("hi"));
        bus.publish(new Events.EnemyKilled("Crawler", 0, 0));
        assertEquals(0, got.size(), "nothing until dispatch");
        bus.dispatch();
        assertEquals(List.of("toast:hi", "kill:Crawler"), got);
    }

    @Test
    void listenersCanPublishAndUnsubscribeWhileDispatching() {
        EventBus bus = new EventBus();
        List<String> got = new ArrayList<>();
        Runnable[] handle = new Runnable[1];
        handle[0] = bus.subscribe(Events.Toast.class, e -> {
            got.add(e.text());
            handle[0].run();
            bus.publish(new Events.Toast("second"));
        });
        bus.publish(new Events.Toast("first"));
        bus.dispatch();
        assertEquals(List.of("first"), got, "unsubscribed itself, so 'second' is not received");
        assertEquals(0, bus.pendingCount());
    }
}
