package phantom.screens;

import java.util.List;
import java.util.function.IntConsumer;

import phantom.PhantomGame;
import phantom.audio.SoundId;
import phantom.input.GameAction;
import phantom.input.InputSource;
import phantom.ui.DialogueBox;

/**
 * Overlay that plays a conversation line by line with a typewriter effect and, on the last
 * line, lets the player pick a choice. The result goes to a callback (lambda).
 * Conversations whose last choice is "Leave" or "No" can be cancelled with Back.
 * <p>OOP concept: overlay screen; callbacks via {@link IntConsumer}.</p>
 */
public final class DialogueScreen extends BaseScreen {
    private static final float CHARS_PER_SECOND = 48f;
    private final String speaker;
    private final List<String> lines;
    private final List<String> choices;
    private final IntConsumer onChoice;
    private final DialogueBox box = new DialogueBox();
    private int line;
    private float chars;
    private int selected;
    private float time;

    public DialogueScreen(PhantomGame game, String speaker, List<String> lines, List<String> choices, IntConsumer onChoice) {
        super(game);
        this.speaker = speaker;
        this.lines = lines.isEmpty() ? List.of("...") : lines;
        this.choices = choices;
        this.onChoice = onChoice;
    }

    @Override
    public void update(float dt) {
        time += dt;
        InputSource in = game.input();
        String current = lines.get(line);
        chars += CHARS_PER_SECOND * dt;
        boolean last = line == lines.size() - 1;
        boolean choosing = last && !choices.isEmpty() && chars >= current.length();
        if (choosing) {
            if (in.isJustPressed(GameAction.DOWN)) { selected = (selected + 1) % choices.size(); game.audio().play(SoundId.UI_MOVE); }
            if (in.isJustPressed(GameAction.UP)) { selected = Math.floorMod(selected - 1, choices.size()); game.audio().play(SoundId.UI_MOVE); }
        }
        if (in.isJustPressed(GameAction.BACK) && cancellable()) {
            close(choices.isEmpty() ? -1 : choices.size() - 1);
            return;
        }
        if (!advancePressed(in)) return;
        if (chars < current.length()) {
            chars = current.length();
        } else if (!last) {
            line++;
            chars = 0;
            game.audio().play(SoundId.UI_MOVE);
        } else {
            game.audio().play(SoundId.UI_SELECT);
            close(choices.isEmpty() ? -1 : selected);
        }
    }

    private boolean advancePressed(InputSource in) {
        return in.isJustPressed(GameAction.CONFIRM) || in.isJustPressed(GameAction.ATTACK) || in.isJustPressed(GameAction.INTERACT)
                && choices.isEmpty();
    }

    private boolean cancellable() {
        if (choices.isEmpty()) return true;
        String lastChoice = choices.get(choices.size() - 1);
        return lastChoice.equals("Leave") || lastChoice.equals("No");
    }

    private void close(int choice) {
        game.screens().pop();
        if (onChoice != null) onChoice.accept(choice);
    }

    @Override
    public void render(float alpha, float frameDelta) {
        String current = lines.get(line);
        boolean showChoices = line == lines.size() - 1 && !choices.isEmpty() && chars >= current.length();
        game.beginUi();
        game.batch().begin();
        box.render(game.ui(), speaker, current, (int) chars, choices, selected, showChoices, Math.min(1f, time * 6f), time);
        game.batch().end();
    }

    @Override
    public boolean isOverlay() { return true; }
}
