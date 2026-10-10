package phantom.world.validation;

import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.util.Constants;

/**
 * What the player can physically do with a given set of abilities, derived from the same
 * constants the real physics uses (jump velocity, gravity, run and dash speed), with a safety
 * margin so the validator never promises a jump the game cannot deliver.
 * <p>OOP concept: immutable value object computed from {@link Constants}.</p>
 */
public final class MovementModel {
    private static final float HORIZONTAL_SAFETY = 0.85f;
    private static final float VERTICAL_SAFETY_PX = 8f;

    private final float launchVelocity;
    private final float apexPx;
    private final float dashBonusPx;
    private final boolean wallClimb;
    private final boolean doubleJump;

    public MovementModel(Set<AbilityType> abilities) {
        doubleJump = abilities.contains(AbilityType.DOUBLE_JUMP);
        float v = Constants.JUMP_VELOCITY;
        float v2 = doubleJump ? Constants.DOUBLE_JUMP_VELOCITY : 0f;
        // energy of both jumps combined: a conservative single "virtual" jump
        launchVelocity = (float) Math.sqrt(v * v + v2 * v2);
        apexPx = launchVelocity * launchVelocity / (2f * Constants.GRAVITY);
        dashBonusPx = abilities.contains(AbilityType.DASH) ? Constants.DASH_SPEED * Constants.DASH_TIME * 0.9f : 0f;
        wallClimb = abilities.contains(AbilityType.WALL_CLING);
    }

    /** Highest ledge (in tiles above the feet) the player can land on. */
    public int maxRiseTiles() {
        return (int) Math.floor((apexPx - VERTICAL_SAFETY_PX) / Constants.TILE);
    }

    /**
     * Horizontal distance (px) the player can cover while ending {@code dyPx} above (positive)
     * or below (negative) the take-off height.
     */
    public float horizontalReachPx(float dyPx) {
        if (dyPx > apexPx - VERTICAL_SAFETY_PX) return -1f;
        float g = Constants.GRAVITY;
        float v = launchVelocity;
        double disc = v * v - 2.0 * g * dyPx;
        if (disc < 0) return -1f;
        double t = (v + Math.sqrt(disc)) / g;
        return (float) (Constants.RUN_SPEED * t * HORIZONTAL_SAFETY) + dashBonusPx;
    }

    /** Horizontal travel needed to get from one column to another (feet may overhang edges). */
    public static float requiredTravelPx(int dxTiles) {
        return Math.max(0f, (Math.abs(dxTiles) - 1) * Constants.TILE - (Constants.PLAYER_WIDTH - 2f));
    }

    /** Widest horizontal reach in tiles, used to bound the search. */
    public int maxReachTiles() {
        return (int) Math.ceil(horizontalReachPx(-12 * Constants.TILE) / Constants.TILE) + 1;
    }

    public boolean canWallClimb() { return wallClimb; }
    public boolean hasDoubleJump() { return doubleJump; }
}
