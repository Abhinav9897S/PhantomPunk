package phantom.screens;

import com.badlogic.gdx.graphics.Color;

import phantom.PhantomGame;
import phantom.input.GameAction;
import phantom.ui.Menu;
import phantom.ui.MenuButton;
import phantom.util.Constants;

/**
 * Pause overlay: the game underneath stops updating but is still drawn.
 * <p>OOP concept: overlay screen ({@link #isOverlay()} returns true).</p>
 */
public final class PauseScreen extends BaseScreen {
    private static final Color TEXT = new Color(0.92f, 0.94f, 0.98f, 1f);
    private final Menu menu = new Menu();
    private float time;

    public PauseScreen(PhantomGame game, GameScreen gameScreen) {
        super(game);
        menu.add(new MenuButton("Resume", () -> game.screens().pop()))
            .add(new MenuButton("Map", () -> game.screens().push(new MapScreen(game, gameScreen))))
            .add(new MenuButton("Settings", () -> game.screens().push(new SettingsScreen(game))))
            .add(new MenuButton("Save & Quit to Title", gameScreen::saveAndQuit));
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (game.input().isJustPressed(GameAction.PAUSE) || game.input().isJustPressed(GameAction.BACK)) {
            game.screens().pop();
            return;
        }
        menu.update(game.input(), game.audio(), dt);
    }

    @Override
    public void render(float alpha, float frameDelta) {
        float a = Math.min(1f, time * 6f);
        game.beginUi();
        game.batch().begin();
        game.ui().rect(0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT, 0f, 0f, 0.01f, 0.6f * a);
        game.ui().centered(game.fonts().large(), "Paused", Constants.UI_WIDTH / 2f, 520, TEXT, a);
        game.ui().ornament(Constants.UI_WIDTH / 2f, 455, 360, a);
        menu.render(game.ui(), game.fonts().medium(), Constants.UI_WIDTH / 2f, 400, 50, a);
        game.batch().end();
    }

    @Override
    public boolean isOverlay() { return true; }
}
