package phantom.entities;

import phantom.entities.player.Player;
import phantom.world.WorldContext;

/**
 * Objects the player can use by pressing Up: benches, NPCs, shrines.
 * <p>OOP concept: <b>interface</b> - the player looks for any nearby {@code Interactable}.</p>
 */
public interface Interactable extends Collidable {
    boolean canInteract(Player player);

    void interact(Player player, WorldContext world);

    /** Short verb shown above the object, e.g. "Rest" or "Listen". */
    String getPrompt();
}
