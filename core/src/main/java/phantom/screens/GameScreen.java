package phantom.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import phantom.PhantomGame;
import phantom.autoplay.AutoPilot;
import phantom.entities.Interactable;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
import phantom.input.ScriptedInput;
import phantom.render.WorldRenderer;
import phantom.save.SaveData;
import phantom.save.SaveException;
import phantom.story.EndingDirector;
import phantom.story.Ending;
import phantom.systems.AchievementTracker;
import phantom.ui.Hud;
import phantom.util.Constants;
import phantom.world.GameWorld;

/**
 * The main gameplay screen. Wires a {@link GameWorld} to its renderer, HUD, audio,
 * achievements and endings through the event bus, and opens overlays (pause, map, dialogue, death).
 * In <b>autoplay</b> mode an {@link AutoPilot} presses the buttons instead of the keyboard, dialogues
 * read themselves, F changes the speed, and nothing is saved.
 * <p>OOP concepts: <b>Observer</b> (all reactions are event subscriptions) and composition.</p>
 */
public final class GameScreen extends BaseScreen {
    private static final Color PROMPT = new Color(0.95f, 0.95f, 1f, 1f);
    private static final Color AUTO = new Color(1f, 0.86f, 0.6f, 1f);
    private static final Color AUTO_DIM = new Color(0.72f, 0.74f, 0.8f, 1f);
    private static final int[] SPEEDS = {1, 2, 4};

    private final EventBus bus = new EventBus();
    private final GameWorld world;
    private final WorldRenderer renderer;
    private final Hud hud;
    private final AchievementTracker achievements;
    private final EndingDirector endings;
    private final List<Runnable> subscriptions = new ArrayList<>();
    /** Non-null in autoplay mode: the AI and the virtual controller it presses. */
    private final AutoPilot pilot;
    private final ScriptedInput pilotInput;
    private int speedIndex;
    private String pendingEnding;
    private float endingFade;
    private float time;

    public GameScreen(PhantomGame game, SaveData save) {
        this(game, save, false);
    }

    /** A fresh game played from start to finish by the autoplay AI. The player's own save is left alone. */
    public static GameScreen autoplay(PhantomGame game) {
        return new GameScreen(game, null, true);
    }

    private GameScreen(PhantomGame game, SaveData save, boolean autoplay) {
        super(game);
        pilot = autoplay ? new AutoPilot(game.worldData()) : null;
        pilotInput = autoplay ? new ScriptedInput() : null;
        world = new GameWorld(game.worldData(), bus, autoplay ? pilotInput : game.input(), save);
        renderer = new WorldRenderer(game.batch(), game.art());
        hud = new Hud(game.ui());
        achievements = new AchievementTracker(bus, world);
        endings = new EndingDirector(bus, world);
        game.audio().attach(bus);
        subscribe();
        world.start();
        if (autoplay) hud.toast("Autoplay - sit back and watch the descent");
        else if (save == null) hud.toast("A / D to move  -  Space to jump  -  J to strike");
    }

    private void subscribe() {
        subscriptions.add(bus.subscribe(Events.DialogueRequest.class, e -> {
            DialogueScreen dialogue = new DialogueScreen(game, e.speaker(), e.lines(), e.choices(), e.onChoice());
            if (pilot != null) dialogue.autoAnswer(pilot::chooseOption);
            game.screens().push(dialogue);
        }));
        subscriptions.add(bus.subscribe(Events.PlayerDied.class, e -> game.screens().push(new DeathScreen(game, this, e.shardsLost()))));
        subscriptions.add(bus.subscribe(Events.BenchRested.class, e -> {
            save();
            hud.toast("You rest. The bench remembers you.");
        }));
        subscriptions.add(bus.subscribe(Events.RoomEntered.class, e -> {
            if (e.regionChanged()) hud.titleCard(e.region().getDisplayName(), e.region().getTagline());
        }));
        subscriptions.add(bus.subscribe(Events.BossEncounter.class, e -> hud.titleCard(e.bossName(), e.subtitle())));
        subscriptions.add(bus.subscribe(Events.BossDefeated.class, e -> {
            if (!e.finalBoss()) save();
        }));
        subscriptions.add(bus.subscribe(Events.Toast.class, e -> hud.toast(e.text())));
        subscriptions.add(bus.subscribe(Events.AchievementUnlocked.class, e -> hud.toast("Achievement - " + e.title())));
        subscriptions.add(bus.subscribe(Events.EndingReached.class, e -> {
            pendingEnding = e.endingId();
            save();
        }));
    }

    @Override
    public void update(float dt) {
        time += dt;
        world.getCamera().setShakeEnabled(game.settings().isScreenShake());
        if (pendingEnding != null) {
            endingFade += dt;
            if (pilotInput != null) {
                pilotInput.releaseAll();
                pilotInput.poll();
            }
            world.update(dt);
            if (endingFade > 2.4f) {
                game.audio().stopAmbience();
                game.screens().replaceAll(new EndingScreen(game, Ending.valueOf(pendingEnding), world.getProgress(),
                        world.getPlayer().getInventory().getRelicCount()));
            }
            return;
        }
        if (game.input().isJustPressed(GameAction.PAUSE)) {
            game.screens().push(new PauseScreen(game, this));
            return;
        }
        if (game.input().isJustPressed(GameAction.MAP)) {
            game.screens().push(new MapScreen(game, this));
            return;
        }
        if (pilot != null) drivePilot();
        world.update(dt);
        endings.update(dt);
        hud.update(dt, world);
    }

    /** Autoplay: the AI picks this tick's buttons; F (Focus key, unused while watching) cycles the speed. */
    private void drivePilot() {
        if (game.input().isJustPressed(GameAction.FOCUS)) {
            speedIndex = (speedIndex + 1) % SPEEDS.length;
            game.setSimulationSpeed(SPEEDS[speedIndex]);
        }
        pilot.update(world);
        pilotInput.releaseAll();
        for (GameAction a : pilot.buttons()) pilotInput.press(a);
        pilotInput.poll();
    }

    @Override
    public void render(float alpha, float frameDelta) {
        renderer.render(world, alpha, frameDelta);
        SpriteBatch batch = game.batch();
        batch.setProjectionMatrix(renderer.getCamera().combined);
        batch.begin();
        drawPrompt();
        batch.end();

        game.beginUi();
        batch.begin();
        batch.draw(game.art().texture("vignette"), 0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT);
        hud.render(world);
        float flash = world.getFlash();
        if (flash > 0) game.ui().rect(0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT, 1f, 1f, 1f, flash * 0.6f);
        float fade = Math.max(world.getTransitionFade(), pendingEnding == null ? 0f : Math.min(1f, endingFade / 2f));
        if (fade > 0) game.ui().rect(0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT, 0f, 0f, 0f, fade);
        if (pilot != null) drawAutoplayBanner();
        batch.end();
    }

    private void drawAutoplayBanner() {
        float right = Constants.UI_WIDTH - 28f;
        float top = Constants.UI_HEIGHT - 30f;
        String title = "AUTOPLAY" + (game.simulationSpeed() > 1 ? "  x" + game.simulationSpeed() : "");
        String status = pilot.status();
        String hint = "Esc  pause / stop     F  speed";
        float w = Math.max(game.ui().width(game.fonts().small(), status), game.ui().width(game.fonts().small(), hint)) + 36f;
        w = Math.max(w, 260f);
        game.ui().panel(right - w, top - 92f, w, 92f, 0.9f);
        float pulse = 0.75f + 0.25f * (float) Math.sin(time * 3f);
        game.ui().text(game.fonts().small(), title, right - w + 18f, top - 14f, AUTO, pulse);
        game.ui().text(game.fonts().small(), status, right - w + 18f, top - 42f, PROMPT, 0.95f);
        game.ui().text(game.fonts().small(), hint, right - w + 18f, top - 68f, AUTO_DIM, 0.8f);
    }

    private void drawPrompt() {
        Player p = world.getPlayer();
        Interactable target = p.getNearbyInteractable();
        if (target == null || !p.isOnGround() || p.isResting()) return;
        float x = target.getHitbox().centerX();
        float y = target.getHitbox().top() + 34f + (float) Math.sin(time * 3f) * 3f;
        game.ui().centered(game.fonts().small(), "W  " + target.getPrompt(), x, y, PROMPT, 0.9f);
    }

    /** Writes the current progress to disk; failures are shown, never thrown. Autoplay never saves. */
    public void save() {
        if (pilot != null) return;
        try {
            game.saves().save(world.snapshot());
        } catch (SaveException e) {
            hud.toast("Could not save: " + e.getMessage());
        }
    }

    public void saveAndQuit() {
        save();
        game.audio().stopAmbience();
        game.screens().replaceAll(new TitleScreen(game));
    }

    public GameWorld getWorld() { return world; }

    /** True while the autoplay AI is playing. */
    public boolean isAutoplay() { return pilot != null; }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        if (pilot != null) game.setSimulationSpeed(1);
        game.audio().detach();
        achievements.detach();
        endings.detach();
        for (Runnable r : subscriptions) r.run();
        subscriptions.clear();
        bus.clear();
        renderer.dispose();
    }
}
