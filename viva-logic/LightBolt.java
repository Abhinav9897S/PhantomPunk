/** Light Bolt: spend 33 soul for a 14-damage attack.   OOP: another Ability strategy. */
public class LightBolt implements Ability {
    public static final int COST = 33;
    public static final int DAMAGE = 14;

    @Override
    public AbilityType getType() {
        return AbilityType.LIGHT_BOLT;
    }

    @Override
    public String use(Player player, Character target) {
        if (target == null) return "There is nothing to aim at.";
        if (!player.spendSoul(COST)) return "Not enough soul for a Light Bolt (need " + COST + ").";
        target.takeDamage(DAMAGE);   // works for ANY Character (polymorphism)
        return "Your Light Bolt hits " + target.getName() + " for " + DAMAGE + "!";
    }
}
