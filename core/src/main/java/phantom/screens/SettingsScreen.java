package phantom.screens;

import com.badlogic.gdx.graphics.Color;

import phantom.PhantomGame;
import phantom.input.GameAction;
import phantom.save.Settings;
import phantom.ui.Menu;
import phantom.ui.MenuButton;
import phantom.util.Constants;

/**
 * Settings overlay: music and effects volume, screen shake and fullscreen. Saved on exit.
 * <p>OOP concept: menu buttons with left/right adjusters (lambdas).</p>
 */
public final class SettingsScreen extends BaseScreen {
    private static final Color TEXT = new Color(0.92f, 0.94f, 0.98f, 1f);
    private static final Color HINT = new Color(0.55f, 0.6f, 0.66f, 1f);
    private final Menu menu = new Menu();
    private float time;

    public SettingsScreen(PhantomGame game) {
        super(game);
        Settings s = game.settings();
        menu.add(new MenuButton(() -> "Music volume   " + pct(s.getMusicVolume()), null, () -> true,
                d -> { s.setMusicVolume(s.getMusicVolume() + d * 0.1f); game.audio().applyVolumes(); }))
            .add(new MenuButton(() -> "Effects volume   " + pct(s.getSfxVolume()), null, () -> true,
                d -> s.setSfxVolume(s.getSfxVolume() + d * 0.1f)))
            .add(new MenuButton(() -> "Screen shake   " + (s.isScreenShake() ? "On" : "Off"),
                () -> s.setScreenShake(!s.isScreenShake()), () -> true, d -> s.setScreenShake(!s.isScreenShake())))
            .add(new MenuButton(() -> "Fullscreen   " + (s.isFullscreen() ? "On" : "Off"),
                () -> toggleFullscreen(s), () -> true, d -> toggleFullscreen(s)))
            .add(new MenuButton("Back", this::close));
    }

    private void toggleFullscreen(Settings s) {
        s.setFullscreen(!s.isFullscreen());
        game.applyDisplaySettings();
    }

    private static String pct(float v) {
        return Math.round(v * 100) + "%";
    }

    private void close() {
        game.saveSettings();
        game.screens().pop();
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (game.input().isJustPressed(GameAction.BACK) || game.input().isJustPressed(GameAction.PAUSE)) {
            close();
            return;
        }
        menu.update(game.input(), game.audio(), dt);
    }

    @Override
    public void render(float alpha, float frameDelta) {
        float a = Math.min(1f, time * 6f);
        game.beginUi();
        game.batch().begin();
        game.ui().rect(0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT, 0f, 0f, 0.01f, 0.75f * a);
        game.ui().centered(game.fonts().large(), "Settings", Constants.UI_WIDTH / 2f, 560, TEXT, a);
        game.ui().ornament(Constants.UI_WIDTH / 2f, 495, 360, a);
        menu.render(game.ui(), game.fonts().medium(), Constants.UI_WIDTH / 2f, 440, 54, a);
        game.ui().centered(game.fonts().small(), "Left / Right to adjust  -  Esc to go back", Constants.UI_WIDTH / 2f, 90, HINT, a);
        game.batch().end();
    }

    @Override
    public boolean isOverlay() { return true; }
}
