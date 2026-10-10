package phantom.world;

/**
 * The three connected regions of the Umbral Descent.
 * <p>OOP concept: enum carrying display data; also used as an {@link java.util.EnumMap} key for palettes.</p>
 */
public enum Region {
    WHISPERING_ROOTS("Whispering Roots", "where the old forest still dreams"),
    GILDED_CISTERN("Gilded Cistern", "the drowned engine of Veyra"),
    UMBRAL_HEART("The Umbral Heart", "the black core of the Mist");

    private final String displayName;
    private final String tagline;

    Region(String displayName, String tagline) {
        this.displayName = displayName;
        this.tagline = tagline;
    }

    public String getDisplayName() { return displayName; }
    public String getTagline() { return tagline; }
}
