package phantom.screens;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import phantom.PhantomGame;
import phantom.entities.Interactable;
import phantom.entities.player.Player;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.input.GameAction;
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
 * <p>OOP concepts: <b>Observer</b> (all reactions are event subscriptions) and composition.</p>
 */
public final class GameScreen extends BaseScreen {
    private static final Color PROMPT = new Color(0.95f, 0.95f, 1f, 1f);

    private final EventBus bus = new EventBus();
    private final GameWorld world;
    private final WorldRenderer renderer;
    private final Hud hud;
    private final AchievementTracker achievements;
    private final EndingDirector endings;
    private final List<Runnable> subscriptions = new ArrayList<>();
    private String pendingEnding;
    private float endingFade;
    private float time;

    public GameScreen(PhantomGame game, SaveData save) {
        super(game);
        world = new GameWorld(game.worldData(), bus, game.input(), save);
        renderer = new WorldRenderer(game.batch(), game.art());
        hud = new Hud(game.ui());
        achievements = new AchievementTracker(bus, world);
        endings = new EndingDirector(bus, world);
        game.audio().attach(bus);
        subscribe();
        world.start();
        if (save == null) hud.toast("A / D to move  -  Space to jump  -  J to strike");
    }

    private void subscribe() {
        subscriptions.add(bus.subscribe(Events.DialogueRequest.class, e ->
                game.screens().push(new DialogueScreen(game, e.speaker(), e.lines(), e.choices(), e.onChoice()))));
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
        world.update(dt);
        endings.update(dt);
        hud.update(dt, world);
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
        batch.end();
    }

    private void drawPrompt() {
        Player p = world.getPlayer();
        Interactable target = p.getNearbyInteractable();
        if (target == null || !p.isOnGround() || p.isResting()) return;
        float x = target.getHitbox().centerX();
        float y = target.getHitbox().top() + 34f + (float) Math.sin(time * 3f) * 3f;
        game.ui().centered(game.fonts().small(), "W  " + target.getPrompt(), x, y, PROMPT, 0.9f);
    }

    /** Writes the current progress to disk; failures are shown, never thrown. */
    public void save() {
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

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        game.audio().detach();
        achievements.detach();
        endings.detach();
        for (Runnable r : subscriptions) r.run();
        subscriptions.clear();
        bus.clear();
        renderer.dispose();
    }
}
