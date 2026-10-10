package phantom.world;

import java.util.Random;

import phantom.entities.GameObject;
import phantom.entities.Projectile;
import phantom.entities.ProjectileKind;
import phantom.entities.Shade;
import phantom.entities.Team;
import phantom.entities.bosses.Boss;
import phantom.entities.player.Player;
import phantom.entities.props.Bench;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.save.SaveData;
import phantom.systems.CameraController;
import phantom.systems.CollisionSystem;
import phantom.systems.CombatSystem;
import phantom.systems.Effects;
import phantom.systems.ParticleSystem;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.util.ObjectPool;

/**
 * The running game world: the current room, its objects, the player and all gameplay systems.
 * It contains no rendering code, so it runs headless in tests.
 * <p>OOP concepts: implements {@link WorldContext}; <b>composition</b> of EntityManager, ParticleSystem,
 * CombatSystem, CollisionSystem, CameraController and an {@code ObjectPool<Projectile>}.</p>
 */
public final class GameWorld implements WorldContext {
    private final WorldData worldData;
    private final EventBus events;
    private final InputSource input;
    private final GameProgress progress = new GameProgress();
    private final Player player;
    private final EntityManager entities = new EntityManager();
    private final CollisionSystem collision = new CollisionSystem();
    private final CombatSystem combat = new CombatSystem();
    private final Random random = new Random();
    private final ParticleSystem particles = new ParticleSystem(random);
    private final Effects effects = new Effects(particles);
    private final CameraController camera = new CameraController(Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
    private final ObjectPool<Projectile> projectiles = new ObjectPool<>(Projectile::new, 128);
    private final EnemyFactory enemyFactory = new EnemyFactory();
    private final RoomTransition transition = new RoomTransition();
    private Room room;
    private BossArena arena;
    private Region lastRegion;
    private float time;
    private float hitStop;
    private float flash;
    private float lookTimer;

    public GameWorld(WorldData worldData, EventBus events, InputSource input, SaveData save) {
        this.worldData = worldData;
        this.events = events;
        this.input = input;
        this.player = new Player(0, 0, input);
        if (save != null) {
            progress.readFrom(save);
            player.getInventory().readFrom(save);
            player.getAbilities().readFrom(save);
        }
        player.applyUpgrades();
        player.getHealth().healFull();
        entities.setRemovalListener(o -> {
            if (o instanceof Projectile p) projectiles.free(p);
        });
        events.subscribe(Events.BossDefeated.class, e -> {
            if (arena != null) arena.onDefeated(this);
        });
    }

    /** Places the player at the last bench (or the starting room for a new game). */
    public void start() {
        String bench = progress.getBenchRoom();
        if (bench != null && worldData.has(bench)) respawn();
        else enterRoom(worldData.getStartRoom(), null);
    }

    public void update(float dt) {
        time += dt;
        progress.addPlayTime(dt);
        flash = Math.max(0f, flash - dt * 2.5f);
        if (transition.isActive()) {
            transition.update(dt, this);
        } else if (hitStop > 0) {
            hitStop -= dt;
        } else {
            simulate(dt);
        }
        events.dispatch();
    }

    private void simulate(float dt) {
        entities.update(dt, this);
        combat.resolveContacts(this);
        if (arena != null) arena.update(this, dt);
        checkDoors();
        effects.ambient(room.getRegion(), camera.getX(), camera.getY(), camera.getViewWidth(), camera.getViewHeight(), dt);
        particles.update(dt);
        updateCamera(dt);
    }

    private void updateCamera(float dt) {
        boolean still = player.isOnGround() && Math.abs(player.getVelocityX()) < 5f;
        boolean up = input.isDown(GameAction.UP);
        boolean down = input.isDown(GameAction.DOWN);
        lookTimer = still && (up || down) ? lookTimer + dt : 0f;
        float lookY = lookTimer > 0.45f ? (up ? 140f : -160f) : 0f;
        camera.update(player.centerX(), player.centerY(), player.getFacing().dx(), lookY,
                room.getTileMap().widthPx(), room.getTileMap().heightPx(), dt);
    }

    private void checkDoors() {
        if (player.isDead()) return;
        for (DoorData d : room.getData().getDoors()) {
            float x = d.col() * Constants.TILE;
            float y = d.row() * Constants.TILE;
            if (!player.getHitbox().overlaps(x, y, Constants.TILE, DoorData.HEIGHT * Constants.TILE)) continue;
            if (room.getTileMap().areGatesClosed()) continue;
            transition.begin(d);
            events.publish(new Events.Sfx(phantom.audio.SoundId.DOOR));
            return;
        }
    }

    /** Loads a room and places the player at the given door (or the room's start point). */
    public void enterRoom(String roomId, String doorId) {
        RoomData data = worldData.get(roomId);
        if (data == null) {
            System.err.println("[World] Missing room '" + roomId + "', staying put.");
            return;
        }
        entities.clear();
        particles.clear();
        room = new Room(data, progress);
        entities.add(player);
        arena = room.populate(entities, enemyFactory, progress);
        if (progress.hasShadeIn(roomId)) {
            entities.add(new Shade(progress.getShadeX(), progress.getShadeY(), progress.getShadeShards()));
        }
        DoorData door = doorId == null ? null : data.findDoor(doorId);
        if (door != null) {
            player.enterAt(door.arrivalX(player.getWidth()), door.arrivalY());
            player.setFacing(door.side().opposite());
        } else {
            player.enterAt(data.getStartCol() * Constants.TILE + 5, data.getStartRow() * Constants.TILE);
        }
        camera.snap(player.centerX(), player.centerY(), room.getTileMap().widthPx(), room.getTileMap().heightPx());
        boolean first = progress.discover(roomId);
        boolean regionChanged = lastRegion != data.getRegion();
        lastRegion = data.getRegion();
        events.publish(new Events.RoomEntered(roomId, data.getName(), data.getRegion(), first, regionChanged));
    }

    /** Returns the player to the last bench after death. */
    public void respawn() {
        String roomId = progress.getBenchRoom();
        if (roomId == null || !worldData.has(roomId)) roomId = worldData.getStartRoom();
        enterRoom(roomId, null);
        Bench bench = null;
        for (Bench b : entities.getAll(Bench.class)) {
            if (bench == null || b.getBenchId().equals(progress.getBenchId())) bench = b;
        }
        float x = bench != null ? bench.centerX() - player.getWidth() / 2f : player.getX();
        float y = bench != null ? bench.getY() : player.getY();
        player.revive(x, y);
        player.getSoul().empty();
        camera.snap(player.centerX(), player.centerY(), room.getTileMap().widthPx(), room.getTileMap().heightPx());
    }

    @Override
    public void onPlayerDeath() {
        progress.addDeath();
        int lost = player.getInventory().dropAllShards();
        progress.leaveShade(room.getId(), player.getSafeX(), player.getSafeY(), lost);
        events.publish(new Events.PlayerDied(lost));
    }

    /** Copies every saveable part of the game into a snapshot. */
    public SaveData snapshot() {
        SaveData data = new SaveData();
        progress.writeTo(data);
        player.getInventory().writeTo(data);
        player.getAbilities().writeTo(data);
        return data;
    }

    @Override
    public void spawn(GameObject object) {
        entities.add(object);
    }

    @Override
    public Projectile spawnProjectile(ProjectileKind kind, Team team, float cx, float cy, float vx, float vy, int damage) {
        Projectile p = projectiles.obtain();
        p.configure(kind, team, cx, cy, vx, vy, damage);
        entities.add(p);
        return p;
    }

    @Override
    public void hitStop(float seconds) { hitStop = Math.max(hitStop, seconds); }

    @Override
    public void shake(float trauma) { camera.addTrauma(trauma); }

    @Override
    public void flash(float strength) { flash = Math.max(flash, strength); }

    /** The boss whose health bar the HUD should show, or null. */
    public Boss getActiveBoss() { return arena == null ? null : arena.getActiveBoss(); }

    public Direction getPlayerFacing() { return player.getFacing(); }
    public float getFlash() { return flash; }
    public float getTransitionFade() { return transition.getFade(); }
    public CameraController getCamera() { return camera; }
    public Room getRoom() { return room; }
    public WorldData getWorldData() { return worldData; }
    public ObjectPool<Projectile> getProjectilePool() { return projectiles; }

    @Override public TileMap getTileMap() { return room.getTileMap(); }
    @Override public EntityManager getEntities() { return entities; }
    @Override public Player getPlayer() { return player; }
    @Override public Effects getEffects() { return effects; }
    @Override public ParticleSystem getParticles() { return particles; }
    @Override public EventBus getEvents() { return events; }
    @Override public CombatSystem getCombat() { return combat; }
    @Override public CollisionSystem getCollision() { return collision; }
    @Override public GameProgress getProgress() { return progress; }
    @Override public Region getRegion() { return room.getRegion(); }
    @Override public String getRoomId() { return room.getId(); }
    @Override public float getTime() { return time; }
    @Override public Random getRandom() { return random; }
}
