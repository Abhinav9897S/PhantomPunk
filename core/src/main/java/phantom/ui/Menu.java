package phantom.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import phantom.audio.AudioManager;
import phantom.audio.SoundId;
import phantom.input.GameAction;
import phantom.input.InputSource;

/**
 * A vertical keyboard/gamepad menu with a glowing selection marker.
 * <p>OOP concept: encapsulates selection state; holds a {@link List} of {@link MenuButton}s.</p>
 */
public final class Menu {
    private static final Color NORMAL = new Color(0.62f, 0.66f, 0.72f, 1f);
    private static final Color SELECTED = new Color(1f, 0.97f, 0.9f, 1f);
    private static final Color DISABLED = new Color(0.3f, 0.32f, 0.36f, 1f);

    private final List<MenuButton> buttons = new ArrayList<>();
    private int selected;
    private float pulse;

    public Menu add(MenuButton button) {
        buttons.add(button);
        return this;
    }

    public void update(InputSource input, AudioManager audio, float dt) {
        pulse += dt;
        if (buttons.isEmpty()) return;
        if (input.isJustPressed(GameAction.DOWN)) move(1, audio);
        if (input.isJustPressed(GameAction.UP)) move(-1, audio);
        MenuButton b = buttons.get(selected);
        if (b.canAdjust()) {
            if (input.isJustPressed(GameAction.LEFT)) { b.adjust(-1); audio.play(SoundId.UI_MOVE); }
            if (input.isJustPressed(GameAction.RIGHT)) { b.adjust(1); audio.play(SoundId.UI_MOVE); }
        }
        if (input.isJustPressed(GameAction.CONFIRM)) {
            if (b.isEnabled()) {
                audio.play(SoundId.UI_SELECT);
                b.press();
            } else {
                audio.play(SoundId.UI_DENY);
            }
        }
    }

    private void move(int dir, AudioManager audio) {
        int start = selected;
        do {
            selected = Math.floorMod(selected + dir, buttons.size());
        } while (!buttons.get(selected).isEnabled() && selected != start);
        audio.play(SoundId.UI_MOVE);
    }

    /** Selects the first enabled entry (e.g. "Continue" only if a save exists). */
    public void selectFirstEnabled() {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).isEnabled()) {
                selected = i;
                return;
            }
        }
    }

    public void render(UiKit ui, BitmapFont font, float cx, float topY, float spacing, float alpha) {
        for (int i = 0; i < buttons.size(); i++) {
            MenuButton b = buttons.get(i);
            float y = topY - i * spacing;
            boolean sel = i == selected;
            Color c = !b.isEnabled() ? DISABLED : (sel ? SELECTED : NORMAL);
            String label = b.getLabel();
            ui.centered(font, label, cx, y, c, alpha);
            if (sel) {
                float w = ui.width(font, label) / 2f + 26f + (float) Math.sin(pulse * 4f) * 3f;
                float my = y - font.getCapHeight() / 2f;
                ui.diamond(cx - w, my, 8, alpha);
                ui.diamond(cx + w, my, 8, alpha);
            }
        }
    }

    public int getSelected() { return selected; }
    public int size() { return buttons.size(); }
}
