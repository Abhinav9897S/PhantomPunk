package phantom.entities.player;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import phantom.audio.SoundId;
import phantom.components.Hitbox;
import phantom.entities.Damageable;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.systems.CombatSystem;
import phantom.util.Constants;
import phantom.world.WorldContext;

/**
 * The lantern-blade: forward, up and down slashes with a short active window.
 * Each target can be hit only once per swing.
 * <p>OOP concept: composition (a component of {@link Player}); nested enum for swing direction.</p>
 */
public final class PlayerAttack {
    /** Direction of a slash. */
    public enum Swing { FORWARD, UP, DOWN }

    private static final float VISUAL_TIME = 0.17f;

    private final Hitbox box = new Hitbox();
    private final Set<Damageable> alreadyHit = Collections.newSetFromMap(new IdentityHashMap<>());
    private Swing swing = Swing.FORWARD;
    private float cooldown;
    private float active;
    private float visual;
    private int swingCount;

    void update(Player p, InputSource in, WorldContext world, float dt) {
        if (cooldown > 0) cooldown -= dt;
        if (visual > 0) visual -= dt;
        if (active > 0) {
            active -= dt;
            strike(p, world);
        } else if (in.isJustPressed(GameAction.ATTACK) && cooldown <= 0 && p.canAttack()) {
            start(p, in, world);
        }
    }

    private void start(Player p, InputSource in, WorldContext world) {
        if (in.isDown(GameAction.UP)) swing = Swing.UP;
        else if (in.isDown(GameAction.DOWN) && !p.isOnGround()) swing = Swing.DOWN;
        else swing = Swing.FORWARD;
        active = Constants.ATTACK_ACTIVE;
        visual = VISUAL_TIME;
        cooldown = Constants.ATTACK_COOLDOWN;
        alreadyHit.clear();
        swingCount++;
        world.getEvents().publish(new Events.Sfx(SoundId.SLASH));
        strike(p, world);
    }

    private void strike(Player p, WorldContext world) {
        updateBox(p);
        CombatSystem.SlashOutcome outcome = world.getCombat().playerSlash(p, box, swing, alreadyHit, world);
        if (outcome.pogo()) {
            p.pogo(world);
            active = 0; // one bounce per swing
        } else if (outcome.hit() && swing == Swing.FORWARD) {
            p.recoil(p.getFacing().dx() * -180f);
        }
    }

    private void updateBox(Player p) {
        float dir = p.getFacing().dx();
        switch (swing) {
            case UP:
                box.set(p.centerX() - 30, p.getY() + p.getHeight() - 10, 60, 74);
                break;
            case DOWN:
                box.set(p.centerX() - 30, p.getY() - 66, 60, 74);
                break;
            default:
                float x = dir > 0 ? p.getX() + p.getWidth() - 6 : p.getX() - 70 + 6;
                box.set(x, p.getY() - 2, 70, 48);
                break;
        }
    }

    void cancel() {
        active = 0;
        visual = 0;
    }

    public boolean isSwinging() { return visual > 0; }
    public float getVisualProgress() { return 1f - Math.max(0f, visual) / VISUAL_TIME; }
    public Swing getSwing() { return swing; }
    public Hitbox getBox() { return box; }
    public int getSwingCount() { return swingCount; }
}
