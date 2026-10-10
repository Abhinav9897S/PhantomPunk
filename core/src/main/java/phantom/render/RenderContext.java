package phantom.render;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import phantom.art.Palette;
import phantom.art.SpriteFactory;
import phantom.art.SpriteSheet;
import phantom.systems.LightingSystem;
import phantom.world.Region;

/**
 * Everything an object needs to draw itself: the sprite batch, the sprite cache, the lighting
 * system and frame information. Also queues "emissive" sprites that are drawn after lighting.
 * <p>OOP concept: a <b>parameter object / facade</b> passed to {@code render()}; it hides batch
 * maths (pivots, flipping, flash overlays) behind a few intent-revealing methods.</p>
 */
public final class RenderContext {
    private final SpriteBatch batch;
    private final SpriteFactory art;
    private final LightingSystem lighting;
    private final List<float[]> emissiveParams = new ArrayList<>();
    private final List<SpriteSheet> emissiveSheets = new ArrayList<>();
    private int emissiveCount;
    private float time;
    private Region region = Region.WHISPERING_ROOTS;
    private Palette palette;

    public RenderContext(SpriteBatch batch, SpriteFactory art, LightingSystem lighting) {
        this.batch = batch;
        this.art = art;
        this.lighting = lighting;
    }

    public void beginFrame(float frameTime, Region frameRegion, Palette framePalette) {
        time = frameTime;
        region = frameRegion;
        palette = framePalette;
    }

    /** Sprite anchored at its feet (bottom centre). */
    public void sprite(SpriteSheet sheet, int frame, float cx, float footY, boolean flipX) {
        sprite(sheet, frame, cx, footY, flipX, 1f, 1f, 0f, 0f, 1f);
    }

    /** Full version: scale and rotation pivot around the feet; {@code flash} overlays white. */
    public void sprite(SpriteSheet sheet, int frame, float cx, float footY, boolean flipX,
                       float sx, float sy, float rotation, float flash, float alpha) {
        if (alpha <= 0f) return;
        float w = sheet.getWidth();
        float h = sheet.getHeight();
        float foot = sheet.getFootOffset();
        draw(sheet.frame(frame), cx - w / 2f, footY - foot, w / 2f, foot, w, h, flipX ? -sx : sx, sy, rotation, alpha);
        if (flash > 0f) draw(sheet.flash(frame), cx - w / 2f, footY - foot, w / 2f, foot, w, h, flipX ? -sx : sx, sy, rotation, alpha * flash);
    }

    /** Sprite anchored at its centre (projectiles, the Hollow Heart, slash arcs). */
    public void spriteCentered(SpriteSheet sheet, int frame, float cx, float cy, boolean flipX,
                               float sx, float sy, float rotation, float alpha) {
        if (alpha <= 0f) return;
        float w = sheet.getWidth();
        float h = sheet.getHeight();
        draw(sheet.frame(frame), cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, flipX ? -sx : sx, sy, rotation, alpha);
    }

    public void spriteFlashCentered(SpriteSheet sheet, int frame, float cx, float cy, float scale, float alpha) {
        float w = sheet.getWidth();
        float h = sheet.getHeight();
        draw(sheet.flash(frame), cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, scale, scale, 0f, alpha);
    }

    /** Queues a centred sprite to be drawn after lighting, at full brightness. */
    public void emissiveCentered(SpriteSheet sheet, int frame, float cx, float cy, boolean flipX,
                                 float sx, float sy, float rotation, float alpha) {
        if (emissiveCount == emissiveParams.size()) {
            emissiveParams.add(new float[8]);
            emissiveSheets.add(sheet);
        }
        float[] p = emissiveParams.get(emissiveCount);
        p[0] = frame;
        p[1] = cx;
        p[2] = cy;
        p[3] = flipX ? -sx : sx;
        p[4] = sy;
        p[5] = rotation;
        p[6] = alpha;
        emissiveSheets.set(emissiveCount, sheet);
        emissiveCount++;
    }

    /** Draws and clears the emissive queue (called by the world renderer after lighting). */
    public void flushEmissive() {
        for (int i = 0; i < emissiveCount; i++) {
            float[] p = emissiveParams.get(i);
            SpriteSheet s = emissiveSheets.get(i);
            float w = s.getWidth();
            float h = s.getHeight();
            draw(s.frame((int) p[0]), p[1] - w / 2f, p[2] - h / 2f, w / 2f, h / 2f, w, h, p[3], p[4], p[5], p[6]);
        }
        emissiveCount = 0;
    }

    private void draw(TextureRegion region, float x, float y, float ox, float oy, float w, float h,
                      float sx, float sy, float rotation, float alpha) {
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(region, x, y, ox, oy, w, h, sx, sy, rotation);
        batch.setColor(Color.WHITE);
    }

    /** Solid rectangle (uses the 1-pixel white sprite). */
    public void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(art.region("pixel"), x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** A soft additive glow drawn after lighting (eyes, lantern cores). */
    public void glow(float x, float y, float radius, Color c, float intensity) {
        lighting.addGlow(x, y, radius, c, intensity);
    }

    /** A light source that brightens the scene around it. */
    public void light(float x, float y, float radius, Color c, float intensity) {
        lighting.addLight(x, y, radius, c, intensity);
    }

    public SpriteBatch batch() { return batch; }
    public SpriteFactory art() { return art; }
    public float time() { return time; }
    public Region region() { return region; }
    public Palette palette() { return palette; }
}
