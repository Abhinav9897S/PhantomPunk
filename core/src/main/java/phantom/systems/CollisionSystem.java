package phantom.systems;

import phantom.entities.Entity;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.world.TileMap;

/**
 * Hand-written AABB versus tile-grid collision. Movement is resolved one axis at a time
 * (X then Y), which keeps the maths simple and avoids corner snagging.
 * Speeds are kept below one tile per tick, so a single leading-edge test per axis is enough.
 * <p>OOP concept: a stateless <b>system</b> object owned by the world (composition).</p>
 */
public final class CollisionSystem {
    private static final int T = Constants.TILE;
    private static final float EPS = 0.001f;

    /** Moves the entity by its velocity and resolves overlaps with the map. */
    public void move(Entity e, TileMap map, float dt, boolean dropThrough) {
        CollisionResult result = e.getContacts();
        result.reset();
        moveX(e, map, e.getVelocityX() * dt, result);
        moveY(e, map, e.getVelocityY() * dt, dropThrough, result);
    }

    private void moveX(Entity e, TileMap map, float dx, CollisionResult result) {
        if (dx == 0) return;
        float x = e.getX() + dx;
        float y = e.getY();
        int r0 = TileMap.row(y + EPS);
        int r1 = TileMap.row(y + e.getHeight() - EPS);
        if (dx > 0) {
            int c = TileMap.col(x + e.getWidth() - EPS);
            for (int r = r0; r <= r1; r++) {
                if (map.isSolid(c, r)) {
                    x = c * T - e.getWidth();
                    result.setHitRight();
                    e.setVelocityX(0);
                    break;
                }
            }
        } else {
            int c = TileMap.col(x + EPS);
            for (int r = r0; r <= r1; r++) {
                if (map.isSolid(c, r)) {
                    x = (c + 1) * T;
                    result.setHitLeft();
                    e.setVelocityX(0);
                    break;
                }
            }
        }
        e.moveTo(x, y);
    }

    private void moveY(Entity e, TileMap map, float dy, boolean dropThrough, CollisionResult result) {
        if (dy == 0) return;
        float x = e.getX();
        float oldBottom = e.getY();
        float y = oldBottom + dy;
        int c0 = TileMap.col(x + EPS);
        int c1 = TileMap.col(x + e.getWidth() - EPS);
        if (dy < 0) {
            int r = TileMap.row(y + EPS);
            float top = (r + 1) * T;
            for (int c = c0; c <= c1; c++) {
                boolean oneWay = !dropThrough && map.isOneWay(c, r) && oldBottom >= top - 0.5f;
                if (map.isSolid(c, r) || oneWay) {
                    y = top;
                    result.setOnGround();
                    e.setVelocityY(0);
                    break;
                }
            }
        } else {
            int r = TileMap.row(y + e.getHeight() - EPS);
            for (int c = c0; c <= c1; c++) {
                if (map.isSolid(c, r)) {
                    y = r * T - e.getHeight();
                    result.setHitCeiling();
                    e.setVelocityY(0);
                    break;
                }
            }
        }
        e.moveTo(x, y);
    }

    /** True if a solid tile is directly beside the entity on the given side. */
    public boolean isTouchingWall(Entity e, TileMap map, Direction side) {
        float probeX = side == Direction.LEFT ? e.getX() - 1f : e.getX() + e.getWidth() + 1f;
        float bottom = e.getY() + 4f;
        float top = e.getY() + e.getHeight() - 4f;
        for (float y = bottom; y <= top; y += T * 0.5f) {
            if (map.isSolidAt(probeX, y)) return true;
        }
        return map.isSolidAt(probeX, top);
    }

    /** True if there is floor just beyond the entity's leading foot (used by patrolling enemies). */
    public boolean hasGroundAhead(Entity e, TileMap map, Direction dir) {
        float probeX = dir == Direction.RIGHT ? e.getX() + e.getWidth() + 2f : e.getX() - 2f;
        return map.isStandableAt(probeX, e.getY() - 2f);
    }

    /** True if the body would hit a wall a few pixels ahead. */
    public boolean hasWallAhead(Entity e, TileMap map, Direction dir) {
        return isTouchingWall(e, map, dir);
    }

    /** True if a box at the given position would overlap a solid tile. */
    public boolean overlapsSolid(TileMap map, float x, float y, float w, float h) {
        int c0 = TileMap.col(x + EPS);
        int c1 = TileMap.col(x + w - EPS);
        int r0 = TileMap.row(y + EPS);
        int r1 = TileMap.row(y + h - EPS);
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                if (map.isSolid(c, r)) return true;
            }
        }
        return false;
    }
}
