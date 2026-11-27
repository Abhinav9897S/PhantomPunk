/** First boss. Grants Umbral Dash.   OOP: concrete class implementing Boss's abstract methods. */
public final class Gnarlwarden extends Boss {
    public Gnarlwarden() {
        super("Gnarlwarden", 40, 80);
    }

    @Override
    public AbilityType getReward() {
        return AbilityType.DASH;
    }

    @Override
    protected String attackName(int phase) {
        return phase == 1 ? "lowers its antlers to charge" : "rears up to slam the ground";
    }
}
