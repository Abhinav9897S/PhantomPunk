package phantom.entities.player;

import phantom.abilities.AbilityType;
import phantom.abilities.Focus;
import phantom.abilities.WallCling;
import phantom.audio.SoundId;
import phantom.components.AbilitySet;
import phantom.components.Animator;
import phantom.components.Inventory;
import phantom.components.SoulMeter;
import phantom.components.Upgrade;
import phantom.entities.Character;
import phantom.entities.DamageInfo;
import phantom.entities.DamageKind;
import phantom.entities.GameObject;
import phantom.entities.Interactable;
import phantom.entities.Team;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.render.RenderContext;
import phantom.util.Constants;
import phantom.util.Direction;
import phantom.world.TileMap;
import phantom.world.WorldContext;

/**
 * Phantom, the player character.
 * <p>OOP concepts: <b>inheritance</b> ({@code GameObject -> Entity -> Character -> Player}),
 * <b>composition</b> (Inventory, AbilitySet, SoulMeter, Animator, PlayerMotor, PlayerAttack),
 * <b>encapsulation</b> (state changes go through methods such as {@link #takeDamage}).</p>
 */
public final class Player extends Character {
    private final InputSource input;
    private final Inventory inventory = new Inventory();
    private final AbilitySet abilities = new AbilitySet();
    private final SoulMeter soul = new SoulMeter(Constants.SOUL_MAX);
    private final Animator animator = new Animator();
    private final PlayerMotor motor = new PlayerMotor();
    private final PlayerAttack attack = new PlayerAttack();
    private final PlayerView view = new PlayerView();
    private float controlLock;
    private boolean wallLeft;
    private boolean wallRight;
    private float safeX;
    private float safeY;
    private float safeTimer;
    private boolean dead;
    private boolean resting;
    private Interactable nearby;

    public Player(float x, float y, InputSource input) {
        super(x, y, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT, Constants.PLAYER_BASE_MASKS);
        this.input = input;
        safeX = x;
        safeY = y;
    }

    @Override
    public void update(float dt, WorldContext world) {
        updateCharacter(dt);
        animator.update(dt);
        if (dead) return;
        if (controlLock > 0) controlLock -= dt;
        if (resting) {
            if (input.horizontal() != 0 || input.isJustPressed(GameAction.JUMP)) resting = false;
            else return;
        }
        boolean wasGrounded = isOnGround();
        float fallSpeed = getVelocityY();
        if (!abilities.controlsMovement()) motor.updateHorizontal(this, input, dt);
        motor.updateJump(this, input, world, dt);
        abilities.update(this, input, world, dt);

        float gravity = abilities.controlsMovement() ? 0f : Constants.GRAVITY;
        moveWithPhysics(world, dt, gravity, abilities.modifyMaxFall(Constants.MAX_FALL_SPEED), motor.isDroppingThrough());
        TileMap map = world.getTileMap();
        wallLeft = world.getCollision().isTouchingWall(this, map, Direction.LEFT);
        wallRight = world.getCollision().isTouchingWall(this, map, Direction.RIGHT);
        if (!wasGrounded && isOnGround()) land(world, fallSpeed);

        attack.update(this, input, world, dt);
        handleInteraction(world);
        handleHazards(world, dt);
        view.chooseClip(this);
    }

    private void land(WorldContext world, float impactSpeed) {
        abilities.refreshAll(this);
        float strength = Math.min(1f, -impactSpeed / Constants.MAX_FALL_SPEED);
        animator.squash(1f + 0.35f * strength, 1f - 0.3f * strength);
        if (strength > 0.3f) {
            world.getEffects().dust(centerX(), getY(), (int) (4 + strength * 8));
            world.getEvents().publish(new Events.Sfx(SoundId.LAND));
        }
    }

    private void handleInteraction(WorldContext world) {
        nearby = null;
        for (GameObject o : world.getEntities().view()) {
            if (o instanceof Interactable i && i.canInteract(this) && overlaps(i)) {
                nearby = i;
                break;
            }
        }
        if (nearby != null && isOnGround() && input.isJustPressed(GameAction.INTERACT) && !input.isDown(GameAction.ATTACK)) {
            nearby.interact(this, world);
        }
    }

    private void handleHazards(WorldContext world, float dt) {
        TileMap map = world.getTileMap();
        if (map.overlapsHazard(getHitbox())) {
            takeDamage(new DamageInfo(1, centerX(), getY() - 10, DamageKind.HAZARD), world);
            if (isAlive()) hazardRespawn(world);
            return;
        }
        boolean solidUnder = map.isStandableAt(getX() + 2, getY() - 2) && map.isStandableAt(getX() + getWidth() - 2, getY() - 2);
        boolean hazardNear = map.isHazard(TileMap.col(getX() - 20), TileMap.row(getY() + 4))
                || map.isHazard(TileMap.col(getX() + getWidth() + 20), TileMap.row(getY() + 4));
        if (isOnGround() && solidUnder && !hazardNear) {
            safeTimer += dt;
            if (safeTimer > 0.12f) {
                safeX = getX();
                safeY = getY();
            }
        } else {
            safeTimer = 0;
        }
    }

    private void hazardRespawn(WorldContext world) {
        teleport(safeX, safeY);
        setVelocity(0, 0);
        controlLock = 0.35f;
        abilities.resetAll();
        world.flash(0.6f);
    }

    @Override
    public boolean takeDamage(DamageInfo info, WorldContext world) {
        if (dead) return false;
        boolean hazard = info.kind() == DamageKind.HAZARD;
        float knockX = hazard ? 0 : Constants.KNOCKBACK_X;
        if (!applyHit(info, Constants.PLAYER_IFRAMES, knockX, hazard ? 0 : Constants.KNOCKBACK_Y)) return false;
        resting = false;
        controlLock = hazard ? 0 : 0.18f;
        abilities.onDamaged(this);
        abilities.resetAll();
        attack.cancel();
        world.hitStop(Constants.HITSTOP_HEAVY);
        world.shake(0.5f);
        world.getEffects().hitSparks(centerX(), centerY(), info.awayFrom(centerX()));
        world.getEvents().publish(new Events.PlayerDamaged(getHealth().getCurrent()));
        world.getEvents().publish(new Events.Sfx(SoundId.PLAYER_HURT));
        if (!isAlive()) {
            dead = true;
            setVelocity(0, 0);
            onDeath(world);
        }
        return true;
    }

    @Override
    public void onDeath(WorldContext world) {
        world.getEffects().enemyDeath(centerX(), centerY(), com.badlogic.gdx.graphics.Color.WHITE);
        world.onPlayerDeath();
    }

    /** Down-slash bounce: refreshes air moves like landing does. */
    void pogo(WorldContext world) {
        motor.bounce(this, Constants.POGO_VELOCITY);
        abilities.refreshAll(this);
        animator.squash(0.85f, 1.2f);
    }

    void recoil(float vx) {
        if (isOnGround()) setVelocityX(vx);
    }

    /** Restores the player after death or a new game. */
    public void revive(float x, float y) {
        dead = false;
        resting = false;
        getHealth().healFull();
        getHealth().makeInvincible(1f);
        teleport(x, y);
        setVelocity(0, 0);
        safeX = x;
        safeY = y;
        abilities.resetAll();
        motor.reset();
        attack.cancel();
    }

    /** Called after a room transition. */
    public void enterAt(float x, float y) {
        teleport(x, y);
        safeX = x;
        safeY = y;
        safeTimer = 0;
        motor.reset();
        attack.cancel();
        abilities.resetAll();
        resting = false;
    }

    public void rest() {
        resting = true;
        setVelocity(0, 0);
        getHealth().healFull();
    }

    /** Applies purchased upgrades to max health, soul capacity and damage. */
    public void applyUpgrades() {
        int masks = Constants.PLAYER_BASE_MASKS + (inventory.hasUpgrade(Upgrade.MASK_SHARD) ? 1 : 0);
        getHealth().setMax(masks, false);
        soul.setMax(Constants.SOUL_MAX + (inventory.hasUpgrade(Upgrade.EMBER_VESSEL) ? Constants.SOUL_COST : 0));
    }

    public void gainSoul(int amount, WorldContext world) {
        soul.add(amount);
        world.getEffects().soulGain(centerX(), centerY());
    }

    public int getNailDamage() {
        return Constants.NAIL_DAMAGE + (inventory.hasUpgrade(Upgrade.KEEN_EDGE) ? Constants.KEEN_EDGE_BONUS : 0);
    }

    public boolean isStandingOnOneWay(WorldContext world) {
        TileMap map = world.getTileMap();
        int r = TileMap.row(getY() - 2);
        int c0 = TileMap.col(getX() + 1);
        int c1 = TileMap.col(getX() + getWidth() - 1);
        boolean anyOneWay = false;
        for (int c = c0; c <= c1; c++) {
            if (map.isSolid(c, r)) return false;
            anyOneWay |= map.isOneWay(c, r);
        }
        return anyOneWay;
    }

    @Override
    public void render(RenderContext ctx, float alpha) {
        view.render(this, ctx, alpha);
    }

    @Override
    public int getRenderLayer() { return 10; }

    @Override
    public Team getTeam() { return Team.PLAYER; }

    public void lockControl(float seconds) { controlLock = Math.max(controlLock, seconds); }
    public boolean isControlLocked() { return controlLock > 0; }
    public boolean isTouchingWall(Direction side) { return side == Direction.LEFT ? wallLeft : wallRight; }
    public boolean isClinging() { return abilities.has(AbilityType.WALL_CLING) && abilities.get(AbilityType.WALL_CLING, WallCling.class).isClinging(); }
    public boolean isDashing() { return abilities.controlsMovement() && !isFocusing(); }
    public boolean isFocusing() { return abilities.get(AbilityType.FOCUS, Focus.class).getProgress() > 0; }
    public boolean canAttack() { return !dead && !resting && !abilities.controlsMovement(); }
    public boolean isDead() { return dead; }
    public float getSafeX() { return safeX; }
    public float getSafeY() { return safeY; }
    public boolean isResting() { return resting; }
    public Interactable getNearbyInteractable() { return nearby; }
    public Inventory getInventory() { return inventory; }
    public AbilitySet getAbilities() { return abilities; }
    public SoulMeter getSoul() { return soul; }
    public Animator getAnimator() { return animator; }
    public PlayerAttack getAttack() { return attack; }
    public InputSource getInput() { return input; }
}
