/** Focus: spend 33 soul to heal one mask.   OOP: implements the Ability interface (Strategy). */
public class Focus implements Ability {
    public static final int COST = 33;

    @Override
    public AbilityType getType() {
        return AbilityType.FOCUS;
    }

    @Override
    public String use(Player player, Character target) {
        if (player.getHealth().isFull()) return "You are already at full health.";
        if (!player.spendSoul(COST)) return "Not enough soul to focus (need " + COST + ").";
        player.getHealth().heal(1);
        return "You focus your Light and heal one mask.";
    }
}
