package phantom.entities.props;

import phantom.entities.Entity;
import phantom.world.WorldContext;

/**
 * Base for static room objects (benches, NPCs, shrines, breakable walls, lights).
 * <p>OOP concept: abstract class that gives every prop a hitbox via {@link Entity}
 * and a default no-op {@link #onDeath}.</p>
 */
public abstract class Prop extends Entity {
    protected Prop(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    @Override
    public void onDeath(WorldContext world) {
        remove();
    }

    @Override
    public int getRenderLayer() { return 2; }
}
