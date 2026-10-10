package phantom.art;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test double for {@link SpriteRegistry}: keeps painted canvases in memory instead of
 * uploading them to the GPU, so painters can be tested headless.
 */
public final class RecordingRegistry implements SpriteRegistry {
    private final Map<String, List<Canvas>> sprites = new LinkedHashMap<>();
    private final Map<String, Canvas> textures = new LinkedHashMap<>();

    @Override
    public Canvas canvas(float w, float h) {
        return new Canvas(w, h, SpriteFactory.SUPERSAMPLE);
    }

    @Override
    public Canvas canvas(float w, float h, float scale) {
        return new Canvas(w, h, scale);
    }

    @Override
    public void sprite(String name, float footOffset, boolean flash, Canvas... frames) {
        sprites.put(name, List.of(frames));
    }

    @Override
    public void texture(String name, Canvas canvas, int downsample, boolean repeat) {
        textures.put(name, canvas);
    }

    public Map<String, List<Canvas>> getSprites() { return sprites; }
    public Map<String, Canvas> getTextures() { return textures; }
}
