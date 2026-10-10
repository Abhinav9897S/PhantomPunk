package phantom.util;

/**
 * An object that can be recycled by an {@link ObjectPool}.
 * <p>OOP concept: interface defining a contract (reset to a clean state).</p>
 */
public interface Poolable {
    /** Restores the object to its freshly-constructed state. */
    void reset();
}
