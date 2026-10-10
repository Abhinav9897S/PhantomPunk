package phantom.abilities;

import phantom.audio.SoundId;
import phantom.entities.ProjectileKind;
import phantom.entities.Team;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.util.Constants;
import phantom.world.WorldContext;

/**
 * Light Bolt: spends Light to throw a piercing projectile.
 * <p>OOP concept: implements {@link Ability}; uses the world's projectile pool via {@link WorldContext}.</p>
 */
public final class LightBolt implements Ability {
    private float cooldown;

    @Override
    public AbilityType getType() {
        return AbilityType.LIGHT_BOLT;
    }

    @Override
    public void update(Player player, InputSource input, WorldContext world, float dt) {
        if (cooldown > 0) cooldown -= dt;
        if (!input.isJustPressed(GameAction.BOLT) || cooldown > 0) return;
        if (!player.getSoul().spend(Constants.SOUL_COST)) {
            world.getEvents().publish(new Events.Sfx(SoundId.UI_DENY));
            return;
        }
        cooldown = 0.4f;
        float dir = player.getFacing().dx();
        float x = player.centerX() + dir * 18f;
        float y = player.getY() + player.getHeight() * 0.55f;
        world.spawnProjectile(ProjectileKind.LIGHT_BOLT, Team.PLAYER, x, y, dir * Constants.BOLT_SPEED, 0f,
                Constants.BOLT_DAMAGE);
        player.setVelocityX(-dir * 160f);
        player.getAnimator().squash(1.2f, 0.85f);
        world.shake(0.15f);
        world.getEvents().publish(new Events.Sfx(SoundId.BOLT));
    }
}
