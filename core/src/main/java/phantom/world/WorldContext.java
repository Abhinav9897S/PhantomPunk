package phantom.world;

import java.util.Random;

import phantom.entities.GameObject;
import phantom.entities.Projectile;
import phantom.entities.ProjectileKind;
import phantom.entities.Team;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.systems.CollisionSystem;
import phantom.systems.CombatSystem;
import phantom.systems.Effects;
import phantom.systems.ParticleSystem;

/**
 * The services an object may use while updating: the map, other entities, effects and events.
 * <p>OOP concept: <b>interface segregation / dependency inversion</b> - entities depend on this
 * abstraction rather than on the concrete {@link GameWorld}, which also makes them testable.</p>
 */
public interface WorldContext {
    TileMap getTileMap();

    EntityManager getEntities();

    Player getPlayer();

    Effects getEffects();

    ParticleSystem getParticles();

    EventBus getEvents();

    CombatSystem getCombat();

    CollisionSystem getCollision();

    GameProgress getProgress();

    Region getRegion();

    String getRoomId();

    float getTime();

    Random getRandom();

    void spawn(GameObject object);

    Projectile spawnProjectile(ProjectileKind kind, Team team, float cx, float cy, float vx, float vy, int damage);

    /** Freezes the simulation briefly to sell the weight of a hit. */
    void hitStop(float seconds);

    void shake(float trauma);

    /** Brief full-screen white flash (0..1). */
    void flash(float strength);

    void onPlayerDeath();
}
