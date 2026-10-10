package phantom.systems;

import phantom.util.Poolable;

/**
 * One pooled particle (spore, spark, dust mote, wisp).
 * <p>OOP concept: implements {@link Poolable} so it can live in an {@code ObjectPool<Particle>}.</p>
 */
public final class Particle implements Poolable {
    private float x;
    private float y;
    private float vx;
    private float vy;
    private float life;
    private float maxLife;
    private float size;
    private float endSize;
    private float r;
    private float g;
    private float b;
    private float a;
    private float gravity;
    private float drag;
    private boolean additive;
    private float wobble;

    Particle set(float px, float py, float pvx, float pvy, float lifetime, float startSize, float finalSize) {
        x = px;
        y = py;
        vx = pvx;
        vy = pvy;
        life = lifetime;
        maxLife = lifetime;
        size = startSize;
        endSize = finalSize;
        return this;
    }

    Particle color(float cr, float cg, float cb, float ca) {
        r = cr;
        g = cg;
        b = cb;
        a = ca;
        return this;
    }

    Particle physics(float grav, float dragFactor, float wobbleAmount) {
        gravity = grav;
        drag = dragFactor;
        wobble = wobbleAmount;
        return this;
    }

    Particle additive(boolean add) {
        additive = add;
        return this;
    }

    /** Returns false when the particle has expired. */
    boolean update(float dt, float time) {
        life -= dt;
        if (life <= 0) return false;
        vy -= gravity * dt;
        float k = 1f - Math.min(1f, drag * dt);
        vx *= k;
        vy *= k;
        x += (vx + (float) Math.sin(time * 2.1f + y * 0.05f) * wobble) * dt;
        y += vy * dt;
        return true;
    }

    @Override
    public void reset() {
        set(0, 0, 0, 0, 0, 0, 0);
        color(1, 1, 1, 1);
        physics(0, 0, 0);
        additive = false;
    }

    /** 1 at birth, 0 at death. */
    public float lifeFraction() { return maxLife <= 0 ? 0 : life / maxLife; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getSize() { return endSize + (size - endSize) * lifeFraction(); }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    /** Fades in quickly and out slowly. */
    public float getAlpha() {
        float f = lifeFraction();
        return a * Math.min(1f, (1f - f) * 8f) * Math.min(1f, f * 1.6f);
    }
    public boolean isAdditive() { return additive; }
}
