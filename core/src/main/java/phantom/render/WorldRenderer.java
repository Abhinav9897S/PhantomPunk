package phantom.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import phantom.art.Palette;
import phantom.art.ParallaxBackground;
import phantom.art.SpriteFactory;
import phantom.systems.LightingSystem;
import phantom.systems.Particle;
import phantom.util.Constants;
import phantom.util.GameMath;
import phantom.world.GameWorld;
import phantom.world.Region;

/**
 * Draws a {@link GameWorld} in layers: parallax background and light shafts, tiles, entities
 * and particles, the multiplied light map, additive glows, emissive sprites and finally the
 * dark foreground silhouettes.
 * <p>OOP concept: rendering is fully separated from game logic; owns its camera, viewport and
 * lighting system, all released in {@link #dispose()}.</p>
 */
public final class WorldRenderer implements Disposable {
    private final SpriteBatch batch;
    private final SpriteFactory art;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT, camera);
    private final LightingSystem lighting;
    private final RenderContext ctx;
    private final ParallaxBackground parallax;
    private final TileRenderer tiles;
    private final TextureRegion shaft;
    private float time;

    public WorldRenderer(SpriteBatch batch, SpriteFactory art) {
        this.batch = batch;
        this.art = art;
        this.lighting = new LightingSystem(art.texture("soft"));
        this.ctx = new RenderContext(batch, art, lighting);
        this.parallax = new ParallaxBackground(art);
        this.tiles = new TileRenderer(art);
        Texture shaftTex = art.texture("shaft");
        this.shaft = shaftTex == null ? art.region("pixel") : new TextureRegion(shaftTex);
    }

    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    public void render(GameWorld world, float alpha, float frameDelta) {
        time += frameDelta;
        Region region = world.getRegion();
        Palette palette = art.getPalettes().get(region);
        float camX = world.getCamera().renderX(alpha);
        float camY = world.getCamera().renderY(alpha);
        float vw = Constants.VIEW_WIDTH;
        float vh = Constants.VIEW_HEIGHT;
        camera.position.set(camX, camY, 0);
        camera.update();
        viewport.apply();
        Color sky = palette.skyBottomRef();
        Gdx.gl.glClearColor(sky.r * 0.4f, sky.g * 0.4f, sky.b * 0.4f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        ctx.beginFrame(time, region, palette);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        parallax.drawBack(batch, region, palette, camX, camY, vw, vh);
        drawShafts(world, palette, camX, camY);
        tiles.render(batch, world.getTileMap(), region, camX, camY, vw, vh, time);
        world.getEntities().render(ctx, alpha);
        drawParticles(world, false);
        batch.end();

        lighting.renderLightMap(batch, camera, viewport, palette.ambientRef());
        batch.setProjectionMatrix(camera.combined);
        lighting.composite(batch, camX, camY, vw, vh);

        batch.begin();
        lighting.drawGlows(batch);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawParticles(world, true);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        ctx.flushEmissive();
        parallax.drawFront(batch, region, camX, camY, vw, vh);
        batch.end();
    }

    /** Diagonal additive light shafts falling from the ceiling, placed deterministically per room. */
    private void drawShafts(GameWorld world, Palette palette, float camX, float camY) {
        int seed = world.getRoomId().hashCode();
        float roomW = world.getTileMap().widthPx();
        float top = world.getTileMap().heightPx();
        Color a = palette.accentRef();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < 3; i++) {
            float x = GameMath.hash(seed, i) * roomW;
            if (Math.abs(x - camX) > Constants.VIEW_WIDTH) continue;
            float pulse = 0.07f + 0.03f * (float) Math.sin(time * 0.6f + i * 2f);
            batch.setColor(a.r * 0.6f + 0.4f, a.g * 0.6f + 0.4f, a.b * 0.6f + 0.4f, pulse);
            float w = 140 + 80 * GameMath.hash(i, seed);
            float h = Math.min(top + 100f, 900f);
            batch.draw(shaft, x - w / 2f, top - h, w / 2f, h, w, h, 1f, 1f, -18f + 10f * GameMath.hash(seed, i * 7));
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawParticles(GameWorld world, boolean additive) {
        Texture soft = art.texture("soft");
        for (Particle p : world.getParticles().getLive()) {
            if (p.isAdditive() != additive) continue;
            float s = p.getSize();
            batch.setColor(p.getR(), p.getG(), p.getB(), p.getAlpha());
            batch.draw(soft, p.getX() - s, p.getY() - s, s * 2f, s * 2f);
        }
        batch.setColor(Color.WHITE);
    }

    public Viewport getViewport() { return viewport; }
    public OrthographicCamera getCamera() { return camera; }

    @Override
    public void dispose() {
        lighting.dispose();
    }
}
