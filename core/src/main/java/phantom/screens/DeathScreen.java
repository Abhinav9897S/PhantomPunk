package phantom.screens;

import com.badlogic.gdx.graphics.Color;

import phantom.PhantomGame;
import phantom.util.Constants;

/**
 * Overlay shown when Phantom falls: fades to black, explains the shade, then respawns the
 * player at the last bench and fades back in. The world keeps simulating underneath.
 * <p>OOP concept: overlay with {@link #updatesBelow()} overridden.</p>
 */
public final class DeathScreen extends BaseScreen {
    private static final Color TEXT = new Color(0.9f, 0.9f, 0.95f, 1f);
    private static final Color DIM = new Color(0.65f, 0.6f, 0.8f, 1f);
    private static final float HOLD = 3.0f;
    private static final float FADE_IN = 0.8f;

    private final GameScreen gameScreen;
    private final int shardsLost;
    private float time;
    private boolean respawned;

    public DeathScreen(PhantomGame game, GameScreen gameScreen, int shardsLost) {
        super(game);
        this.gameScreen = gameScreen;
        this.shardsLost = shardsLost;
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (!respawned && time >= HOLD) {
            respawned = true;
            gameScreen.getWorld().respawn();
        }
        if (time >= HOLD + FADE_IN) game.screens().pop();
    }

    @Override
    public boolean updatesBelow() {
        return true;
    }

    @Override
    public void render(float alpha, float frameDelta) {
        float black = time < HOLD ? Math.min(1f, time / 1.2f) : Math.max(0f, 1f - (time - HOLD) / FADE_IN);
        float text = time < HOLD ? Math.min(1f, Math.max(0f, (time - 0.8f) * 2f)) * Math.min(1f, (HOLD - time) * 3f) : 0f;
        game.beginUi();
        game.batch().begin();
        game.ui().rect(0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT, 0f, 0f, 0f, black);
        float cx = Constants.UI_WIDTH / 2f;
        game.ui().centered(game.fonts().large(), "Your light fades...", cx, 410, TEXT, text);
        if (shardsLost > 0) {
            game.ui().centered(game.fonts().italic(), shardsLost + " shards remain with your shade. Find it.", cx, 330, DIM, text);
        }
        game.batch().end();
    }

    @Override
    public boolean isOverlay() { return true; }
}
