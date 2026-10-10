package phantom.entities;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import phantom.entities.player.Player;
import phantom.render.RenderContext;
import phantom.util.Poolable;
import phantom.world.WorldContext;

/**
 * A pooled projectile: the player's Light Bolt, sentinel orbs, boss gears, wisps and shockwaves.
 * <p>OOP concepts: inheritance from {@link Entity}; implements {@link Poolable} so the world can
 * recycle it through {@code ObjectPool<Projectile>}.</p>
 */
public final class Projectile extends Entity implements Poolable {
    private final Set<Damageable> pierced = Collections.newSetFromMap(new IdentityHashMap<>());
    private ProjectileKind kind = ProjectileKind.ORB;
    private Team team = Team.ENEMY;
    private int damage;
    private float age;
    private boolean dying;

    public Projectile() {
        super(0, 0, 10, 10);
    }

    /** Prepares a recycled projectile; (cx, cy) is its centre. */
    public void configure(ProjectileKind newKind, Team newTeam, float cx, float cy, float vx, float vy, int dmg) {
        kind = newKind;
        team = newTeam;
        damage = dmg;
        age = 0;
        dying = false;
        pierced.clear();
        clearRemoved();
        resize(kind.getWidth(), kind.getHeight());
        teleport(cx - kind.getWidth() / 2f, cy - kind.getHeight() / 2f);
        setVelocity(vx, vy);
    }

    @Override
    public void update(float dt, WorldContext world) {
        age += dt;
        if (age > kind.getLifetime()) {
            onDeath(world);
            return;
        }
        if (kind.isHoming()) steer(world.getPlayer(), dt);
        moveTo(getX() + getVelocityX() * dt, getY() + getVelocityY() * dt);
        if (hitsWall(world)) {
            onDeath(world);
            return;
        }
        if (world.getCombat().resolveProjectile(this, world)) onDeath(world);
    }

    private boolean hitsWall(WorldContext world) {
        if (kind == ProjectileKind.SHOCKWAVE) {
            float frontX = getVelocityX() > 0 ? getX() + getWidth() : getX();
            return world.getTileMap().isSolidAt(frontX, centerY());
        }
        return world.getTileMap().isSolidAt(centerX(), centerY());
    }

    private void steer(Player target, float dt) {
        if (age < 0.35f || target == null) return;
        float dx = target.centerX() - centerX();
        float dy = target.centerY() - centerY();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        float speed = 260f;
        float turn = Math.min(1f, 2.4f * dt);
        setVelocity(getVelocityX() + (dx / len * speed - getVelocityX()) * turn,
                getVelocityY() + (dy / len * speed - getVelocityY()) * turn);
    }

    @Override
    public void onDeath(WorldContext world) {
        if (dying) return;
        dying = true;
        world.getEffects().lightBurst(centerX(), centerY(), kind.getColor(), 10);
        remove();
    }

    /** Remembers a target already pierced so it is hit only once. */
    public boolean markPierced(Damageable target) {
        return pierced.add(target);
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float cx = renderX(alpha) + getWidth() / 2f;
        float cy = renderY(alpha) + getHeight() / 2f;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 30f);
        switch (kind) {
            case GEAR:
                ctx.spriteCentered(ctx.art().sheet("gear"), 0, cx, cy, false, 1f, 1f, age * 540f, 1f);
                ctx.glow(cx, cy, 20f, kind.getColor(), 0.4f);
                break;
            case LIGHT_BOLT:
                float dir = Math.signum(getVelocityX());
                ctx.spriteCentered(ctx.art().sheet("bolt"), 0, cx, cy, dir < 0, 1f, 1f, 0f, 1f);
                ctx.glow(cx, cy, 34f * pulse, kind.getColor(), 0.8f);
                break;
            default:
                ctx.glow(cx, cy, getWidth() * 1.6f * pulse, kind.getColor(), 0.9f);
                ctx.glow(cx, cy, getWidth() * 0.5f, com.badlogic.gdx.graphics.Color.WHITE, 0.9f);
                break;
        }
        ctx.light(cx, cy, kind == ProjectileKind.LIGHT_BOLT ? 220f : 120f, kind.getColor(), 0.9f);
    }

    @Override
    public void reset() {
        configure(ProjectileKind.ORB, Team.ENEMY, 0, 0, 0, 0, 0);
    }

    @Override
    public int getRenderLayer() { return 20; }

    public ProjectileKind getKind() { return kind; }
    public Team getTeam() { return team; }
    public int getDamage() { return damage; }
    public boolean isDying() { return dying; }
}
