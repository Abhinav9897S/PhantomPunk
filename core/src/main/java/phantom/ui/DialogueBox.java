package phantom.ui;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.util.Constants;

/**
 * Draws the dialogue panel: speaker name, the current line revealed letter by letter,
 * and the list of choices when the conversation offers any.
 * <p>OOP concept: a stateless view; {@code DialogueScreen} owns the conversation state.</p>
 */
public final class DialogueBox {
    private static final Color NAME = new Color(1f, 0.88f, 0.65f, 1f);
    private static final Color TEXT = new Color(0.9f, 0.92f, 0.96f, 1f);
    private static final Color CHOICE = new Color(0.62f, 0.66f, 0.72f, 1f);
    private static final Color SELECTED = new Color(1f, 0.97f, 0.9f, 1f);

    public void render(UiKit ui, String speaker, String line, int visibleChars, List<String> choices,
                       int selected, boolean showChoices, float alpha, float time) {
        float w = 900;
        float h = showChoices ? 150 + choices.size() * 36 : 170;
        float x = (Constants.UI_WIDTH - w) / 2f;
        float y = 40;
        ui.panel(x, y, w, h, alpha);
        ui.text(ui.fonts().medium(), speaker, x + 34, y + h - 22, NAME, alpha);
        String shown = line.substring(0, Math.min(line.length(), visibleChars));
        ui.wrapped(ui.fonts().small(), shown, x + 34, y + h - 66, w - 68, TEXT, alpha, false);
        if (showChoices) {
            for (int i = 0; i < choices.size(); i++) {
                boolean sel = i == selected;
                float cy = y + 30 + (choices.size() - 1 - i) * 36;
                ui.text(ui.fonts().small(), choices.get(i), x + 74, cy + 18, sel ? SELECTED : CHOICE, alpha);
                if (sel) ui.diamond(x + 56 + (float) Math.sin(time * 5f) * 2f, cy + 11, 8, alpha);
            }
        } else if (visibleChars >= line.length()) {
            ui.diamond(x + w - 40, y + 26 + (float) Math.sin(time * 4f) * 3f, 8, alpha);
        }
    }
}
