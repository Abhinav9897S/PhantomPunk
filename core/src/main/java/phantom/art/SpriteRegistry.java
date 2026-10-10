package phantom.art;

/**
 * What a painter needs: canvases to paint on and a place to register the results.
 * <p>OOP concept: <b>interface</b> - the real {@link SpriteFactory} packs textures on the GPU,
 * while tests use a recording implementation, so painters run headless.</p>
 */
public interface SpriteRegistry {
    /** Supersampled canvas in logical units (see {@link SpriteFactory#SUPERSAMPLE}). */
    Canvas canvas(float logicalWidth, float logicalHeight);

    /** Canvas at an explicit scale (backgrounds use a cheaper scale). */
    Canvas canvas(float logicalWidth, float logicalHeight, float scale);

    /** Registers an animated sprite; {@code flash} also creates white hit-flash frames. */
    void sprite(String name, float footOffset, boolean flash, Canvas... frames);

    /** Registers a standalone texture (backgrounds, light masks). */
    void texture(String name, Canvas canvas, int downsample, boolean repeat);
}
