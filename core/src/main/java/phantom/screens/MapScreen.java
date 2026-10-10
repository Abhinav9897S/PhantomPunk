package phantom.screens;

import phantom.PhantomGame;
import phantom.input.GameAction;
import phantom.ui.MapRenderer;

/**
 * Map overlay showing the rooms discovered so far.
 * <p>OOP concept: overlay screen delegating drawing to {@link MapRenderer}.</p>
 */
public final class MapScreen extends BaseScreen {
    private final GameScreen gameScreen;
    private final MapRenderer map = new MapRenderer();
    private float time;

    public MapScreen(PhantomGame game, GameScreen gameScreen) {
        super(game);
        this.gameScreen = gameScreen;
    }

    @Override
    public void update(float dt) {
        time += dt;
        if (game.input().isJustPressed(GameAction.MAP) || game.input().isJustPressed(GameAction.BACK)
                || game.input().isJustPressed(GameAction.PAUSE)) {
            game.screens().pop();
        }
    }

    @Override
    public void render(float alpha, float frameDelta) {
        game.beginUi();
        game.batch().begin();
        map.render(game.ui(), gameScreen.getWorld(), Math.min(1f, time * 6f), time);
        game.batch().end();
    }

    @Override
    public boolean isOverlay() { return true; }
}
