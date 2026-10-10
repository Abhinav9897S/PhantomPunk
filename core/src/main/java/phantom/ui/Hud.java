package phantom.ui;

import java.util.ArrayDeque;
import java.util.Deque;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import phantom.art.SpriteSheet;
import phantom.entities.bosses.Boss;
import phantom.entities.player.Player;
import phantom.util.Constants;
import phantom.util.GameMath;
import phantom.world.GameWorld;

/**
 * Minimal in-game interface: flame masks, the soul orb, the shard counter, boss health,
 * toast notifications and the large area / boss title cards.
 * <p>OOP concept: a view class fed by events (toasts and titles are queued by the GameScreen's
 * subscriptions) and by read-only queries on the world.</p>
 */
public final class Hud {
    private static final Color TEXT = new Color(0.9f, 0.92f, 0.96f, 1f);
    private static final Color DIM = new Color(0.62f, 0.66f, 0.74f, 1f);
    private static final Color BOSS = new Color(0.8f, 0.78f, 0.86f, 1f);

    private final UiKit ui;
    private final TextureRegion orbPart = new TextureRegion();
    private final Deque<String> toasts = new ArrayDeque<>();
    private String toast;
    private float toastTimer;
    private String cardTitle;
    private String cardSubtitle;
    private float cardTimer;
    private float shownShards;
    private float shownSoul;
    private float time;

    public Hud(UiKit ui) {
        this.ui = ui;
    }

    public void toast(String text) {
        if (toasts.size() < 6) toasts.add(text);
    }

    /** Large centred title (region names, boss introductions). */
    public void titleCard(String title, String subtitle) {
        cardTitle = title;
        cardSubtitle = subtitle;
        cardTimer = 3.6f;
    }

    public void update(float dt, GameWorld world) {
        time += dt;
        Player p = world.getPlayer();
        shownShards = GameMath.approach(shownShards, p.getInventory().getShards(), Math.max(30f, Math.abs(p.getInventory().getShards() - shownShards) * 4f) * dt);
        shownSoul = GameMath.damp(shownSoul, p.getSoul().fraction(), 10f, dt);
        if (cardTimer > 0) cardTimer -= dt;
        if (toastTimer > 0) {
            toastTimer -= dt;
        } else if (!toasts.isEmpty()) {
            toast = toasts.poll();
            toastTimer = 3.2f;
        } else {
            toast = null;
        }
    }

    public void render(GameWorld world) {
        Player p = world.getPlayer();
        drawSoulOrb(p);
        drawMasks(p);
        drawShards();
        Boss boss = world.getActiveBoss();
        if (boss != null) drawBossBar(boss);
        if (toast != null) drawToast();
        if (cardTimer > 0) drawCard();
    }

    private void drawSoulOrb(Player p) {
        SpriteBatch b = ui.batch();
        float x = 34;
        float y = Constants.UI_HEIGHT - 34 - 84;
        SpriteSheet fill = ui.art().sheet("soul_fill");
        TextureRegion full = fill.frame(0);
        float f = GameMath.clamp(shownSoul, 0f, 1f);
        if (f > 0.01f) {
            TextureRegion part = orbPart;
            part.setRegion(full);
            int h = Math.round(full.getRegionHeight() * f);
            part.setRegion(full.getRegionX(), full.getRegionY() + full.getRegionHeight() - h, full.getRegionWidth(), h);
            float glow = p.getSoul().has(Constants.SOUL_COST) ? 0.85f + 0.15f * (float) Math.sin(time * 4f) : 0.7f;
            b.setColor(glow, glow, glow, 1f);
            b.draw(part, x + 12, y + 12, 60, 60 * f);
            b.setColor(Color.WHITE);
        }
        b.draw(ui.art().region("soul_orb"), x, y, 84, 84);
    }

    private void drawMasks(Player p) {
        SpriteBatch b = ui.batch();
        float x = 132;
        float y = Constants.UI_HEIGHT - 34 - 40;
        int max = p.getHealth().getMax();
        int cur = p.getHealth().getCurrent();
        for (int i = 0; i < max; i++) {
            boolean full = i < cur;
            float bob = full ? (float) Math.sin(time * 3f + i * 0.7f) * 1.5f : 0f;
            b.draw(ui.art().region(full ? "mask_full" : "mask_empty"), x + i * 34, y + bob, 28, 36);
        }
    }

    private void drawShards() {
        SpriteBatch b = ui.batch();
        float x = 136;
        float y = Constants.UI_HEIGHT - 34 - 78;
        b.draw(ui.art().region("shard_icon"), x, y - 18, 18, 22);
        ui.text(ui.fonts().medium(), String.valueOf(Math.round(shownShards)), x + 28, y + 2, TEXT, 1f);
    }

    private void drawBossBar(Boss boss) {
        float w = 620;
        float x = (Constants.UI_WIDTH - w) / 2f;
        float y = 46;
        ui.centered(ui.fonts().small(), boss.getTitle(), Constants.UI_WIDTH / 2f, y + 34, BOSS, 0.95f);
        ui.rect(x - 2, y - 2, w + 4, 12, 0.02f, 0.02f, 0.03f, 0.85f);
        ui.rect(x, y, w * boss.getHealth().fraction(), 8, 0.78f, 0.72f, 0.88f, 0.95f);
        ui.ornament(Constants.UI_WIDTH / 2f, y - 10, w * 0.6f, 0.6f);
    }

    private void drawToast() {
        float a = Math.min(1f, Math.min(toastTimer, 3.2f - toastTimer) * 3f);
        float w = Math.max(320f, ui.width(ui.fonts().small(), toast) + 60f);
        float cx = Constants.UI_WIDTH / 2f;
        float y = Constants.UI_HEIGHT - 120;
        ui.panel(cx - w / 2f, y - 34, w, 46, a);
        ui.centered(ui.fonts().small(), toast, cx, y - 4, TEXT, a);
    }

    private void drawCard() {
        float t = 3.6f - cardTimer;
        float a = Math.min(1f, Math.min(t * 1.5f, cardTimer * 1.2f));
        float cx = Constants.UI_WIDTH / 2f;
        float y = Constants.UI_HEIGHT * 0.66f;
        ui.centered(ui.fonts().large(), cardTitle, cx, y, TEXT, a);
        ui.ornament(cx, y - 66, 420, a * 0.8f);
        if (cardSubtitle != null) ui.centered(ui.fonts().italic(), cardSubtitle, cx, y - 82, DIM, a);
    }
}
