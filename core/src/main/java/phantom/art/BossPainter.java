package phantom.art;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;

import static phantom.art.ProceduralArt.pts;
import static phantom.art.ProceduralArt.rgb;
import static phantom.art.ProceduralArt.rgba;

/**
 * Paints the three guardians: the Gnarlwarden (root beast), the Gilded Warden (brass knight)
 * and the Hollow Heart (floating core with a cracked mask and tendrils).
 * <p>OOP concept: implements {@link SpritePainter}.</p>
 */
public final class BossPainter implements SpritePainter {
    private static final Color INK = rgb(0.015f, 0.015f, 0.02f);

    @Override
    public void paint(SpriteRegistry reg) {
        reg.sprite("gnarlwarden", 4, true,
                gnarl(reg, 0, 0, 0), gnarl(reg, 1, 0, 0), gnarl(reg, 0, 8, 0),
                gnarl(reg, 2, 14, 8), gnarl(reg, 3, -4, 0), gnarl(reg, 4, -6, 0));
        reg.sprite("warden", 4, true, warden(reg, 0), warden(reg, 1), warden(reg, 2), warden(reg, 3), warden(reg, 4));
        reg.sprite("heart_core", 80, true, heartCore(reg, 0), heartCore(reg, 1), heartCore(reg, 2));
        reg.sprite("heart_tendrils", 128, false, tendrils(reg));
    }

    /** @param pose 0/1 idle, 2 charge, 3 leap, 4 roar; dy lowers the head, dx pushes it forward */
    private Canvas gnarl(SpriteRegistry reg, int pose, float dy, float dx) {
        Canvas c = reg.canvas(192, 160);
        float foot = 156;
        Color bark = rgb(0.075f, 0.07f, 0.06f);
        Color root = rgb(0.12f, 0.11f, 0.085f);
        Color moss = rgb(0.16f, 0.33f, 0.22f);
        boolean tucked = pose == 3;
        float[][] legs = {{48, 120, 40, foot}, {72, 124, 70, foot}, {112, 124, 116, foot}, {136, 118, 146, foot}};
        for (float[] l : legs) {
            float ly = tucked ? l[3] - 14 : l[3];
            c.stroke(l[0], l[1], l[2], ly, 16, 11, INK);
            ProceduralArt.tendril(c, l[2], ly - 2, l[2] - 8, ly, 0.3f, 4, 1, INK);
            ProceduralArt.tendril(c, l[2], ly - 2, l[2] + 9, ly, -0.3f, 4, 1, INK);
        }
        ProceduralArt.blob(c, 90, 96 + dy * 0.4f, 66, 46, 0.07f, 9, bark);
        Random rnd = new Random(31 + pose);
        for (int i = 0; i < 16; i++) {
            float x0 = 34 + rnd.nextFloat() * 110;
            float y0 = 128 + dy * 0.3f;
            float x1 = x0 + (rnd.nextFloat() - 0.5f) * 40;
            float y1 = 56 + rnd.nextFloat() * 30 + dy * 0.4f;
            ProceduralArt.tendril(c, x0, y0, x1, y1, (rnd.nextFloat() - 0.5f) * 0.4f, 5 + rnd.nextFloat() * 4, 1.5f, root);
        }
        for (int i = 0; i < 7; i++) {
            ProceduralArt.blob(c, 44 + i * 15, 58 + Math.abs(i - 3) * 5 + dy * 0.4f, 9, 4, 0.4f, 70 + i, moss);
        }
        float hx = 136 + dx;
        float hy = 86 + dy;
        drawAntlers(c, hx, hy, pose, root);
        c.fillEllipse(hx, hy, 27, 22, bark);
        c.fillEllipse(hx + 6, hy + 4, 15, 11, INK);
        if (pose == 4) c.fillEllipse(hx + 8, hy + 14, 10, 7, rgb(0, 0, 0));
        for (int i = 0; i < 5; i++) {
            float sway = pose == 1 ? 3 : 0;
            ProceduralArt.tendril(c, 50 + i * 16, 132, 48 + i * 16 + sway, 148 - (i % 2) * 6, 0.3f, 3, 0.5f, moss);
        }
        c.grain(0.035f, 12 + pose);
        c.rimLight(-0.7f, -1f, rgba(0.6f, 0.95f, 0.75f, 1f), 0.55f, 1.6f);
        return c;
    }

    private void drawAntlers(Canvas c, float hx, float hy, int pose, Color root) {
        float lift = pose == 4 ? -8 : 0;
        ProceduralArt.tendril(c, hx - 10, hy - 14, hx - 34, hy - 70 + lift, -0.25f, 9, 2, root);
        ProceduralArt.tendril(c, hx - 24, hy - 46 + lift, hx - 52, hy - 60 + lift, 0.2f, 4, 1, root);
        ProceduralArt.tendril(c, hx + 6, hy - 16, hx + 24, hy - 74 + lift, 0.2f, 9, 2, root);
        ProceduralArt.tendril(c, hx + 17, hy - 48 + lift, hx + 44, hy - 58 + lift, -0.25f, 4, 1, root);
        ProceduralArt.tendril(c, hx - 28, hy - 60 + lift, hx - 30, hy - 82 + lift, 0.3f, 3, 0.8f, root);
    }

    /** @param pose 0 idle, 1 windup, 2 lunge, 3 leap, 4 cast */
    private Canvas warden(SpriteRegistry reg, int pose) {
        Canvas c = reg.canvas(160, 176);
        float foot = 172;
        Color brass = rgb(0.42f, 0.3f, 0.12f);
        Color brassDark = rgb(0.2f, 0.13f, 0.05f);
        Color brassLight = rgb(0.85f, 0.65f, 0.32f);
        Color cape = rgb(0.06f, 0.05f, 0.07f);
        float bend = pose == 3 ? 10 : (pose == 1 ? 4 : 0);
        float lean = pose == 2 ? 10 : 0;
        c.fillPolygon(pts(62 + lean * 0.3f, 90 + bend, 54, 168, 64, 160, 70, 170, 78, 162, 86, 171, 98 + lean * 0.3f, 92 + bend), cape);
        c.fillPolygon(pts(68, 136 + bend, 64, foot, 76, foot, 79, 136 + bend), brassDark);
        c.fillPolygon(pts(83, 136 + bend, 86 + lean * 0.5f, foot, 98 + lean * 0.5f, foot, 94, 136 + bend), brassDark);
        c.fillPolygon(pts(60 + lean, 86 + bend, 100 + lean, 86 + bend, 96 + lean * 0.6f, 140 + bend, 64 + lean * 0.6f, 140 + bend), brass);
        c.fillPolygon(pts(66 + lean, 92 + bend, 94 + lean, 92 + bend, 80 + lean, 126 + bend), brassDark);
        c.fillCircle(80 + lean, 110 + bend, 6, brassLight);
        c.fillCircle(80 + lean, 110 + bend, 3.5f, rgb(1f, 0.85f, 0.5f));
        c.fillEllipse(58 + lean, 90 + bend, 9, 7, brass);
        c.fillEllipse(102 + lean, 90 + bend, 9, 7, brass);
        float hx = 80 + lean * 1.2f;
        float hy = 72 + bend;
        c.fillPolygon(pts(hx - 13, hy + 13, hx - 14, hy - 6, hx, hy - 20, hx + 14, hy - 6, hx + 13, hy + 13), brass);
        ProceduralArt.spike(c, hx, hy - 18, 6, 14, 0, brassLight);
        c.fillRect(hx - 9, hy + 1, 18, 3.2f, INK);
        drawSpear(c, pose, lean, bend, brassLight, brassDark);
        if (pose == 4) {
            ProceduralArt.gear(c, 80, 40, 22, 10, 5, rgba(0.85f, 0.65f, 0.32f, 0.6f));
            c.cut(80, 40, 11, 11);
        }
        c.grain(0.05f, 40 + pose);
        c.rimLight(-0.7f, -1f, rgba(1f, 0.85f, 0.55f, 1f), 0.7f, 1.4f);
        return c;
    }

    private void drawSpear(Canvas c, int pose, float lean, float bend, Color light, Color dark) {
        float x0;
        float y0;
        float x1;
        float y1;
        switch (pose) {
            case 1: x0 = 70; y0 = 120; x1 = 30; y1 = 70; break;
            case 2: x0 = 92 + lean; y0 = 112; x1 = 158; y1 = 108; break;
            case 3: x0 = 100; y0 = 100 + bend; x1 = 128; y1 = 20; break;
            case 4: x0 = 104; y0 = 104; x1 = 112; y1 = 18; break;
            default: x0 = 104; y0 = 128; x1 = 138; y1 = 34; break;
        }
        c.stroke(x0, y0, x1, y1, 3.2f, 2.6f, dark);
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ux = dx / len;
        float uy = dy / len;
        c.fillPolygon(pts(x1 - uy * 5, y1 + ux * 5, x1 + ux * 18, y1 + uy * 18, x1 + uy * 5, y1 - ux * 5), light);
    }

    private Canvas heartCore(SpriteRegistry reg, int cracks) {
        Canvas c = reg.canvas(160, 160);
        Color shell = rgb(0.05f, 0.03f, 0.08f);
        Color mask = rgb(0.86f, 0.84f, 0.92f);
        Color maskShade = rgb(0.6f, 0.56f, 0.7f);
        for (int i = 0; i < 9; i++) {
            double a = i / 9.0 * Math.PI * 2 - Math.PI / 2;
            float x = 80 + (float) Math.cos(a) * 46;
            float y = 80 + (float) Math.sin(a) * 46;
            ProceduralArt.spike(c, x, y, 14, 18, (float) Math.toDegrees(a) + 90, shell);
        }
        c.fillCircle(80, 80, 48, shell);
        c.radial(80, 80, 46, rgba(0.45f, 0.3f, 0.7f, 0.6f), 2.5f, true);
        c.fillEllipse(80, 84, 23, 28, maskShade);
        c.fillEllipse(79, 82, 21, 26, mask);
        c.fillEllipse(80, 78, 7, 9, INK);
        c.fillEllipse(70, 98, 3, 2, INK);
        c.fillEllipse(90, 98, 3, 2, INK);
        Random rnd = new Random(5);
        int n = 3 + cracks * 4;
        for (int i = 0; i < n; i++) {
            float x0 = 66 + rnd.nextFloat() * 28;
            float y0 = 60 + rnd.nextFloat() * 44;
            ProceduralArt.tendril(c, x0, y0, x0 + (rnd.nextFloat() - 0.5f) * 20, y0 + (rnd.nextFloat() - 0.5f) * 22, 0.3f, 1.4f, 0.3f, INK);
        }
        if (cracks >= 2) c.cut(94, 70, 8, 11);
        c.rimLight(-0.6f, -1f, rgba(0.85f, 0.75f, 1f, 1f), 0.7f, 1.4f);
        return c;
    }

    private Canvas tendrils(SpriteRegistry reg) {
        Canvas c = reg.canvas(256, 256);
        Color dark = rgba(0.06f, 0.03f, 0.1f, 0.95f);
        for (int i = 0; i < 9; i++) {
            double a = i / 9.0 * Math.PI * 2;
            float x1 = 128 + (float) Math.cos(a) * 118;
            float y1 = 128 + (float) Math.sin(a) * 118;
            ProceduralArt.tendril(c, 128, 128, x1, y1, 0.35f, 15, 1.2f, dark);
            float mx = 128 + (float) Math.cos(a + 0.2) * 80;
            float my = 128 + (float) Math.sin(a + 0.2) * 80;
            ProceduralArt.tendril(c, mx, my, mx + (float) Math.cos(a + 1.2) * 30, my + (float) Math.sin(a + 1.2) * 30, -0.3f, 4, 0.6f, dark);
        }
        c.rimLight(-0.6f, -1f, rgba(0.7f, 0.5f, 1f, 1f), 0.6f, 1.5f);
        return c;
    }
}
