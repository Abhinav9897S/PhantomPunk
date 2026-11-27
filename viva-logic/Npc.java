import java.util.List;

/**
 * A character who tells part of the story.
 * OOP: COLLECTIONS - an immutable List<String> of lines (List.of).
 */
public final class Npc extends GameObject implements Interactable {
    private final List<String> lines;

    public Npc(String name, String... lines) {     // VARARGS: any number of lines
        super(name);
        this.lines = List.of(lines);
    }

    @Override
    public void interact(Player player, Game game) {
        for (String line : lines) game.log(getName() + ": \"" + line + "\"");
    }

    @Override
    public String getPrompt() { return "Talk"; }

    @Override
    public String describe() { return getName() + " - interact to talk"; }
}
