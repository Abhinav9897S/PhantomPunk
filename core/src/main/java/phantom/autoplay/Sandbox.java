package phantom.autoplay;

import java.util.Random;

import phantom.entities.GameObject;
import phantom.entities.Projectile;
import phantom.entities.ProjectileKind;
import phantom.entities.Team;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.input.ScriptedInput;
import phantom.systems.CollisionSystem;
import phantom.systems.CombatSystem;
import phantom.systems.Effects;
import phantom.systems.ParticleSystem;
import phantom.util.Constants;
import phantom.world.EntityManager;
import phantom.world.GameProgress;
import phantom.world.Region;
import phantom.world.TileMap;
import phantom.world.WorldContext;

/**
 * An empty copy of the current room holding a single "ghost" of the player. The autoplay AI copies
 * Phantom's exact movement state into the ghost and runs the real player physics a few seconds ahead,
 * so it knows where a jump will land before it presses a key.
 * <p>OOP concept: a second implementation of {@link WorldContext} - the {@link Player} class runs
 * unchanged against it (dependency inversion makes the prediction exact).</p>
 */
final class Sandbox implements WorldContext {
    private final ScriptedInput input = new ScriptedInput();
    private final Player ghost = new Player(0, 0, input);
    private final EntityManager entities = new EntityManager();
    private final CollisionSystem collision = new CollisionSystem();
    private final CombatSystem combat = new CombatSystem();
    private final Random random = new Random(7);
    private final ParticleSystem particles = new ParticleSystem(random);
    private final Effects effects = new Effects(particles);
    private final EventBus events = new EventBus();
    private final GameProgress progress = new GameProgress();
    private final Projectile scratch = new Projectile();
    private TileMap map;
    private Region region = Region.WHISPERING_ROOTS;
    private String roomId = "";
    private float time;
    private boolean hurt;

    /** Points the sandbox at a room's live tile map. */
    void setRoom(TileMap map, Region region, String roomId) {
        this.map = map;
        this.region = region;
        this.roomId = roomId;
    }

    /** Makes the ghost an exact movement copy of {@code source}, with full health and no input held. */
    Player reset(Player source) {
        ghost.copyMotionStateFrom(source);
        prepare();
        // carry over which buttons are held, so "press" in the sandbox means the same as in the game
        if (source.getInput() != null) {
            for (phantom.input.GameAction a : phantom.input.GameAction.values()) {
                if (source.getInput().isDown(a)) input.press(a);
            }
            input.poll();
        }
        return ghost;
    }

    /** Places a ghost with the given abilities standing still at a canonical position. */
    Player resetStanding(Player abilitiesFrom, float x, float y) {
        ghost.copyMotionStateFrom(abilitiesFrom);
        ghost.enterAt(x, y);
        ghost.setFacing(phantom.util.Direction.RIGHT);
        ghost.setVelocity(0, 0);
        prepare();
        // settle onto the floor so ground contact and coyote time match a standing player
        input.releaseAll();
        step();
        step();
        return ghost;
    }

    private void prepare() {
        ghost.getHealth().update(100f);
        ghost.getHealth().healFull();
        input.releaseAll();
        input.poll();
        input.poll();
        particles.clear();
        events.clear();
        hurt = false;
    }

    /** Advances the ghost one fixed step with the currently held input. Returns false if it was hurt. */
    boolean step() {
        input.poll();
        ghost.beginTick();
        ghost.update(Constants.STEP, this);
        time += Constants.STEP;
        events.clear();
        if (particles.getLiveCount() > 1500) particles.clear();
        if (ghost.getHealth().getCurrent() < ghost.getHealth().getMax()) hurt = true;
        return !hurt;
    }

    /** Where a simulated manoeuvre ended. */
    record Outcome(boolean ok, float x, float y, float centerX, int ticks) { }

    /** Runs a manoeuvre on the ghost (already reset) until it finishes, fails, gets hurt or times out. */
    Outcome run(Maneuver m, int maxTicks) {
        return run(m, maxTicks, null, 0f);
    }

    /** As {@link #run(Maneuver, int)}, but also fails if the ghost touches a predicted danger. */
    Outcome run(Maneuver m, int maxTicks, Dangers dangers, float invulnerable) {
        java.util.Set<phantom.input.GameAction> buttons = Maneuver.newButtons();
        for (int i = 0; i < maxTicks; i++) {
            Maneuver.Status s = m.step(ghost, buttons);
            if (s != Maneuver.Status.RUNNING) return outcome(s == Maneuver.Status.DONE, i);
            input.releaseAll();
            for (phantom.input.GameAction a : buttons) input.press(a);
            if (!step()) return outcome(false, i);
            float t = (i + 1) * Constants.STEP;
            if (dangers != null && t > invulnerable && dangers.hits(t, ghost)) return outcome(false, i);
        }
        return outcome(false, maxTicks);
    }

    private Outcome outcome(boolean ok, int ticks) {
        return new Outcome(ok, ghost.getX(), ghost.getY(), ghost.centerX(), ticks);
    }

    ScriptedInput input() { return input; }

    @Override public TileMap getTileMap() { return map; }
    @Override public EntityManager getEntities() { return entities; }
    @Override public Player getPlayer() { return ghost; }
    @Override public Effects getEffects() { return effects; }
    @Override public ParticleSystem getParticles() { return particles; }
    @Override public EventBus getEvents() { return events; }
    @Override public CombatSystem getCombat() { return combat; }
    @Override public CollisionSystem getCollision() { return collision; }
    @Override public GameProgress getProgress() { return progress; }
    @Override public Region getRegion() { return region; }
    @Override public String getRoomId() { return roomId; }
    @Override public float getTime() { return time; }
    @Override public Random getRandom() { return random; }
    @Override public void spawn(GameObject object) { }

    @Override
    public Projectile spawnProjectile(ProjectileKind kind, Team team, float cx, float cy, float vx, float vy, int damage) {
        scratch.configure(kind, team, cx, cy, vx, vy, damage);
        return scratch;
    }

    @Override public void hitStop(float seconds) { }
    @Override public void shake(float trauma) { }
    @Override public void flash(float strength) { }
    @Override public void onPlayerDeath() { }
}
