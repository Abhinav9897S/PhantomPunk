package phantom.entities;

import phantom.components.Hitbox;

/**
 * An object with an axis-aligned hitbox.
 * <p>OOP concept: <b>interface</b> with a default method.</p>
 */
public interface Collidable {
    Hitbox getHitbox();

    default boolean overlaps(Collidable other) {
        return getHitbox().overlaps(other.getHitbox());
    }
}
