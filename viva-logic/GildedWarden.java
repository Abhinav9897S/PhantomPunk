/** Second boss. Grants Wisp Leap (double jump). */
public final class GildedWarden extends Boss {
    public GildedWarden() {
        super("Gilded Warden", 60, 120);
    }

    @Override
    public AbilityType getReward() {
        return AbilityType.DOUBLE_JUMP;
    }

    @Override
    protected String attackName(int phase) {
        return phase == 1 ? "raises its brass lance" : "spins up a storm of gears";
    }
}
