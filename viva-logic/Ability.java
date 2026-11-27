/**
 * One player ability.
 *
 * OOP: INTERFACE + STRATEGY PATTERN - Focus, LightBolt, Dash and DoubleJump are
 *      interchangeable objects. The Player just calls ability.use(...) and each
 *      class decides what that means (polymorphism).
 */
public interface Ability {
    AbilityType getType();

    /** Uses the ability. target may be null when there is no enemy. Returns a message. */
    String use(Player player, Character target);
}
