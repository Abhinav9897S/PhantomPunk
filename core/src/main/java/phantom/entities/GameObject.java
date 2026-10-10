package phantom.entities;

import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * Root of every object living in a room. Stores the current and previous position so the
 * renderer can interpolate between fixed simulation ticks.
 * <p>OOP concept: <b>abstraction</b> - {@link #update} and {@link #render} are abstract;
 * the world keeps a {@code List<GameObject>} and calls them <b>polymorphically</b>.</p>
 */
public abstract class GameObject implements Renderable {
    private float x;
    private float y;
    private float prevX;
    private float prevY;
    private boolean removed;

    protected GameObject(float x, float y) {
        teleport(x, y);
    }

    /** Advances the object by one fixed step. */
    public abstract void update(float dt, WorldContext world);

    @Override
    public abstract void render(RenderContext ctx, float alpha);

    /** Called by the entity manager before {@link #update}; remembers last tick's position. */
    public final void beginTick() {
        prevX = x;
        prevY = y;
    }

    /** Moves without breaking interpolation (normal movement). */
    public void moveTo(float nx, float ny) {
        x = nx;
        y = ny;
    }

    /** Jumps to a position with no interpolation trail (spawns, respawns, room changes). */
    public void teleport(float nx, float ny) {
        x = nx;
        y = ny;
        prevX = nx;
        prevY = ny;
    }

    public float renderX(float alpha) {
        return prevX + (x - prevX) * alpha;
    }

    public float renderY(float alpha) {
        return prevY + (y - prevY) * alpha;
    }

    public void remove() { removed = true; }
    /** Lets pooled objects be reused after removal. */
    protected final void clearRemoved() { removed = false; }
    public boolean isRemoved() { return removed; }
    public float getX() { return x; }
    public float getY() { return y; }
}
