package phantom.systems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import phantom.entities.Entity;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.world.TileMap;
import phantom.world.TileType;
import phantom.world.WorldContext;

/** AABB-versus-tile resolution, one-way platforms and probes. */
class CollisionSystemTest {
    private static final int T = Constants.TILE;
    private final CollisionSystem collision = new CollisionSystem();

    /** Minimal entity for driving the collision system directly. */
    private static final class Box extends Entity {
        Box(float x, float y) { super(x, y, 22, 40); }
        @Override public void update(float dt, WorldContext world) { }
        @Override public void render(RenderContext ctx, float alpha) { }
        @Override public void onDeath(WorldContext world) { }
    }

    private TileMap floorMap() {
        TileMap map = new TileMap(10, 10);
        for (int c = 0; c < 10; c++) map.set(c, 0, TileType.SOLID);
        return map;
    }

    @Test
    void fallingBoxLandsOnTheFloor() {
        TileMap map = floorMap();
        Box b = new Box(64, 100);
        for (int i = 0; i < 120; i++) {
            b.setVelocityY(Math.max(b.getVelocityY() - Constants.GRAVITY * Constants.STEP, -Constants.MAX_FALL_SPEED));
            collision.move(b, map, Constants.STEP, false);
        }
        assertEquals(T, b.getY(), 0.001f);
        assertTrue(b.isOnGround());
        assertEquals(0f, b.getVelocityY(), 0.001f);
    }

    @Test
    void wallStopsHorizontalMovement() {
        TileMap map = floorMap();
        for (int r = 1; r < 10; r++) map.set(5, r, TileType.SOLID);
        Box b = new Box(2 * T, T);
        for (int i = 0; i < 60; i++) {
            b.setVelocityX(300);
            collision.move(b, map, Constants.STEP, false);
        }
        assertEquals(5 * T - 22, b.getX(), 0.001f);
        assertTrue(b.getContacts().isHitRight());
        assertTrue(collision.isTouchingWall(b, map, Direction.RIGHT));
        assertFalse(collision.isTouchingWall(b, map, Direction.LEFT));
    }

    @Test
    void ceilingStopsUpwardMovement() {
        TileMap map = floorMap();
        for (int c = 0; c < 10; c++) map.set(c, 4, TileType.SOLID);
        Box b = new Box(64, T);
        b.setVelocityY(900);
        boolean bumped = false;
        for (int i = 0; i < 10; i++) {
            collision.move(b, map, Constants.STEP, false);
            bumped |= b.getContacts().isHitCeiling();
        }
        assertEquals(4 * T - 40, b.getY(), 0.001f);
        assertTrue(bumped);
        assertEquals(0f, b.getVelocityY(), 0.001f);
    }

    @Test
    void oneWayPlatformsLetYouUpButHoldYouFromAbove() {
        TileMap map = floorMap();
        for (int c = 0; c < 10; c++) map.set(c, 3, TileType.ONE_WAY);
        Box b = new Box(64, T);
        b.setVelocityY(800);
        for (int i = 0; i < 8; i++) collision.move(b, map, Constants.STEP, false);
        assertTrue(b.getY() > 4 * T, "passed up through the platform");
        for (int i = 0; i < 120; i++) {
            b.setVelocityY(b.getVelocityY() - Constants.GRAVITY * Constants.STEP);
            collision.move(b, map, Constants.STEP, false);
        }
        assertEquals(4 * T, b.getY(), 0.001f, "lands on top of the platform");
        for (int i = 0; i < 30; i++) {
            b.setVelocityY(b.getVelocityY() - Constants.GRAVITY * Constants.STEP);
            collision.move(b, map, Constants.STEP, true);
        }
        assertTrue(b.getY() < 4 * T, "drops through when asked");
    }

    @Test
    void ledgeProbeDetectsTheEdge() {
        TileMap map = new TileMap(10, 10);
        for (int c = 0; c < 4; c++) map.set(c, 0, TileType.SOLID);
        Box b = new Box(4 * T - 23, T); // right foot on the last floor tile
        assertFalse(collision.hasGroundAhead(b, map, Direction.RIGHT));
        assertTrue(collision.hasGroundAhead(b, map, Direction.LEFT));
    }

    @Test
    void outOfBoundsIsSolid() {
        TileMap map = new TileMap(5, 5);
        assertTrue(map.isSolid(-1, 2));
        assertTrue(map.isSolid(2, -1));
        assertTrue(map.isSolid(5, 2));
        assertFalse(map.isSolid(2, 2));
    }
}
