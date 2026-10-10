package phantom.screens;

import com.badlogic.gdx.utils.Disposable;

import phantom.PhantomGame;

/**
 * Base of every screen on the {@link ScreenManager} stack.
 * <p>OOP concepts: <b>abstract class</b> (update/render are abstract) and the <b>State pattern</b>:
 * the game's overall mode is whichever screen is on top of the stack.</p>
 */
public abstract class BaseScreen implements Disposable {
    protected final PhantomGame game;

    protected BaseScreen(PhantomGame game) {
        this.game = game;
    }

    /** Fixed-step logic; only the top screen (and screens it lets through) is updated. */
    public abstract void update(float dt);

    /** Draws the screen; {@code alpha} interpolates between simulation ticks. */
    public abstract void render(float alpha, float frameDelta);

    /** Overlays (pause, map, dialogue) are drawn on top of the screen below them. */
    public boolean isOverlay() {
        return false;
    }

    /** True if the screen underneath should keep simulating (e.g. behind the death fade). */
    public boolean updatesBelow() {
        return false;
    }

    public void onEnter() { }

    public void onExit() { }

    public void resize(int width, int height) { }

    @Override
    public void dispose() { }
}
