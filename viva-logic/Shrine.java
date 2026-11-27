/** A shrine holding a lost ability; it can be taken once. */
public final class Shrine extends GameObject implements Interactable {
    private final AbilityType ability;
    private boolean taken;

    public Shrine(AbilityType ability) {
        super("Shrine of " + ability.getDisplayName());
        this.ability = ability;
    }

    @Override
    public void interact(Player player, Game game) {
        if (taken) {
            game.log("The shrine is quiet now.");
            return;
        }
        taken = true;
        if (player.unlock(ability)) game.getEvents().publish(new GameEvent.AbilityUnlocked(ability));
    }

    @Override
    public String getPrompt() { return "Touch"; }

    @Override
    public String describe() { return getName() + (taken ? " (empty)" : " - interact to take its power"); }
}
