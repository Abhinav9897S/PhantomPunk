package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.DamageInfo;
import phantom.entities.DamageKind;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.world.TileMap;
import phantom.world.WorldContext;

/**
 * A stalactite that shakes and drops when the player walks below it, then regrows.
 * Bosses also use it as one-off falling debris.
 * <p>OOP concept: hazard object with its own small state machine (an enum + switch).</p>
 */
public final class FallingRock extends Prop {
    private enum Phase { WAITING, SHAKING, FALLING, BROKEN }

    private static final Color STONE = new Color(0.35f, 0.36f, 0.4f, 1f);
    private final float homeX;
    private final float homeY;
    private final boolean regrows;
    private Phase phase = Phase.WAITING;
    private float timer;

    private FallingRock(float x, float y, boolean regrows) {
        super(x, y, 24f, 30f);
        homeX = x;
        homeY = y;
        this.regrows = regrows;
    }

    /** A trap hanging from the ceiling above the given cell. */
    public static FallingRock trap(TileMap map, int col, int row) {
        int r = row;
        while (r < map.getRows() - 1 && !map.isSolid(col, r + 1)) r++;
        float y = (r + 1) * Constants.TILE - 30f;
        return new FallingRock(col * Constants.TILE + 4f, y, true);
    }

    /** Debris that starts shaking immediately (boss attack). */
    public static FallingRock debris(TileMap map, float x, float fromY) {
        int c = TileMap.col(x);
        int r = TileMap.row(fromY);
        while (r < map.getRows() - 1 && !map.isSolid(c, r + 1)) r++;
        FallingRock rock = new FallingRock(x - 12f, (r + 1) * Constants.TILE - 30f, false);
        rock.phase = Phase.SHAKING;
        return rock;
    }

    @Override
    public void update(float dt, WorldContext world) {
        timer += dt;
        switch (phase) {
            case WAITING:
                float dx = Math.abs(world.getPlayer().centerX() - centerX());
                if (dx < 48f && world.getPlayer().getY() < getY()) change(Phase.SHAKING);
                break;
            case SHAKING:
                if (timer > 0.45f) change(Phase.FALLING);
                break;
            case FALLING:
                moveWithPhysics(world, dt, Constants.GRAVITY, Constants.MAX_FALL_SPEED, true);
                if (world.getPlayer().overlaps(this)) {
                    world.getPlayer().takeDamage(new DamageInfo(1, centerX(), getY() + 30, DamageKind.CONTACT), world);
                }
                if (isOnGround() || getY() <= 0) shatter(world);
                break;
            default:
                if (!regrows) remove();
                else if (timer > 4f) reset();
                break;
        }
    }

    private void shatter(WorldContext world) {
        world.getEffects().debris(getX(), getY(), getWidth(), 10, STONE, 10);
        world.shake(0.15f);
        world.getEvents().publish(new Events.Sfx(SoundId.BREAK));
        change(Phase.BROKEN);
    }

    private void reset() {
        teleport(homeX, homeY);
        setVelocity(0, 0);
        change(Phase.WAITING);
    }

    private void change(Phase next) {
        phase = next;
        timer = 0;
    }

    public boolean isFalling() { return phase == Phase.FALLING; }

    @Override
    public void render(RenderContext ctx, float alpha) {
        if (phase == Phase.BROKEN) return;
        float shake = phase == Phase.SHAKING ? (float) Math.sin(timer * 90f) * 2f : 0f;
        float grow = phase == Phase.WAITING && regrows ? Math.min(1f, 0.2f + timer * 2f) : 1f;
        ctx.sprite(ctx.art().sheet("rock"), 0, renderCenterX(alpha) + shake, renderY(alpha), false, grow, grow, 0f, 0f, 1f);
    }
}
