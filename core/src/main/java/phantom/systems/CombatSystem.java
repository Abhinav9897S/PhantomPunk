package phantom.systems;

import java.util.Set;

import phantom.audio.SoundId;
import phantom.components.Hitbox;
import phantom.entities.DamageInfo;
import phantom.entities.DamageKind;
import phantom.entities.Damageable;
import phantom.entities.GameObject;
import phantom.entities.Projectile;
import phantom.entities.Team;
import phantom.entities.enemies.Enemy;
import phantom.entities.player.Player;
import phantom.entities.player.PlayerAttack;
import phantom.events.Events;
import phantom.util.Constants;
import phantom.world.TileType;
import phantom.world.WorldContext;

/**
 * Resolves all hits: player slashes, enemy contact damage and projectiles.
 * Works only through the {@link Damageable} interface, so new hittable things need no changes here.
 * <p>OOP concept: <b>polymorphism via interfaces</b>; a record for the slash result.</p>
 */
public final class CombatSystem {
    /** What a slash connected with. */
    public record SlashOutcome(boolean hit, boolean pogo) { }

    private static final SlashOutcome NOTHING = new SlashOutcome(false, false);

    /** Applies a player slash hitbox to every damageable object in the room. */
    public SlashOutcome playerSlash(Player player, Hitbox box, PlayerAttack.Swing swing,
                                    Set<Damageable> alreadyHit, WorldContext world) {
        boolean hit = false;
        boolean pogo = false;
        DamageKind kind = swing == PlayerAttack.Swing.DOWN ? DamageKind.DOWN_SLASH : DamageKind.SLASH;
        for (GameObject o : world.getEntities().view()) {
            if (!(o instanceof Damageable target) || target == player || target.getTeam() == Team.PLAYER) continue;
            if (!target.isAlive() || alreadyHit.contains(target) || !box.overlaps(target.getHitbox())) continue;
            alreadyHit.add(target);
            DamageInfo info = new DamageInfo(player.getNailDamage(), player.centerX(), player.centerY(), kind);
            if (target.takeDamage(info, world)) {
                hit = true;
                if (target.givesSoul()) player.gainSoul(Constants.SOUL_PER_HIT, world);
                Hitbox t = target.getHitbox();
                float hx = (Math.max(box.getX(), t.getX()) + Math.min(box.right(), t.right())) * 0.5f;
                float hy = (Math.max(box.getY(), t.getY()) + Math.min(box.top(), t.top())) * 0.5f;
                world.getEffects().hitSparks(hx, hy, player.getFacing().dx());
                world.hitStop(Constants.HITSTOP_LIGHT);
                world.shake(0.14f);
                world.getEvents().publish(new Events.Sfx(SoundId.HIT));
            }
            if (kind == DamageKind.DOWN_SLASH && target.isPogoTarget()) pogo = true;
        }
        if (kind == DamageKind.DOWN_SLASH && touchesHazard(box, world)) {
            pogo = true;
            world.getEffects().hitSparks(box.centerX(), box.getY() + 20, 0);
        }
        return hit || pogo ? new SlashOutcome(hit, pogo) : NOTHING;
    }

    private boolean touchesHazard(Hitbox box, WorldContext world) {
        return world.getTileMap().overlapsType(box, TileType.SPIKES) || world.getTileMap().overlapsType(box, TileType.MIST);
    }

    /** Enemies hurt the player by touching them. */
    public void resolveContacts(WorldContext world) {
        Player player = world.getPlayer();
        if (player == null || player.isDead()) return;
        for (GameObject o : world.getEntities().view()) {
            if (o instanceof Enemy enemy && enemy.isAlive() && enemy.isHarmful() && enemy.overlaps(player)) {
                player.takeDamage(new DamageInfo(enemy.getContactDamage(), enemy.centerX(), enemy.centerY(),
                        DamageKind.CONTACT), world);
                return;
            }
        }
    }

    /** Applies a projectile's damage. Returns true if the projectile should be destroyed. */
    public boolean resolveProjectile(Projectile p, WorldContext world) {
        if (p.getTeam() == Team.ENEMY) {
            Player player = world.getPlayer();
            if (player != null && !player.isDead() && p.overlaps(player)) {
                player.takeDamage(new DamageInfo(p.getDamage(), p.centerX(), p.centerY(), DamageKind.PROJECTILE), world);
                return true;
            }
            return false;
        }
        for (GameObject o : world.getEntities().view()) {
            if (!(o instanceof Damageable target) || target.getTeam() == Team.PLAYER || !target.isAlive()) continue;
            if (!p.overlaps(target) || !p.markPierced(target)) continue;
            if (target.takeDamage(new DamageInfo(p.getDamage(), p.centerX(), p.centerY(), DamageKind.BOLT), world)) {
                world.getEffects().hitSparks(p.centerX(), p.centerY(), Math.signum(p.getVelocityX()));
                world.hitStop(Constants.HITSTOP_LIGHT);
                world.getEvents().publish(new Events.Sfx(SoundId.HIT));
            }
            if (!p.getKind().isPiercing()) return true;
        }
        return false;
    }
}
