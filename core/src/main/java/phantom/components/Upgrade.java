package phantom.components;

/**
 * Permanent upgrades sold by the shopkeeper.
 * <p>OOP concept: enum with data (price, description).</p>
 */
public enum Upgrade {
    MASK_SHARD("Hollow Mask", "A cracked mask that holds one more flame of life.", 120),
    KEEN_EDGE("Keen Edge", "Whetted lantern-blade. Strikes deal more damage.", 160),
    EMBER_VESSEL("Ember Vessel", "A glass heart that stores more Light.", 100);

    private final String displayName;
    private final String description;
    private final int price;

    Upgrade(String displayName, String description, int price) {
        this.displayName = displayName;
        this.description = description;
        this.price = price;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
}
