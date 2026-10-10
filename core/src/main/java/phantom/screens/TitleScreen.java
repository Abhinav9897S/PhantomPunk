package phantom.screens;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

import phantom.PhantomGame;
import phantom.art.Palette;
import phantom.art.ParallaxBackground;
import phantom.save.SaveData;
import phantom.systems.Effects;
import phantom.systems.Particle;
import phantom.systems.ParticleSystem;
import phantom.ui.Menu;
import phantom.ui.MenuButton;
import phantom.ui.UiKit;
import phantom.util.Constants;
import phantom.world.Region;

/**
 * Animated title screen: drifting parallax caves, spores, the game's title and the main menu
 * (Continue only when a save exists, New Game asks for confirmation over an existing save).
 * <p>OOP concept: a concrete {@link BaseScreen}; menu actions are lambdas.</p>
 */
public final class TitleScreen extends BaseScreen {
    private static final Color TITLE = new Color(0.94f, 0.95f, 0.98f, 1f);
    private static final Color SUB = new Color(0.7f, 0.78f, 0.82f, 1f);
    private static final Color WARN = new Color(1f, 0.75f, 0.55f, 1f);
    private static final Color HINT = new Color(0.5f, 0.55f, 0.6f, 1f);

    private final Menu menu = new Menu();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT, camera);
    private final ParallaxBackground parallax;
    private final ParticleSystem particles = new ParticleSystem(new Random(3));
    private final Effects effects = new Effects(particles);
    private float time;
    private String message;

    public TitleScreen(PhantomGame game) {
        super(game);
        parallax = new ParallaxBackground(game.art());
        message = game.worldError();
        boolean worldOk = game.worldData() != null;
        menu.add(new MenuButton(() -> "Continue", this::continueGame, () -> worldOk && game.saves().hasSave(), null))
            .add(new MenuButton(() -> "New Game", this::newGame, () -> worldOk, null))
            .add(new MenuButton("Settings", () -> game.screens().push(new SettingsScreen(game))))
            .add(new MenuButton("Credits", () -> game.screens().push(new CreditsScreen(game))))
            .add(new MenuButton("Exit", () -> Gdx.app.exit()));
        menu.selectFirstEnabled();
    }

    private void continueGame() {
        SaveData data = game.saves().loadOrNull(warning -> message = warning);
        if (data == null) {
            menu.selectFirstEnabled();
            return;
        }
        game.screens().replaceAll(new GameScreen(game, data));
    }

    private void newGame() {
        if (!game.saves().hasSave()) {
            startFresh();
            return;
        }
        game.screens().push(new DialogueScreen(game, "A New Descent",
                List.of("Begin again from the surface? Your current journey will be erased."),
                List.of("Yes, descend anew", "No"), choice -> {
                    if (choice == 0) startFresh();
                }));
    }

    private void startFresh() {
        game.saves().deleteSave();
        game.screens().replaceAll(new GameScreen(game, null));
    }

    @Override
    public void onEnter() {
        game.audio().playAmbience(Region.WHISPERING_ROOTS);
    }

    @Override
    public void update(float dt) {
        time += dt;
        menu.update(game.input(), game.audio(), dt);
        effects.ambient(Region.WHISPERING_ROOTS, camX(), 270, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT, dt);
        particles.update(dt);
    }

    private float camX() {
        return 2000f + time * 22f;
    }

    @Override
    public void render(float alpha, float frameDelta) {
        Palette palette = game.art().getPalettes().get(Region.WHISPERING_ROOTS);
        Gdx.gl.glClearColor(0.01f, 0.02f, 0.025f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        SpriteBatch batch = game.batch();
        viewport.apply();
        camera.position.set(camX(), 270, 0);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        parallax.drawBack(batch, Region.WHISPERING_ROOTS, palette, camX(), 270, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        Texture soft = game.art().texture("soft");
        for (Particle p : particles.getLive()) {
            float s = p.getSize();
            batch.setColor(p.getR(), p.getG(), p.getB(), p.getAlpha());
            batch.draw(soft, p.getX() - s, p.getY() - s, s * 2, s * 2);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        parallax.drawFront(batch, Region.WHISPERING_ROOTS, camX(), 270, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
        batch.end();

        game.beginUi();
        batch.begin();
        drawOverlay(game.ui(), soft);
        batch.end();
    }

    private void drawOverlay(UiKit ui, Texture soft) {
        SpriteBatch batch = ui.batch();
        float fade = Math.min(1f, time / 1.5f);
        batch.draw(game.art().texture("vignette"), 0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT);
        float cx = Constants.UI_WIDTH / 2f;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float pulse = 0.18f + 0.05f * (float) Math.sin(time * 1.3f);
        batch.setColor(0.6f, 0.9f, 0.85f, pulse * fade);
        batch.draw(soft, cx - 420, 430, 840, 260);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ui.centered(ui.fonts().title(), "PHANTOM", cx, 610, TITLE, fade);
        ui.ornament(cx, 500, 520, fade * 0.9f);
        ui.centered(ui.fonts().italic(), "The Umbral Descent", cx, 488, SUB, fade);
        menu.render(ui, ui.fonts().medium(), cx, 400, 48, fade);
        if (message != null) ui.wrapped(ui.fonts().small(), message, cx - 450, 120, 900, WARN, fade, true);
        ui.centered(ui.fonts().small(), "W/S or arrows to choose  -  Enter to confirm", cx, 40, HINT, fade * 0.8f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }
}
