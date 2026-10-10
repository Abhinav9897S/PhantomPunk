package phantom.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;

import phantom.PhantomGame;
import phantom.components.Inventory;
import phantom.input.GameAction;
import phantom.story.Ending;
import phantom.util.Constants;
import phantom.world.GameProgress;

/**
 * Shows the chosen ending line by line, then the journey's statistics.
 * <p>OOP concept: a full screen driven by the {@link Ending} enum's data.</p>
 */
public final class EndingScreen extends BaseScreen {
    private static final Color TEXT = new Color(0.92f, 0.94f, 0.98f, 1f);
    private static final Color DIM = new Color(0.6f, 0.64f, 0.72f, 1f);
    private static final Color GOLD = new Color(1f, 0.85f, 0.6f, 1f);

    private final Ending ending;
    private final String stats;
    private float time;

    public EndingScreen(PhantomGame game, Ending ending, GameProgress progress, int relics) {
        super(game);
        this.ending = ending;
        int seconds = (int) progress.getPlayTime();
        this.stats = String.format("Time %d:%02d:%02d   -   Deaths %d   -   Foes vanquished %d   -   Ember Relics %d / %d",
                seconds / 3600, (seconds / 60) % 60, seconds % 60, progress.getDeaths(), progress.getEnemiesKilled(),
                relics, Inventory.TOTAL_RELICS);
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (time > 3f && game.input().isJustPressed(GameAction.CONFIRM)) {
            game.screens().replaceAll(new TitleScreen(game));
        }
    }

    @Override
    public void render(float alpha, float frameDelta) {
        Color bg = ending == Ending.DAWN ? new Color(0.09f, 0.07f, 0.06f, 1f) : new Color(0.01f, 0.01f, 0.02f, 1f);
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.beginUi();
        game.batch().begin();
        float cx = Constants.UI_WIDTH / 2f;
        float a = Math.min(1f, time / 2f);
        if (ending == Ending.DAWN) {
            game.batch().setColor(1f, 0.8f, 0.55f, 0.25f * a);
            game.batch().draw(game.art().texture("soft"), cx - 700, -300, 1400, 900);
            game.batch().setColor(Color.WHITE);
        }
        game.ui().centered(game.fonts().large(), ending.getTitle(), cx, 600, ending == Ending.DAWN ? GOLD : TEXT, a);
        game.ui().ornament(cx, 535, 420, a);
        for (int i = 0; i < ending.getText().size(); i++) {
            float la = Math.max(0f, Math.min(1f, (time - 1.5f - i * 1.6f) / 1.2f));
            game.ui().centered(game.fonts().italic(), ending.getText().get(i), cx, 470 - i * 48, TEXT, la);
        }
        float sa = Math.max(0f, Math.min(1f, (time - 2f - ending.getText().size() * 1.6f) / 1.5f));
        game.ui().centered(game.fonts().small(), stats, cx, 180, DIM, sa);
        if (ending.isHidden()) game.ui().centered(game.fonts().small(), "Hidden ending discovered", cx, 140, GOLD, sa);
        game.ui().centered(game.fonts().small(), "Press Enter to return to the title", cx, 60, DIM, sa * 0.8f);
        game.batch().end();
    }
}
