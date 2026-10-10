package phantom.entities;

import phantom.render.RenderContext;

/**
 * Anything that can draw itself. {@code alpha} is the interpolation factor between the
 * previous and the current simulation tick (0..1) for smooth rendering.
 * <p>OOP concept: <b>interface</b> - the renderer only depends on this contract.</p>
 */
public interface Renderable {
    void render(RenderContext ctx, float alpha);

    /** Lower layers are drawn first. */
    default int getRenderLayer() {
        return 0;
    }
}
