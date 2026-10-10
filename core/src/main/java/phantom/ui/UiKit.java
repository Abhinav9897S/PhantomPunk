package phantom.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

import phantom.art.SpriteFactory;
import phantom.render.Fonts;

/**
 * Small drawing toolkit shared by all screens: panels, lines, centred and wrapped text.
 * Reuses one {@link GlyphLayout} so measuring text does not allocate every frame.
 * <p>OOP concept: composition - screens use a UiKit instead of duplicating drawing code.</p>
 */
public final class UiKit {
    private final SpriteBatch batch;
    private final SpriteFactory art;
    private final Fonts fonts;
    private final GlyphLayout layout = new GlyphLayout();

    public UiKit(SpriteBatch batch, SpriteFactory art, Fonts fonts) {
        this.batch = batch;
        this.art = art;
        this.fonts = fonts;
    }

    public void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        TextureRegion px = art.region("pixel");
        batch.setColor(r, g, b, a);
        batch.draw(px, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** Dark translucent panel with a thin pale frame and corner diamonds. */
    public void panel(float x, float y, float w, float h, float alpha) {
        rect(x, y, w, h, 0.01f, 0.012f, 0.02f, 0.82f * alpha);
        rect(x, y + h - 1.5f, w, 1.5f, 0.8f, 0.82f, 0.88f, 0.5f * alpha);
        rect(x, y, w, 1.5f, 0.8f, 0.82f, 0.88f, 0.5f * alpha);
        diamond(x + w / 2f, y + h, 6, alpha);
        diamond(x + w / 2f, y, 6, alpha);
    }

    public void diamond(float cx, float cy, float size, float alpha) {
        TextureRegion px = art.region("pixel");
        batch.setColor(0.9f, 0.92f, 1f, alpha);
        batch.draw(px, cx - size / 2f, cy - size / 2f, size / 2f, size / 2f, size, size, 1f, 1f, 45f);
        batch.setColor(Color.WHITE);
    }

    /** Thin horizontal ornament line that fades at both ends. */
    public void ornament(float cx, float y, float width, float alpha) {
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            float fade = 1f - Math.abs(t - 0.5f) * 2f;
            rect(cx - width / 2f + t * width - width / steps / 2f, y, width / steps + 1, 1.5f, 0.85f, 0.88f, 0.95f, alpha * fade);
        }
        diamond(cx, y + 0.75f, 7, alpha);
    }

    public void text(BitmapFont font, String s, float x, float y, Color c, float alpha) {
        font.setColor(c.r, c.g, c.b, c.a * alpha);
        font.draw(batch, s, x, y);
        font.setColor(Color.WHITE);
    }

    public void centered(BitmapFont font, String s, float cx, float y, Color c, float alpha) {
        layout.setText(font, s);
        text(font, s, cx - layout.width / 2f, y, c, alpha);
    }

    /** Wrapped text inside a column; returns the height used. */
    public float wrapped(BitmapFont font, String s, float x, float y, float width, Color c, float alpha, boolean center) {
        font.setColor(c.r, c.g, c.b, c.a * alpha);
        layout.setText(font, s, font.getColor(), width, center ? Align.center : Align.left, true);
        font.draw(batch, layout, x, y);
        font.setColor(Color.WHITE);
        return layout.height;
    }

    public float width(BitmapFont font, String s) {
        layout.setText(font, s);
        return layout.width;
    }

    public SpriteBatch batch() { return batch; }
    public SpriteFactory art() { return art; }
    public Fonts fonts() { return fonts; }
}
