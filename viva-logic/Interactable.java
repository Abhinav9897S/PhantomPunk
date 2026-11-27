/**
 * Objects the player can use with the "interact" command: Bench, Npc, Shrine.
 *
 * OOP: INTERFACE - three unrelated classes sign the same contract.
 */
public interface Interactable {
    /** What happens when the player uses this object. */
    void interact(Player player, Game game);

    /** Short verb shown in the room list, e.g. "Rest" or "Talk". */
    String getPrompt();
}
