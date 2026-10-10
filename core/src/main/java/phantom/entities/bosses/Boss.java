package phantom.entities.bosses;

import java.util.List;

import phantom.ai.EnemyState;
import phantom.ai.StateMachine;
import phantom.audio.SoundId;
import phantom.entities.DamageInfo;
import phantom.entities.enemies.Enemy;
import phantom.events.Events;
import phantom.util.GameMath;
import phantom.world.WorldContext;

/**
 * Base class for the three guardians. Adds phases (health thresholds), an intro roar,
 * attack selection, and a long death sequence on top of {@link Enemy}.
 * <p>OOP concepts: <b>abstract class</b> ({@link #attacksFor}, {@link #reposition}) and
 * <b>composition</b> of {@link BossAttack} strategies; state machine IDLE / PATROL / ATTACK / STUNNED.</p>
 */
public abstract class Boss extends Enemy {
    private final String bossId;
    private final String title;
    private final String subtitle;
    private final float[] phaseThresholds;
    private final StateMachine<Boss> ai = new StateMachine<>(this, EnemyState.IDLE);
    private BossAttack current;
    private BossAttack last;
    private int phase;
    private boolean awake;
    private float fade = 1f;

    protected Boss(String bossId, String title, String subtitle, float x, float y, float w, float h,
                   int hp, int shards, float... phaseThresholds) {
        super(title, x, y, w, h, hp, 1, shards);
        this.bossId = bossId;
        this.title = title;
        this.subtitle = subtitle;
        this.phaseThresholds = phaseThresholds.clone();
        ai.on(EnemyState.IDLE, (b, w2, dt) -> b.stepPhysics(w2, dt))
          .on(EnemyState.PATROL, Boss::between)
          .on(EnemyState.ATTACK, Boss::attacking)
          .on(EnemyState.STUNNED, Boss::roaring);
    }

    /** The attack pool for a phase (0 = first). */
    protected abstract List<BossAttack> attacksFor(int phase);

    /** Movement between attacks. */
    protected abstract void reposition(WorldContext world, float dt);

    /** Rest time between attacks for a phase. */
    protected abstract float cooldownFor(int phase);

    /** True for the final boss (its defeat ends the game). */
    public abstract boolean isFinal();

    /** Starts the fight after the arena has locked. */
    public void awaken(WorldContext world) {
        if (awake) return;
        awake = true;
        ai.change(EnemyState.STUNNED);
        world.shake(0.6f);
        world.getEvents().publish(new Events.Sfx(SoundId.BOSS_ROAR));
    }

    private void between(WorldContext world, float dt) {
        reposition(world, dt);
        if (ai.getTimeInState() >= cooldownFor(phase)) chooseAttack(world);
    }

    private void chooseAttack(WorldContext world) {
        List<BossAttack> pool = attacksFor(phase);
        BossAttack next = pool.get(world.getRandom().nextInt(pool.size()));
        if (next == last && pool.size() > 1) next = pool.get((pool.indexOf(next) + 1) % pool.size());
        current = next;
        last = next;
        current.begin(this, world);
        ai.change(EnemyState.ATTACK);
    }

    private void attacking(WorldContext world, float dt) {
        if (current == null || current.update(this, world, dt)) {
            current = null;
            ai.change(EnemyState.PATROL);
        }
    }

    private void roaring(WorldContext world, float dt) {
        setVelocityX(GameMath.approach(getVelocityX(), 0f, 1500f * dt));
        stepPhysics(world, dt);
        if (ai.getTimeInState() > 1.1f) ai.change(EnemyState.PATROL);
    }

    @Override
    protected void onHurt(DamageInfo info, WorldContext world) {
        if (phase < phaseThresholds.length && getHealth().fraction() <= phaseThresholds[phase]) {
            phase++;
            current = null;
            setFade(1f);
            ai.change(EnemyState.STUNNED);
            world.shake(0.7f);
            world.flash(0.35f);
            world.getEvents().publish(new Events.Sfx(SoundId.BOSS_ROAR));
            onPhaseChange(phase, world);
        }
    }

    /** Hook for phase-change effects. */
    protected void onPhaseChange(int newPhase, WorldContext world) { }

    @Override
    public void update(float dt, WorldContext world) {
        if (!isAlive() && world.getRandom().nextFloat() < 0.35f) {
            float x = getX() + world.getRandom().nextFloat() * getWidth();
            float y = getY() + world.getRandom().nextFloat() * getHeight();
            world.getEffects().lightBurst(x, y, getGlowColor(), 6);
            world.shake(0.08f);
        }
        super.update(dt, world);
    }

    @Override
    public void onDeath(WorldContext world) {
        world.getEffects().bossDeath(centerX(), centerY(), getGlowColor());
        world.shake(1f);
        world.flash(0.8f);
        world.hitStop(0.35f);
        world.getProgress().markBossDefeated(bossId);
        world.getEvents().publish(new Events.BossDefeated(bossId, title, isFinal()));
    }

    @Override
    protected boolean canBeHurt() { return awake && !ai.is(EnemyState.STUNNED); }

    @Override
    public boolean isHarmful() { return awake && isAlive() && fade > 0.5f; }

    @Override
    protected float getKnockback() { return 0f; }

    @Override
    protected float deathLinger() { return 2.2f; }

    @Override
    public int getRenderLayer() { return 4; }

    /** Current pose for the renderer: the attack's pose, "roar", or "idle". */
    public String getPose() {
        if (!isAlive()) return "roar";
        if (ai.is(EnemyState.STUNNED)) return "roar";
        if (ai.is(EnemyState.ATTACK) && current != null) return current.getPose();
        return "idle";
    }

    public boolean isTelegraphing() {
        return ai.is(EnemyState.STUNNED) || (current != null && current.isTelegraphing());
    }

    public void setFade(float value) { fade = GameMath.clamp(value, 0f, 1f); }
    public float getFade() { return fade; }
    public boolean isAwake() { return awake; }
    public int getPhase() { return phase; }
    public String getBossId() { return bossId; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }

    @Override
    public StateMachine<Boss> getAi() { return ai; }
}
