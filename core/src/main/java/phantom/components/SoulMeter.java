package phantom.components;

/**
 * Light energy gained by striking enemies and spent on Focus and Light Bolt.
 * <p>OOP concept: encapsulation - the amount can only change through add/spend.</p>
 */
public final class SoulMeter {
    private int amount;
    private int max;

    public SoulMeter(int max) {
        this.max = max;
    }

    public void add(int value) {
        amount = Math.min(max, amount + Math.max(0, value));
    }

    public boolean has(int cost) {
        return amount >= cost;
    }

    /** Spends {@code cost} if available. */
    public boolean spend(int cost) {
        if (!has(cost)) return false;
        amount -= cost;
        return true;
    }

    public void setMax(int max) {
        this.max = max;
        amount = Math.min(amount, max);
    }

    public void fill() { amount = max; }
    public void empty() { amount = 0; }
    public int getAmount() { return amount; }
    public int getMax() { return max; }
    public float fraction() { return amount / (float) max; }
}
