/**
 * A stone bench: heals fully and becomes the respawn point.
 * OOP: extends GameObject AND implements Interactable (a class + an interface).
 */
public final class Bench extends GameObject implements Interactable {
    public Bench() {
        super("Stone Bench");
    }

    @Override
    public void interact(Player player, Game game) {
        player.getHealth().healFull();
        game.setRespawnRoom(game.getCurrentRoomIndex());
        game.getEvents().publish(new GameEvent.BenchRested(game.getCurrentRoom().getName()));
    }

    @Override
    public String getPrompt() { return "Rest"; }

    @Override
    public String describe() { return getName() + " - interact to rest"; }
}
