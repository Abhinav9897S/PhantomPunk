package phantom.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * 2D lighting: entities register lights while they render; the system then draws a light map
 * (dark ambient + additive soft circles) into a frame buffer and multiplies it over the scene
 * using "2x multiply" blending, so lights can also brighten. Glows (eyes, cores) are added on
 * top afterwards so they are never darkened.
 * <p>OOP concept: encapsulates all GPU lighting state; owns and disposes a {@link FrameBuffer}.</p>
 */
public final class LightingSystem implements Disposable {
    private static final int MAX = 384;
    private static final int STRIDE = 7;
    private static final float LIGHT_GAIN = 0.42f;

    private final float[] lights = new float[MAX * STRIDE];
    private final float[] glows = new float[MAX * STRIDE];
    private final Texture soft;
    private int lightCount;
    private int glowCount;
    private FrameBuffer buffer;

    public LightingSystem(Texture soft) {
        this.soft = soft;
    }

    public void addLight(float x, float y, float radius, Color c, float intensity) {
        lightCount = push(lights, lightCount, x, y, radius, c, intensity);
    }

    public void addGlow(float x, float y, float radius, Color c, float intensity) {
        glowCount = push(glows, glowCount, x, y, radius, c, intensity);
    }

    private static int push(float[] arr, int count, float x, float y, float radius, Color c, float intensity) {
        if (count >= MAX || intensity <= 0f) return count;
        int i = count * STRIDE;
        arr[i] = x;
        arr[i + 1] = y;
        arr[i + 2] = radius;
        arr[i + 3] = c.r;
        arr[i + 4] = c.g;
        arr[i + 5] = c.b;
        arr[i + 6] = intensity;
        return count + 1;
    }

    /** Renders the light map. Call outside of batch.begin/end; restores the viewport. */
    public void renderLightMap(SpriteBatch batch, OrthographicCamera camera, Viewport viewport, Color ambient) {
        ensureBuffer(viewport);
        buffer.begin();
        Gdx.gl.glClearColor(ambient.r, ambient.g, ambient.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.begin();
        for (int n = 0; n < lightCount; n++) {
            int i = n * STRIDE;
            float k = lights[i + 6] * LIGHT_GAIN;
            batch.setColor(lights[i + 3] * k, lights[i + 4] * k, lights[i + 5] * k, 1f);
            float r = lights[i + 2];
            batch.draw(soft, lights[i] - r, lights[i + 1] - r, r * 2, r * 2);
        }
        batch.end();
        buffer.end();
        viewport.apply();
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        lightCount = 0;
    }

    /** Multiplies the light map over what has been drawn so far (2x so mid-grey is neutral). */
    public void composite(SpriteBatch batch, float camX, float camY, float viewW, float viewH) {
        if (buffer == null) return;
        batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_SRC_COLOR);
        batch.begin();
        batch.setColor(Color.WHITE);
        Texture t = buffer.getColorBufferTexture();
        batch.draw(t, camX - viewW / 2f, camY - viewH / 2f, viewW, viewH, 0, 0, t.getWidth(), t.getHeight(), false, true);
        batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Draws the additive glows. Call between batch.begin/end. */
    public void drawGlows(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int n = 0; n < glowCount; n++) {
            int i = n * STRIDE;
            batch.setColor(glows[i + 3], glows[i + 4], glows[i + 5], Math.min(1f, glows[i + 6]));
            float r = glows[i + 2];
            batch.draw(soft, glows[i] - r, glows[i + 1] - r, r * 2, r * 2);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        glowCount = 0;
    }

    private void ensureBuffer(Viewport viewport) {
        int w = Math.max(64, viewport.getScreenWidth() / 2);
        int h = Math.max(64, viewport.getScreenHeight() / 2);
        if (buffer != null && buffer.getWidth() == w && buffer.getHeight() == h) return;
        if (buffer != null) buffer.dispose();
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        buffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    public int getLightCount() { return lightCount; }

    @Override
    public void dispose() {
        if (buffer != null) buffer.dispose();
        buffer = null;
    }
}
