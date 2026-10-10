package phantom.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import phantom.entities.GameObject;
import phantom.render.RenderContext;

/**
 * Owns every {@link GameObject} in the current room and runs them polymorphically:
 * it calls {@code update()} and {@code render()} without knowing the concrete type.
 * Additions are deferred so objects can spawn others while the list is being iterated.
 * <p>OOP concept: <b>polymorphism</b> over {@code List<GameObject>}; generic type queries.</p>
 */
public final class EntityManager {
    private static final Comparator<GameObject> BY_LAYER = Comparator.comparingInt(GameObject::getRenderLayer);

    private final List<GameObject> objects = new ArrayList<>();
    private final List<GameObject> pending = new ArrayList<>();
    private final List<GameObject> renderOrder = new ArrayList<>();
    private Consumer<GameObject> removalListener = o -> { };

    public void add(GameObject object) {
        pending.add(object);
    }

    public void setRemovalListener(Consumer<GameObject> listener) {
        removalListener = listener;
    }

    public void update(float dt, WorldContext world) {
        flushPending();
        for (int i = 0; i < objects.size(); i++) {
            GameObject o = objects.get(i);
            if (o.isRemoved()) continue;
            o.beginTick();
            o.update(dt, world);
        }
        sweepRemoved();
        flushPending();
    }

    public void render(RenderContext ctx, float alpha) {
        renderOrder.clear();
        renderOrder.addAll(objects);
        renderOrder.sort(BY_LAYER);
        for (GameObject o : renderOrder) {
            if (!o.isRemoved()) o.render(ctx, alpha);
        }
    }

    private void flushPending() {
        if (pending.isEmpty()) return;
        objects.addAll(pending);
        pending.clear();
    }

    private void sweepRemoved() {
        for (int i = objects.size() - 1; i >= 0; i--) {
            GameObject o = objects.get(i);
            if (o.isRemoved()) {
                objects.remove(i);
                removalListener.accept(o);
            }
        }
    }

    /** Removes everything (room change). The removal listener is still told, so pools get objects back. */
    public void clear() {
        flushPending();
        for (GameObject o : objects) removalListener.accept(o);
        objects.clear();
    }

    /** Live, read-only view of the current objects. */
    public List<GameObject> view() {
        return Collections.unmodifiableList(objects);
    }

    /** All objects of a given type (allocates; meant for tests and occasional queries). */
    public <T> List<T> getAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (GameObject o : objects) {
            if (type.isInstance(o) && !o.isRemoved()) result.add(type.cast(o));
        }
        for (GameObject o : pending) {
            if (type.isInstance(o)) result.add(type.cast(o));
        }
        return result;
    }

    public <T> T first(Class<T> type) {
        for (GameObject o : objects) {
            if (type.isInstance(o) && !o.isRemoved()) return type.cast(o);
        }
        return null;
    }

    public int size() {
        return objects.size() + pending.size();
    }
}
