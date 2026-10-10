package phantom.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * A typed pool that recycles short-lived objects (projectiles, particles) to avoid garbage spikes.
 * <p>OOP concept: <b>generics</b> - one pool implementation works for any {@link Poolable} type.</p>
 *
 * @param <T> the pooled type
 */
public final class ObjectPool<T extends Poolable> {
    private final Deque<T> free = new ArrayDeque<>();
    private final Supplier<T> factory;
    private final int maxFree;
    private int created;

    public ObjectPool(Supplier<T> factory, int maxFree) {
        this.factory = factory;
        this.maxFree = maxFree;
    }

    /** Returns a recycled instance, or a new one when the pool is empty. */
    public T obtain() {
        T object = free.pollFirst();
        if (object == null) {
            object = factory.get();
            created++;
        }
        return object;
    }

    /** Resets the object and keeps it for reuse (unless the pool is full). */
    public void free(T object) {
        object.reset();
        if (free.size() < maxFree) {
            free.addFirst(object);
        }
    }

    public int getFreeCount() { return free.size(); }

    /** Total number of instances this pool ever had to allocate. */
    public int getCreatedCount() { return created; }
}
