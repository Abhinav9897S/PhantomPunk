package phantom.systems;

/**
 * Which sides of an entity touched solid tiles during the last move.
 * <p>OOP concept: encapsulation - only the collision system (same package) may set the flags.</p>
 */
public final class CollisionResult {
    private boolean onGround;
    private boolean hitCeiling;
    private boolean hitLeft;
    private boolean hitRight;

    void reset() {
        onGround = false;
        hitCeiling = false;
        hitLeft = false;
        hitRight = false;
    }

    void setOnGround() { onGround = true; }
    void setHitCeiling() { hitCeiling = true; }
    void setHitLeft() { hitLeft = true; }
    void setHitRight() { hitRight = true; }

    public boolean isOnGround() { return onGround; }
    public boolean isHitCeiling() { return hitCeiling; }
    public boolean isHitLeft() { return hitLeft; }
    public boolean isHitRight() { return hitRight; }
    public boolean isHitWall() { return hitLeft || hitRight; }
}
