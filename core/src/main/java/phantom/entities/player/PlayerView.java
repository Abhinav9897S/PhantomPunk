package phantom.entities.player;

import com.badlogic.gdx.graphics.Color;

import phantom.abilities.AbilityType;
import phantom.abilities.Focus;
import phantom.art.SpriteSheet;
import phantom.render.RenderContext;
import phantom.util.Direction;

/**
 * Chooses the player's animation clip and draws the sprite, eyes, lantern light and slash arcs.
 * <p>OOP concept: separation of concerns - drawing code is kept out of the gameplay class.</p>
 */
final class PlayerView {
    private static final Color EYE = new Color(1f, 0.92f, 0.7f, 1f);
    private static final Color LANTERN = new Color(1f, 0.82f, 0.55f, 1f);
    private static final Color SOUL = new Color(0.8f, 0.92f, 1f, 1f);

    void chooseClip(Player p) {
        String clip;
        if (p.isDead()) clip = "player_fall";
        else if (p.isResting()) clip = "player_rest";
        else if (p.isFocusing()) clip = "player_focus";
        else if (p.isDashing()) clip = "player_dash";
        else if (p.isClinging()) clip = "player_cling";
        else if (!p.isOnGround()) clip = p.getVelocityY() > 0 ? "player_jump" : "player_fall";
        else if (Math.abs(p.getVelocityX()) > 25f) clip = "player_run";
        else clip = "player_idle";
        p.getAnimator().play(clip);
    }

    void render(Player p, RenderContext ctx, float alpha) {
        float cx = p.renderCenterX(alpha);
        float fy = p.renderY(alpha);
        boolean flip = p.getFacing() == Direction.LEFT;
        float dir = flip ? -1f : 1f;
        if (p.isDead()) {
            float t = Math.min(1f, p.getAnimator().getTime() / 1.2f);
            ctx.sprite(ctx.art().sheet("player_fall"), 0, cx, fy, flip, 1f, 1f - t * 0.6f, 0f, 1f - t, 1f - t);
            return;
        }
        String clip = p.getAnimator().getClip();
        SpriteSheet sheet = ctx.art().sheet(clip);
        int frame = frameFor(clip, p, sheet);
        float blink = p.getHealth().isInvincible() && ((int) (ctx.time() * 18f)) % 2 == 0 ? 0.35f : 1f;
        float lean = clip.equals("player_run") ? -p.getVelocityX() / 330f * 5f : 0f;
        ctx.sprite(sheet, frame, cx, fy, flip, p.getAnimator().getScaleX(), p.getAnimator().getScaleY(),
                lean, p.getFlashAmount(), blink);

        float sy = p.getAnimator().getScaleY();
        float eyeX = cx + dir * 4f;
        float eyeY = fy + (clip.equals("player_rest") ? 30f : 34f) * sy;
        ctx.glow(eyeX - 2.6f, eyeY, 6f, EYE, 0.9f * blink);
        ctx.glow(eyeX + 2.6f, eyeY, 6f, EYE, 0.9f * blink);
        ctx.light(cx, fy + 26f, 340f, LANTERN, 1f);
        ctx.glow(cx - dir * 11f, fy + 13f, 9f, LANTERN, 0.7f);

        if (p.isFocusing()) {
            float f = p.getAbilities().get(AbilityType.FOCUS, Focus.class).getProgress();
            ctx.glow(cx, fy + 22f, 30f + 30f * f, SOUL, 0.35f + 0.5f * f);
            ctx.light(cx, fy + 22f, 120f + 120f * f, SOUL, 0.6f);
        }
        if (p.getAttack().isSwinging()) renderSlash(p, ctx, dir);
    }

    private int frameFor(String clip, Player p, SpriteSheet sheet) {
        switch (clip) {
            case "player_run": return p.getAnimator().frame(sheet.count(), 13f, true);
            case "player_idle": return p.getAnimator().frame(sheet.count(), 4f, true);
            case "player_focus": return p.getAnimator().frame(sheet.count(), 8f, true);
            default: return p.getAnimator().frame(sheet.count(), 10f, true);
        }
    }

    private void renderSlash(Player p, RenderContext ctx, float dir) {
        PlayerAttack attack = p.getAttack();
        float progress = attack.getVisualProgress();
        SpriteSheet slash = ctx.art().sheet("slash");
        int frame = Math.min(slash.count() - 1, (int) (progress * slash.count()));
        float cx = attack.getBox().centerX();
        float cy = attack.getBox().centerY();
        float rotation = 0f;
        boolean flip = dir < 0;
        boolean alt = attack.getSwingCount() % 2 == 0;
        switch (attack.getSwing()) {
            case UP: rotation = flip ? -90f : 90f; break;
            case DOWN: rotation = flip ? 90f : -90f; break;
            default: break;
        }
        float fade = 1f - progress * 0.7f;
        ctx.spriteCentered(slash, frame, cx, cy, flip, 1f, alt ? -1f : 1f, rotation, fade);
        ctx.light(cx, cy, 160f, LANTERN, 0.6f * fade);
    }
}
