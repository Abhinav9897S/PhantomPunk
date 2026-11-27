/**
 * Wisp Leap: a passive ability. Owning it lets the player cross high ledges (see Room).
 * OOP: Ability strategy whose use() only gives a message.
 */
public class DoubleJump implements Ability {
    @Override
    public AbilityType getType() {
        return AbilityType.DOUBLE_JUMP;
    }

    @Override
    public String use(Player player, Character target) {
        return "You leap twice into the air. High ledges are no longer a problem.";
    }
}
