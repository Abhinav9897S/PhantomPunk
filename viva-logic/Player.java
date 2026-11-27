import java.util.EnumMap;
import java.util.Map;

/**
 * Phantom, the player.
 *
 * OOP: INHERITANCE - Player IS-A Character IS-A GameObject.
 * OOP: COMPOSITION - Player HAS-A HealthComponent (from Character), an Inventory and
 *      a map of Ability objects. Each part does one job.
 * OOP: COLLECTIONS - EnumMap<AbilityType, Ability> holds the unlocked abilities.
 */
public final class Player extends Character {
    public static final int MAX_MASKS = 5;
    public static final int NAIL_DAMAGE = 5;
    public static final int SOUL_PER_HIT = 11;
    public static final int MAX_SOUL = 99;

    private final Inventory inventory = new Inventory();
    private final Map<AbilityType, Ability> abilities = new EnumMap<>(AbilityType.class);
    private int soul;
    private boolean dodging;

    public Player() {
        super("Phantom", MAX_MASKS);
        unlock(AbilityType.FOCUS);          // Focus is known from the start
    }

    /** Basic sword strike. */
    public String attack(Character target) {
        target.takeDamage(NAIL_DAMAGE);
        if (target.givesSoul()) addSoul(SOUL_PER_HIT);
        return "You strike " + target.getName() + " for " + NAIL_DAMAGE + ".";
    }

    /** Creates the right Ability object for a type (a tiny factory method). */
    public boolean unlock(AbilityType type) {
        if (abilities.containsKey(type)) return false;
        Ability ability;
        switch (type) {
            case LIGHT_BOLT: ability = new LightBolt(); break;
            case DASH: ability = new Dash(); break;
            case DOUBLE_JUMP: ability = new DoubleJump(); break;
            default: ability = new Focus(); break;
        }
        abilities.put(type, ability);
        return true;
    }

    /** Uses an ability through the Ability interface - the Player does not care which class it is. */
    public String useAbility(AbilityType type, Character target) {
        Ability ability = abilities.get(type);
        if (ability == null) return "You have not learned " + type.getDisplayName() + " yet.";
        return ability.use(this, target);
    }

    public boolean hasAbility(AbilityType type) {
        return abilities.containsKey(type);
    }

    public void addSoul(int amount) {
        soul = Math.min(MAX_SOUL, soul + amount);
    }

    public boolean spendSoul(int amount) {
        if (soul < amount) return false;
        soul -= amount;
        return true;
    }

    /** Method OVERRIDING: a dodging player ignores the hit, then normal rules apply (super). */
    @Override
    public boolean takeDamage(int amount) {
        if (dodging) return false;
        return super.takeDamage(amount);
    }

    @Override
    public String describe() {
        return getName() + "  masks " + getHealth().getCurrent() + "/" + getHealth().getMax()
                + "  soul " + soul + "  shards " + inventory.getShards() + "  abilities " + abilities.keySet();
    }

    public int getSoul() { return soul; }
    public void setSoul(int soul) { this.soul = Math.max(0, Math.min(MAX_SOUL, soul)); }
    public Inventory getInventory() { return inventory; }
    public boolean isDodging() { return dodging; }
    public void setDodging(boolean dodging) { this.dodging = dodging; }
}
