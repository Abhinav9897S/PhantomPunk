package phantom.components;

/**
 * Axis-aligned bounding box in world units (x/y is the bottom-left corner).
 * <p>OOP concept: <b>encapsulation</b> - a small value-like component with intent-revealing methods.</p>
 */
public final class Hitbox {
    private float x;
    private float y;
    private float width;
    private float height;

    public Hitbox() {
        this(0, 0, 0, 0);
    }

    public Hitbox(float x, float y, float width, float height) {
        set(x, y, width, height);
    }

    public Hitbox set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean overlaps(Hitbox o) {
        return overlaps(o.x, o.y, o.width, o.height);
    }

    public boolean overlaps(float ox, float oy, float ow, float oh) {
        return x < ox + ow && x + width > ox && y < oy + oh && y + height > oy;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float right() { return x + width; }
    public float top() { return y + height; }
    public float centerX() { return x + width * 0.5f; }
    public float centerY() { return y + height * 0.5f; }

    @Override
    public String toString() {
        return String.format("Hitbox[%.1f, %.1f, %.1f x %.1f]", x, y, width, height);
    }
}
