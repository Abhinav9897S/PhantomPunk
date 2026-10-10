package phantom.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import phantom.save.SaveData;
import phantom.save.Saveable;

/**
 * The player's currency (Shards), Ember Relics and purchased upgrades.
 * <p>OOP concept: <b>composition</b> (owned by the player) and implements {@link Saveable}.</p>
 */
public final class Inventory implements Saveable {
    public static final int TOTAL_RELICS = 3;

    private int shards;
    private final Set<String> relics = new LinkedHashSet<>();
    private final Set<Upgrade> upgrades = EnumSet.noneOf(Upgrade.class);

    public void addShards(int amount) {
        shards += Math.max(0, amount);
    }

    /** Spends shards if the player can afford it. */
    public boolean spendShards(int amount) {
        if (amount < 0 || amount > shards) return false;
        shards -= amount;
        return true;
    }

    /** Removes all shards (on death) and returns how many were lost. */
    public int dropAllShards() {
        int lost = shards;
        shards = 0;
        return lost;
    }

    public boolean addRelic(String id) { return relics.add(id); }
    public boolean hasAllRelics() { return relics.size() >= TOTAL_RELICS; }
    public int getRelicCount() { return relics.size(); }
    public Set<String> getRelics() { return Collections.unmodifiableSet(relics); }

    public boolean addUpgrade(Upgrade upgrade) { return upgrades.add(upgrade); }
    public boolean hasUpgrade(Upgrade upgrade) { return upgrades.contains(upgrade); }
    public int getShards() { return shards; }

    @Override
    public void writeTo(SaveData data) {
        data.setShards(shards);
        data.setRelics(new ArrayList<>(relics));
        List<String> names = new ArrayList<>();
        for (Upgrade u : upgrades) names.add(u.name());
        data.setUpgrades(names);
    }

    @Override
    public void readFrom(SaveData data) {
        shards = data.getShards();
        relics.clear();
        relics.addAll(data.getRelics());
        upgrades.clear();
        for (String name : data.getUpgrades()) {
            try {
                upgrades.add(Upgrade.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Unknown upgrade from a newer or edited save: skip it.
            }
        }
    }
}
