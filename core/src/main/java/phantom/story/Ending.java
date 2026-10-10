package phantom.story;

import java.util.List;

/**
 * The three endings: two normal ones chosen at the end, and a hidden one that needs all relics.
 * <p>OOP concept: enum with rich data (title and text).</p>
 */
public enum Ending {
    SEALED("The Sealed Dark",
            List.of("Phantom opened its lantern and drank the Mist.",
                    "The Heart fell silent. The Descent grew still and grey.",
                    "Far above, Veyra will never know who keeps its dark.",
                    "A new guardian sleeps at the bottom of the world.")),
    UNBOUND("The Unbound Mist",
            List.of("Phantom shattered the Heart.",
                    "The Mist, freed at last, rose through every root and pipe,",
                    "spilling over the dead city like a black dawn.",
                    "Nothing is hungry anymore. Nothing is anything."),
            false),
    DAWN("Dawn over Veyra",
            List.of("Phantom set the three Ember Relics into the broken Heart.",
                    "They caught. They burned. The Mist turned to light and smoke.",
                    "Ossian's lamps flickered on, one by one, up the long shaft.",
                    "For the first time in an age, morning reached the Descent."),
            true);

    private final String title;
    private final List<String> text;
    private final boolean hidden;

    Ending(String title, List<String> text) {
        this(title, text, false);
    }

    Ending(String title, List<String> text, boolean hidden) {
        this.title = title;
        this.text = List.copyOf(text);
        this.hidden = hidden;
    }

    public String getTitle() { return title; }
    public List<String> getText() { return text; }
    public boolean isHidden() { return hidden; }

    /** Builds the list of choices offered after the final boss. */
    public static List<Ending> choicesFor(boolean hasAllRelics) {
        return hasAllRelics ? List.of(SEALED, UNBOUND, DAWN) : List.of(SEALED, UNBOUND);
    }
}
