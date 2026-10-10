package phantom.art;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Frames of one procedurally generated sprite plus matching white "flash" frames,
 * with its logical size and the distance from the bottom of the image to the feet.
 * <p>OOP concept: immutable value object; frame indices wrap so callers cannot go out of range.</p>
 */
public final class SpriteSheet {
    private final TextureRegion[] frames;
    private final TextureRegion[] flashes;
    private final float width;
    private final float height;
    private final float footOffset;

    public SpriteSheet(TextureRegion[] frames, TextureRegion[] flashes, float width, float height, float footOffset) {
        this.frames = frames.clone();
        this.flashes = flashes.clone();
        this.width = width;
        this.height = height;
        this.footOffset = footOffset;
    }

    public TextureRegion frame(int index) {
        return frames[Math.floorMod(index, frames.length)];
    }

    /** White silhouette of a frame (falls back to the normal frame if none was made). */
    public TextureRegion flash(int index) {
        TextureRegion f = flashes[Math.floorMod(index, flashes.length)];
        return f != null ? f : frame(index);
    }

    public boolean hasFlash() {
        return flashes.length > 0 && flashes[0] != null;
    }

    public int count() { return frames.length; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getFootOffset() { return footOffset; }
}
