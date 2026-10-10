package phantom.components;

import phantom.util.GameMath;

/**
 * Tracks the current animation clip, its time and "bone-style" squash and stretch.
 * <p>OOP concept: composition - entities delegate animation timing to this component.</p>
 */
public final class Animator {
    private String clip = "";
    private float time;
    private float scaleX = 1f;
    private float scaleY = 1f;

    /** Switches clip (restarting its time) only when it actually changes. */
    public void play(String name) {
        if (!clip.equals(name)) {
            clip = name;
            time = 0f;
        }
    }

    /** Squash or stretch the sprite; it springs back to 1 over time. */
    public void squash(float sx, float sy) {
        scaleX = sx;
        scaleY = sy;
    }

    public void update(float dt) {
        time += dt;
        scaleX = GameMath.damp(scaleX, 1f, 14f, dt);
        scaleY = GameMath.damp(scaleY, 1f, 14f, dt);
    }

    /** Frame index for a clip with {@code frames} frames at {@code fps}. */
    public int frame(int frames, float fps, boolean loop) {
        int f = (int) (time * fps);
        return loop ? f % frames : Math.min(f, frames - 1);
    }

    public String getClip() { return clip; }
    public float getTime() { return time; }
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
}
