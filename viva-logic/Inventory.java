/**
 * The player's shards (money).
 *
 * OOP: ENCAPSULATION - spendShards() refuses to go below zero, so the rule lives in one place.
 */
public class Inventory {
    private int shards;

    public void addShards(int amount) {
        if (amount > 0) shards += amount;
    }

    /** Returns false (and spends nothing) if the player cannot afford it. */
    public boolean spendShards(int amount) {
        if (amount < 0 || amount > shards) return false;
        shards -= amount;
        return true;
    }

    public int getShards() {
        return shards;
    }
}
