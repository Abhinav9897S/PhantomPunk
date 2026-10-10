package phantom.art;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import phantom.world.Region;

/**
 * Draws a region's four background layers, each scrolling at its own speed, with
 * fog gradients between them to create depth.
 * <p>OOP concept: encapsulates parallax maths; the textures come from the {@link SpriteFactory}.</p>
 */
public final class ParallaxBackground {
    /** Horizontal scroll factor per layer (0 = fixed to the camera, 1 = moves with the world). */
    private static final float[] FACTORS = {0.04f, 0.18f, 0.42f, 1.35f};
    private static final float[] VERTICAL = {0.02f, 0.06f, 0.14f, 0.5f};
    private static final float[] FOG = {0.0f, 0.35f, 0.28f, 0f};

    private final SpriteFactory art;
    private final TextureRegion scratch = new TextureRegion();

    public ParallaxBackground(SpriteFactory art) {
        this.art = art;
    }

    /** Draws layers 0-2 behind the world. Batch must be using the world camera. */
    public void drawBack(SpriteBatch batch, Region region, Palette palette, float camX, float camY, float viewW, float viewH) {
        Color fog = palette.fogRef();
        for (int layer = 0; layer < 3; layer++) {
            drawLayer(batch, region, layer, camX, camY, viewW, viewH, 1f);
            if (FOG[layer] > 0) drawFog(batch, fog, FOG[layer], camX, camY, viewW, viewH);
        }
    }

    /** Draws the near foreground silhouettes in front of the world. */
    public void drawFront(SpriteBatch batch, Region region, float camX, float camY, float viewW, float viewH) {
        drawLayer(batch, region, 3, camX, camY, viewW, viewH, 0.95f);
    }

    private void drawLayer(SpriteBatch batch, Region region, int layer, float camX, float camY,
                           float viewW, float viewH, float alpha) {
        Texture tex = art.texture(BackgroundPainter.name(region, layer));
        if (tex == null) return;
        float h = viewH * (layer == 3 ? 1.25f : 1.15f);
        float w = h * BackgroundPainter.LAYER_W / BackgroundPainter.LAYER_H;
        float left = camX - viewW / 2f;
        float bottom = camY - viewH / 2f - (h - viewH) / 2f - camY * VERTICAL[layer] * 0.15f;
        if (layer == 3) bottom = camY - viewH / 2f - (h - viewH) / 2f;
        float scroll = (camX * FACTORS[layer]) / w;
        scratch.setTexture(tex);
        scratch.setRegion(scroll, 0f, scroll + viewW / w, 1f);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(scratch, left, bottom, viewW, h);
        batch.setColor(Color.WHITE);
    }

    private void drawFog(SpriteBatch batch, Color fog, float amount, float camX, float camY, float viewW, float viewH) {
        Texture tex = art.texture("fog");
        if (tex == null) return;
        batch.setColor(fog.r, fog.g, fog.b, amount);
        // fog texture is transparent at the top and dense at the bottom (y-down image)
        batch.draw(tex, camX - viewW / 2f, camY - viewH / 2f, viewW, viewH, 0, 0, tex.getWidth(), tex.getHeight(), false, false);
        batch.setColor(Color.WHITE);
    }
}
