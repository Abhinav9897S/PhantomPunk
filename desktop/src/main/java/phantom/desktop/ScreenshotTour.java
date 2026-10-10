package phantom.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

import phantom.PhantomGame;
import phantom.abilities.AbilityType;
import phantom.save.SaveData;
import phantom.screens.DialogueScreen;
import phantom.screens.EndingScreen;
import phantom.screens.GameScreen;
import phantom.screens.MapScreen;
import phantom.screens.PauseScreen;
import phantom.story.Ending;
import phantom.util.Constants;
import phantom.world.RoomData;

/**
 * Developer tool: drives the real game through every region, a boss fight and the menus,
 * saving a PNG of each moment. Launch with {@code DesktopLauncher --tour <folder>}.
 * <p>OOP concept: <b>decorator</b> - wraps the game's {@link ApplicationListener} and adds behaviour
 * after each frame without changing the game classes.</p>
 */
final class ScreenshotTour implements ApplicationListener {
    private record Step(int waitFrames, Consumer<PhantomGame> action, String shot) { }

    private final PhantomGame game;
    private final String outDir;
    private final List<Step> steps = new ArrayList<>();
    private GameScreen gameScreen;
    private int index;
    private int frames;

    ScreenshotTour(PhantomGame game, String outDir) {
        this.game = game;
        this.outDir = outDir;
        steps.add(new Step(90, g -> { }, "01_title"));
        steps.add(new Step(150, g -> startGame(null), "02_fallen_gate"));
        steps.add(new Step(90, g -> room("r1_hollow", 16), "03_mossy_hollow"));
        steps.add(new Step(90, g -> room("r1_shaft", 0), "04_spore_shaft"));
        steps.add(new Step(150, g -> startGame(allAbilities()), null));
        steps.add(new Step(90, g -> room("r2_aqueduct", 24), "05_brass_aqueduct"));
        steps.add(new Step(90, g -> room("r2_chapel", 0), "06_drowned_chapel"));
        steps.add(new Step(90, g -> room("r3_verge", 0), "07_mist_verge"));
        steps.add(new Step(90, g -> room("r3_lantern", 0), "08_last_lantern"));
        steps.add(new Step(260, g -> boss("r1_boss"), "09_boss_gnarlwarden"));
        steps.add(new Step(260, g -> boss("r2_boss"), "10_boss_gilded_warden"));
        steps.add(new Step(300, g -> boss("r3_heart"), "11_boss_hollow_heart"));
        steps.add(new Step(20, g -> g.screens().push(new PauseScreen(g, gameScreen)), "12_pause"));
        steps.add(new Step(20, g -> {
            g.screens().pop();
            for (RoomData r : g.worldData().all()) gameScreen.getWorld().getProgress().discover(r.getId());
            g.screens().push(new MapScreen(g, gameScreen));
        }, "13_map"));
        steps.add(new Step(150, g -> {
            g.screens().pop();
            room("r1_gate", 8);
            g.screens().push(new DialogueScreen(g, "Ossian the Lamplighter",
                    List.of("Another lantern, falling from Veyra... I had stopped counting."), List.of(), null));
        }, "14_dialogue"));
        steps.add(new Step(500, g -> g.screens().replaceAll(new EndingScreen(g, Ending.DAWN,
                gameScreen.getWorld().getProgress(), 3)), "15_ending_dawn"));
    }

    private static SaveData allAbilities() {
        SaveData data = new SaveData();
        List<String> names = new ArrayList<>();
        for (AbilityType t : AbilityType.values()) names.add(t.name());
        data.setAbilities(names);
        data.setShards(240);
        return data;
    }

    private void startGame(SaveData save) {
        gameScreen = new GameScreen(game, save);
        game.screens().replaceAll(gameScreen);
    }

    private void room(String id, int col) {
        RoomData data = game.worldData().get(id);
        gameScreen.getWorld().enterRoom(id, data.getDoors().get(0).id());
        if (col > 0) gameScreen.getWorld().getPlayer().enterAt(col * Constants.TILE, gameScreen.getWorld().getPlayer().getY());
    }

    private void boss(String id) {
        room(id, 0);
        var p = gameScreen.getWorld().getPlayer();
        p.enterAt(12 * Constants.TILE, p.getY());
        p.getHealth().makeInvincible(60f);
    }

    @Override
    public void create() {
        game.create();
    }

    @Override
    public void render() {
        game.render();
        if (index >= steps.size()) {
            Gdx.app.exit();
            return;
        }
        Step step = steps.get(index);
        if (frames == 0) step.action().accept(game);
        frames++;
        if (gameScreen != null) gameScreen.getWorld().getPlayer().getHealth().makeInvincible(5f);
        if (frames >= step.waitFrames()) {
            if (step.shot() != null) capture(step.shot());
            index++;
            frames = 0;
        }
    }

    private void capture(String name) {
        int w = Gdx.graphics.getBackBufferWidth();
        int h = Gdx.graphics.getBackBufferHeight();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, w, h);
        // the back buffer's alpha channel is meaningless on screen; make the PNG opaque
        java.nio.ByteBuffer px = pixmap.getPixels();
        for (int i = 3; i < px.limit(); i += 4) px.put(i, (byte) 0xff);
        PixmapIO.writePNG(Gdx.files.absolute(outDir + "/" + name + ".png"), pixmap, 6, true);
        pixmap.dispose();
        Gdx.app.log("Tour", "saved " + name);
    }

    @Override public void resize(int width, int height) { game.resize(width, height); }
    @Override public void pause() { game.pause(); }
    @Override public void resume() { game.resume(); }
    @Override public void dispose() { game.dispose(); }
}
