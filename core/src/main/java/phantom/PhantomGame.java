package phantom;

import java.nio.file.Path;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

import phantom.art.SpriteFactory;
import phantom.audio.AudioManager;
import phantom.input.KeyboardInput;
import phantom.render.Fonts;
import phantom.save.SaveException;
import phantom.save.SaveManager;
import phantom.save.Settings;
import phantom.screens.ScreenManager;
import phantom.screens.TitleScreen;
import phantom.ui.UiKit;
import phantom.util.Constants;
import phantom.world.GdxRoomSource;
import phantom.world.LevelFormatException;
import phantom.world.RoomLoader;
import phantom.world.WorldData;

/**
 * Application entry point. Owns the shared services (batch, art, fonts, audio, saves, input)
 * and the {@link ScreenManager}, and runs the <b>fixed-timestep game loop</b>: logic advances in
 * exact 1/60 s steps, rendering interpolates between the last two steps.
 * <p>OOP concept: composition root - every long-lived object is created and disposed here.</p>
 */
public final class PhantomGame extends ApplicationAdapter {
    private final Path saveDirectory;
    private SpriteBatch batch;
    private SpriteFactory art;
    private Fonts fonts;
    private AudioManager audio;
    private SaveManager saves;
    private Settings settings;
    private KeyboardInput input;
    private ScreenManager screens;
    private UiKit ui;
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private FitViewport uiViewport;
    private WorldData worldData;
    private String worldError;
    private float accumulator;

    public PhantomGame(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    @Override
    public void create() {
        saves = new SaveManager(saveDirectory);
        settings = saves.loadSettings();
        batch = new SpriteBatch(2000);
        art = new SpriteFactory();
        art.build();
        fonts = new Fonts();
        audio = new AudioManager(settings);
        input = new KeyboardInput();
        ui = new UiKit(batch, art, fonts);
        uiViewport = new FitViewport(Constants.UI_WIDTH, Constants.UI_HEIGHT, uiCamera);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        loadWorld();
        applyDisplaySettings();
        screens = new ScreenManager(input);
        screens.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        screens.push(new TitleScreen(this));
    }

    private void loadWorld() {
        try {
            worldData = new RoomLoader().loadWorld(new GdxRoomSource());
        } catch (LevelFormatException e) {
            worldError = e.getMessage();
            Gdx.app.error("World", worldError);
        }
    }

    @Override
    public void render() {
        float frame = Math.min(Gdx.graphics.getDeltaTime(), Constants.MAX_FRAME_TIME);
        accumulator += frame;
        while (accumulator >= Constants.STEP) {
            input.poll();
            screens.update(Constants.STEP);
            accumulator -= Constants.STEP;
        }
        screens.render(accumulator / Constants.STEP, frame);
    }

    /** Applies the UI viewport and sets the batch to UI coordinates (1280x720 virtual). */
    public void beginUi() {
        uiViewport.apply(true);
        batch.setProjectionMatrix(uiCamera.combined);
    }

    public void applyDisplaySettings() {
        try {
            if (settings.isFullscreen() && !Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            } else if (!settings.isFullscreen() && Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            }
        } catch (Throwable t) {
            Gdx.app.error("Display", "Could not change display mode: " + t.getMessage());
        }
        audio.applyVolumes();
    }

    public void saveSettings() {
        try {
            saves.saveSettings(settings);
        } catch (SaveException e) {
            Gdx.app.error("Settings", e.getMessage());
        }
    }

    @Override
    public void resize(int width, int height) {
        uiViewport.update(width, height, true);
        if (screens != null) screens.resize(width, height);
    }

    @Override
    public void dispose() {
        if (screens != null) screens.dispose();
        if (audio != null) audio.dispose();
        if (fonts != null) fonts.dispose();
        if (art != null) art.dispose();
        if (batch != null) batch.dispose();
    }

    public SpriteBatch batch() { return batch; }
    public SpriteFactory art() { return art; }
    public Fonts fonts() { return fonts; }
    public AudioManager audio() { return audio; }
    public SaveManager saves() { return saves; }
    public Settings settings() { return settings; }
    public KeyboardInput input() { return input; }
    public ScreenManager screens() { return screens; }
    public UiKit ui() { return ui; }
    public FitViewport uiViewport() { return uiViewport; }
    public WorldData worldData() { return worldData; }
    public String worldError() { return worldError; }
}
