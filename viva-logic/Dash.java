/** Umbral Dash: dodge - the next enemy attack this turn misses.   OOP: Ability strategy. */
public class Dash implements Ability {
    @Override
    public AbilityType getType() {
        return AbilityType.DASH;
    }

    @Override
    public String use(Player player, Character target) {
        player.setDodging(true);
        return "You dash through the shadows - nothing can touch you this turn.";
    }
}
