package phantom.components;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import phantom.abilities.Ability;
import phantom.abilities.AbilityType;
import phantom.abilities.Dash;
import phantom.abilities.DoubleJump;
import phantom.abilities.Focus;
import phantom.abilities.LightBolt;
import phantom.abilities.WallCling;
import phantom.entities.player.Player;
import phantom.input.InputSource;
import phantom.save.SaveData;
import phantom.save.Saveable;
import phantom.world.WorldContext;

/**
 * Holds every ability object and which ones are unlocked.
 * The player calls this set, and the set forwards to each unlocked {@link Ability} polymorphically.
 * <p>OOP concept: <b>composition</b> + {@link EnumMap} keyed by {@link AbilityType}; {@link Saveable}.</p>
 */
public final class AbilitySet implements Saveable {
    private final Map<AbilityType, Ability> abilities = new EnumMap<>(AbilityType.class);
    private final Map<AbilityType, Boolean> unlocked = new EnumMap<>(AbilityType.class);
    /** Cached list of unlocked abilities so the per-tick loops allocate nothing. */
    private final List<Ability> activeCache = new ArrayList<>();

    public AbilitySet() {
        register(new Dash());
        register(new WallCling());
        register(new DoubleJump());
        register(new LightBolt());
        register(new Focus());
        unlocked.put(AbilityType.FOCUS, true);
        rebuildCache();
    }

    private void register(Ability ability) {
        abilities.put(ability.getType(), ability);
        unlocked.put(ability.getType(), false);
    }

    /** Unlocks an ability; returns true if it was newly gained. */
    public boolean unlock(AbilityType type) {
        Boolean before = unlocked.put(type, true);
        rebuildCache();
        return before == null || !before;
    }

    public boolean has(AbilityType type) {
        return unlocked.getOrDefault(type, false);
    }

    public Ability get(AbilityType type) {
        return abilities.get(type);
    }

    /** Typed access, e.g. {@code get(AbilityType.DASH, Dash.class)}. */
    public <T extends Ability> T get(AbilityType type, Class<T> kind) {
        return kind.cast(abilities.get(type));
    }

    public void update(Player player, InputSource input, WorldContext world, float dt) {
        for (Ability a : active()) a.update(player, input, world, dt);
    }

    /** Offers a mid-air jump press to abilities in priority order (wall jump before double jump). */
    public boolean tryJump(Player player, WorldContext world) {
        for (Ability a : active()) {
            if (a.onJumpPressed(player, world)) return true;
        }
        return false;
    }

    public void refreshAll(Player player) {
        for (Ability a : active()) a.refresh(player);
    }

    public void onDamaged(Player player) {
        for (Ability a : active()) a.onDamaged(player);
    }

    public boolean controlsMovement() {
        for (Ability a : active()) {
            if (a.controlsMovement()) return true;
        }
        return false;
    }

    public float modifyMaxFall(float normal) {
        float result = normal;
        for (Ability a : active()) result = Math.min(result, a.modifyMaxFall(normal));
        return result;
    }

    public void resetAll() {
        for (Ability a : abilities.values()) a.reset();
    }

    /** Unlocked abilities in declaration (priority) order. */
    public List<Ability> active() {
        return java.util.Collections.unmodifiableList(activeCache);
    }

    private void rebuildCache() {
        activeCache.clear();
        for (Map.Entry<AbilityType, Ability> e : abilities.entrySet()) {
            if (has(e.getKey())) activeCache.add(e.getValue());
        }
    }

    @Override
    public void writeTo(SaveData data) {
        List<String> names = new ArrayList<>();
        for (AbilityType t : AbilityType.values()) {
            if (has(t)) names.add(t.name());
        }
        data.setAbilities(names);
    }

    @Override
    public void readFrom(SaveData data) {
        for (AbilityType t : AbilityType.values()) unlocked.put(t, t == AbilityType.FOCUS);
        for (String name : data.getAbilities()) {
            try {
                unlocked.put(AbilityType.valueOf(name), true);
            } catch (IllegalArgumentException ignored) {
                // unknown ability name in an edited save file
            }
        }
        rebuildCache();
    }
}
