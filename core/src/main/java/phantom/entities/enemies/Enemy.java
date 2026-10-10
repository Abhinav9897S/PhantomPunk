package phantom.entities.enemies;

import com.badlogic.gdx.graphics.Color;

import phantom.ai.EnemyState;
import phantom.ai.StateMachine;
import phantom.art.SpriteSheet;
import phantom.audio.SoundId;
import phantom.components.Animator;
import phantom.entities.Character;
import phantom.entities.DamageInfo;
import phantom.entities.ShardPickup;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.world.WorldContext;

/**
 * Base class of every enemy and boss. Handles damage, death, shard drops and physics;
 * subclasses plug their AI into a {@link StateMachine} and draw themselves.
 * <p>OOP concepts: <b>abstract class</b> ({@link #getAi()}, {@link #onHurt}, {@link #render} are left
 * to subclasses), <b>inheritance</b> from {@link Character}, <b>template method</b> in {@link #update}.</p>
 */
public abstract class Enemy extends Character {
    private static final float DEATH_LINGER = 0.45f;

    private final String displayName;
    private final int contactDamage;
    private final int shardValue;
    private final Animator animator = new Animator();
    private float deathTimer;

    protected Enemy(String displayName, float x, float y, float w, float h, int hp, int contactDamage, int shardValue) {
        super(x, y, w, h, hp);
        this.displayName = displayName;
        this.contactDamage = contactDamage;
        this.shardValue = shardValue;
    }

    /** The enemy's state machine (each subclass owns a typed one). */
    public abstract StateMachine<?> getAi();

    /** Reaction to a hit that did not kill (e.g. switch to STUNNED). */
    protected abstract void onHurt(DamageInfo info, WorldContext world);

    /** Accent colour used for glowing eyes and the death burst. */
    protected abstract Color getGlowColor();

    @Override
    public void update(float dt, WorldContext world) {
        updateCharacter(dt);
        animator.update(dt);
        if (!isAlive()) {
            deathTimer += dt;
            setVelocityX(getVelocityX() * 0.9f);
            stepPhysics(world, dt);
            if (deathTimer > deathLinger()) remove();
            return;
        }
        getAi().update(world, dt);
    }

    @Override
    public boolean takeDamage(DamageInfo info, WorldContext world) {
        if (!isAlive() || !canBeHurt()) return false;
        if (!applyHit(info, 0f, getKnockback(), isFlying() ? 0f : 140f * (getKnockback() > 0 ? 1 : 0))) return false;
        if (isAlive()) {
            onHurt(info, world);
        } else {
            die(world);
        }
        return true;
    }

    private void die(WorldContext world) {
        getAi().change(EnemyState.DEAD);
        world.getProgress().addKill();
        world.getEffects().enemyDeath(centerX(), centerY(), getGlowColor());
        world.getEvents().publish(new Events.EnemyKilled(displayName, centerX(), centerY()));
        world.getEvents().publish(new Events.Sfx(SoundId.ENEMY_DEATH));
        dropShards(world, shardValue);
        world.shake(0.25f);
        onDeath(world);
    }

    /** Scatters shard pickups worth {@code total} shards. */
    protected void dropShards(WorldContext world, int total) {
        int remaining = total;
        while (remaining > 0) {
            int piece = remaining >= 5 && world.getRandom().nextFloat() < 0.4f ? 5 : 1;
            remaining -= piece;
            float vx = (world.getRandom().nextFloat() - 0.5f) * 320f;
            float vy = 220f + world.getRandom().nextFloat() * 260f;
            world.spawn(new ShardPickup(centerX(), centerY(), piece, vx, vy));
        }
    }

    @Override
    public void onDeath(WorldContext world) {
        // Default: nothing extra. Bosses override this.
    }

    /** Gravity + tile collision, or flying collision for flying enemies. */
    public void stepPhysics(WorldContext world, float dt) {
        if (isFlying()) {
            world.getCollision().move(this, world.getTileMap(), dt, false);
        } else {
            moveWithPhysics(world, dt, Constants.GRAVITY, Constants.MAX_FALL_SPEED, false);
        }
    }

    /** Turns around at walls and ledges; returns true if it turned. */
    protected boolean turnAtEdges(WorldContext world) {
        Direction f = getFacing();
        boolean blocked = world.getCollision().hasWallAhead(this, world.getTileMap(), f);
        boolean ledge = isOnGround() && !world.getCollision().hasGroundAhead(this, world.getTileMap(), f);
        if (blocked || ledge) {
            setFacing(f.opposite());
            return true;
        }
        return false;
    }

    /** Draws the enemy's sprite with hit flash and fade-out on death. */
    protected void drawSprite(RenderContext ctx, float alpha, SpriteSheet sheet, int frame, float sx, float sy, float rotation) {
        float fade = isAlive() ? 1f : Math.max(0f, 1f - deathTimer / deathLinger());
        ctx.sprite(sheet, frame, renderCenterX(alpha), renderY(alpha), getFacing() == Direction.LEFT,
                sx, sy, rotation, getFlashAmount(), fade);
    }

    protected float deathLinger() { return DEATH_LINGER; }
    protected boolean canBeHurt() { return true; }
    protected float getKnockback() { return 260f; }
    public boolean isFlying() { return false; }
    public boolean isHarmful() { return isAlive(); }
    public int getContactDamage() { return contactDamage; }
    public String getDisplayName() { return displayName; }
    public Animator getAnimator() { return animator; }
    protected float getDeathTimer() { return deathTimer; }

    @Override
    public Team getTeam() { return Team.ENEMY; }

    @Override
    public int getRenderLayer() { return 5; }
}
