package phantom.entities.props;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.entities.DamageInfo;
import phantom.entities.Damageable;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.world.BreakableData;
import phantom.world.TileType;
import phantom.world.WorldContext;

/**
 * A cracked wall made of breakable tiles. Three strikes clear the tiles and open a secret.
 * <p>OOP concept: implements {@link Damageable} - the combat system treats it exactly like an
 * enemy (polymorphism), but it gives no Light.</p>
 */
public final class BreakableWall extends Prop implements Damageable {
    private static final Color STONE = new Color(0.4f, 0.42f, 0.45f, 1f);
    private static final int HITS = 3;
    private final BreakableData data;
    private int hitsLeft = HITS;
    private float shake;

    public BreakableWall(BreakableData data) {
        super(data.col() * Constants.TILE, data.row() * Constants.TILE,
                data.width() * Constants.TILE, data.height() * Constants.TILE);
        this.data = data;
    }

    @Override
    public void update(float dt, WorldContext world) {
        if (shake > 0) shake -= dt;
    }

    @Override
    public boolean takeDamage(DamageInfo info, WorldContext world) {
        if (hitsLeft <= 0) return false;
        hitsLeft--;
        shake = 0.15f;
        world.getEffects().debris(getX(), getY(), getWidth(), getHeight(), STONE, 5);
        world.getEvents().publish(new Events.Sfx(SoundId.BREAK));
        if (hitsLeft == 0) shatter(world);
        return true;
    }

    private void shatter(WorldContext world) {
        for (int r = data.row(); r < data.row() + data.height(); r++) {
            for (int c = data.col(); c < data.col() + data.width(); c++) {
                if (world.getTileMap().get(c, r) == TileType.BREAKABLE) world.getTileMap().set(c, r, TileType.EMPTY);
            }
        }
        world.getProgress().markWallBroken(data.id());
        world.getEffects().debris(getX(), getY(), getWidth(), getHeight(), STONE, 30);
        world.shake(0.4f);
        world.getEvents().publish(new Events.Toast("A hidden passage"));
        remove();
    }

    @Override
    public boolean isAlive() { return hitsLeft > 0; }

    @Override
    public Team getTeam() { return Team.NEUTRAL; }

    @Override
    public boolean givesSoul() { return false; }

    @Override
    public boolean isPogoTarget() { return false; }

    /** 0..1 how damaged the wall is, for the tile renderer's cracks. */
    public float getDamage() { return 1f - hitsLeft / (float) HITS; }

    public float getShakeOffset() {
        return shake > 0 ? (float) Math.sin(shake * 120f) * 3f : 0f;
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float d = getDamage();
        if (d <= 0) return;
        var cracks = ctx.art().sheet("cracks");
        for (int r = 0; r < data.height(); r++) {
            for (int c = 0; c < data.width(); c++) {
                float x = getX() + c * Constants.TILE + Constants.TILE / 2f + getShakeOffset();
                ctx.sprite(cracks, (c + r) % cracks.count(), x, getY() + r * Constants.TILE, false, 1f, 1f, 0f, 0f, d);
            }
        }
    }
}
