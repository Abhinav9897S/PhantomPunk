package phantom.art;

import com.badlogic.gdx.graphics.Color;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints Phantom (every animation clip) and its shade. Each frame is built from layered
 * silhouettes - trailing scarf, legs, bell-shaped cloak, swept-back hood with a dark face -
 * then rim-lit from the upper left. Animation comes from varying a few pose parameters.
 * <p>OOP concept: implements {@link SpritePainter}; a nested record groups pose parameters.</p>
 */
public final class CharacterPainter implements SpritePainter {
    private static final float W = 64f;
    private static final float H = 64f;
    private static final float CX = 32f;
    private static final float FOOT = 60f;

    private static final Color CLOAK = rgb(0.075f, 0.08f, 0.11f);
    private static final Color CLOAK_DARK = rgb(0.035f, 0.04f, 0.055f);
    private static final Color VOID = rgb(0.006f, 0.006f, 0.012f);
    private static final Color SCARF = rgb(0.62f, 0.66f, 0.72f);
    private static final Color BLADE = rgb(0.58f, 0.62f, 0.68f);
    private static final Color LANTERN = rgb(0.95f, 0.78f, 0.48f);
    private static final Color RIM = rgba(0.55f, 0.66f, 0.8f, 1f);

    /** Pose parameters for one frame. */
    record Pose(float bob, float lean, float sway, float legA, float legB, float liftA, float liftB,
                float crouch, float trail, boolean legs, boolean sitting) {
        static Pose stand(float bob, float sway) {
            return new Pose(bob, 0, sway, -4, 4, 0, 0, 0, 0, true, false);
        }
    }

    @Override
    public void paint(SpriteRegistry reg) {
        reg.sprite("player_idle", H - FOOT, true,
                frame(reg, Pose.stand(0, 0)), frame(reg, Pose.stand(0.6f, 0.8f)),
                frame(reg, Pose.stand(1f, 1.2f)), frame(reg, Pose.stand(0.6f, 0.5f)));
        Canvas[] run = new Canvas[6];
        for (int i = 0; i < run.length; i++) {
            double ph = i / (double) run.length * Math.PI * 2;
            float s = (float) Math.sin(ph);
            float liftA = (float) Math.max(0, Math.cos(ph)) * 3.5f;
            float liftB = (float) Math.max(0, -Math.cos(ph)) * 3.5f;
            run[i] = frame(reg, new Pose(Math.abs(s) * 1.4f, 2.5f, -2.5f, s * 6.5f, -s * 6.5f, liftA, liftB, 0, 2, true, false));
        }
        reg.sprite("player_run", H - FOOT, true, run);
        reg.sprite("player_jump", H - FOOT, true, frame(reg, new Pose(-1, 1, -1, -2, 3, 4, 2, 0, 1, true, false)));
        reg.sprite("player_fall", H - FOOT, true,
                frame(reg, new Pose(0, 0, 2.5f, -3, 4, 0, 1, 0, -1, true, false)),
                frame(reg, new Pose(0, 0, 3.2f, -3, 4, 1, 0, 0, -1.5f, true, false)));
        reg.sprite("player_dash", H - FOOT, true, frame(reg, new Pose(1, 6, -6, -6, 2, 3, 3, 0, 9, true, false)));
        reg.sprite("player_cling", H - FOOT, true, frame(reg, new Pose(0, -1, 1.5f, -7, -4, 4, 1, 0, -2, true, false)));
        reg.sprite("player_focus", H - FOOT, true,
                frame(reg, new Pose(3, 0, 1.5f, -5, 5, 0, 0, 3, 0, true, false)),
                frame(reg, new Pose(3.4f, 0, 2.2f, -5, 5, 0, 0, 3, 0, true, false)));
        reg.sprite("player_rest", H - FOOT, true, frame(reg, new Pose(8, 0, 0, 0, 0, 0, 0, 0, 0, true, true)));
        reg.sprite("shade", H - FOOT, false, shade(reg));
    }

    private Canvas frame(SpriteRegistry reg, Pose p) {
        Canvas c = reg.canvas(W, H);
        drawPhantom(c, p, CLOAK, CLOAK_DARK, true);
        c.rimLight(-0.8f, -1f, RIM, 0.75f, 1.1f);
        return c;
    }

    private Canvas shade(SpriteRegistry reg) {
        Canvas c = reg.canvas(W, H);
        Color body = rgba(0.06f, 0.03f, 0.1f, 0.92f);
        drawPhantom(c, new Pose(0, 0, 2, 0, 0, 0, 0, 0, 2, false, false), body, rgba(0.03f, 0.01f, 0.06f, 0.9f), false);
        for (int i = 0; i < 6; i++) {
            ProceduralArt.tendril(c, CX - 12 + i * 5, 52, CX - 14 + i * 5.5f, 62, (i % 2 == 0 ? 0.3f : -0.3f), 4, 0.5f, body);
        }
        c.rimLight(-0.8f, -1f, rgba(0.7f, 0.55f, 1f, 1f), 0.8f, 1.2f);
        return c;
    }

    /** Draws the full figure facing right. */
    private void drawPhantom(Canvas c, Pose p, Color cloak, Color dark, boolean withBlade) {
        float top = p.bob() + p.crouch();
        float lean = p.lean();
        if (p.sitting()) {
            drawSitting(c, cloak, dark, top);
            return;
        }
        // scarf trails behind (to the left), lengthened by speed
        float trail = p.trail();
        ProceduralArt.tendril(c, CX - 1 + lean * 0.5f, 31 + top, CX - 15 - trail * 1.6f, 36 + top - trail * 0.4f, 0.25f, 4.5f, 2.2f, SCARF);
        ProceduralArt.tendril(c, CX - 14 - trail * 1.6f, 36 + top - trail * 0.4f, CX - 22 - trail * 2.2f, 41 + top + p.sway(), -0.3f, 2.2f, 0.6f, SCARF);
        if (p.legs()) {
            c.stroke(CX - 3, 50, CX - 3 + p.legA(), FOOT - p.liftA(), 3.6f, 3f, dark);
            c.stroke(CX + 3, 50, CX + 3 + p.legB(), FOOT - p.liftB(), 3.6f, 3f, dark);
            c.fillEllipse(CX - 2 + p.legA(), FOOT - 0.8f - p.liftA(), 3.2f, 1.6f, dark);
            c.fillEllipse(CX + 4 + p.legB(), FOOT - 0.8f - p.liftB(), 3.2f, 1.6f, dark);
        }
        if (withBlade) {
            c.stroke(CX - 7, 41 + top, CX - 17 - trail, 57 + top * 0.3f, 2.2f, 1f, BLADE);
            c.fillEllipse(CX - 11 - trail * 0.4f, 47 + top * 0.6f, 3.2f, 3.8f, dark);
            c.fillEllipse(CX - 11 - trail * 0.4f, 47 + top * 0.6f, 1.8f, 2.4f, LANTERN);
        }
        float sway = p.sway();
        float hem = 53 + top * 0.25f;
        c.fillPolygon(pts(
                CX - 6 + lean * 0.4f, 29 + top,
                CX - 11 + lean * 0.2f, 35 + top,
                CX - 15 - sway - trail, hem - 1,
                CX - 11 - trail * 0.6f, hem + 2.5f,
                CX - 7 - trail * 0.3f, hem,
                CX - 2, hem + 3.5f,
                CX + 3, hem + 0.2f,
                CX + 8, hem + 3f,
                CX + 14 + sway * 0.6f, hem - 1.5f,
                CX + 11 + lean * 0.3f, 35 + top,
                CX + 6 + lean * 0.5f, 29 + top), cloak);
        // folds in the cloak
        c.stroke(CX - 4 + lean * 0.3f, 36 + top, CX - 7 - trail * 0.4f, hem, 1.4f, 0.6f, dark);
        c.stroke(CX + 4 + lean * 0.4f, 37 + top, CX + 6, hem, 1.2f, 0.6f, dark);
        drawHood(c, CX + lean * 0.6f, top, trail, cloak);
    }

    private void drawHood(Canvas c, float hx, float top, float trail, Color cloak) {
        // swept-back tip, then the rounded hood, then the void where the face should be
        ProceduralArt.tendril(c, hx - 1, 17 + top, hx - 16 - trail * 0.8f, 10 + top + trail * 0.2f, -0.18f, 10, 1.2f, cloak);
        c.fillEllipse(hx + 1, 24 + top, 10.5f, 10f, cloak);
        c.fillEllipse(hx + 4, 26 + top, 6.4f, 6.8f, VOID);
        c.fillEllipse(hx + 6.5f, 27 + top, 3.2f, 4.5f, VOID);
    }

    private void drawSitting(Canvas c, Color cloak, Color dark, float top) {
        c.stroke(CX + 2, 56, CX + 11, 58, 3.6f, 3f, dark);
        c.stroke(CX - 2, 56, CX + 7, 59, 3.6f, 3f, dark);
        c.fillPolygon(pts(CX - 7, 32 + top - 4, CX - 13, 40 + top - 4, CX - 15, 58, CX + 10, 58, CX + 12, 40 + top - 4,
                CX + 7, 32 + top - 4), cloak);
        ProceduralArt.tendril(c, CX - 3, 34 + top - 4, CX - 18, 42 + top - 4, 0.25f, 4.5f, 1.2f, SCARF);
        drawHood(c, CX, top - 4, 0, cloak);
    }
}
