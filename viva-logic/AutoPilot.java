import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The computer plays the game (the "Autoplay" option).
 *
 * OOP: another implementation of InputSource - the Game runs it exactly like a keyboard.
 * The logic is a simple priority list, easy to explain:
 *   1. an enemy is about to attack and we can dash  -> DASH
 *   2. low health, enough soul, no attack coming     -> FOCUS (heal)
 *   3. Light Bolt ready and the enemy is tough        -> BOLT
 *   4. any enemy left                                 -> ATTACK
 *   5. something here we have not used yet            -> INTERACT
 *   6. otherwise                                      -> NEXT_ROOM
 */
public class AutoPilot implements InputSource {
    private final Map<Room, Integer> interactionsDone = new HashMap<>();

    @Override
    public Command nextCommand(Game game) {
        Player p = game.getPlayer();
        Room room = game.getCurrentRoom();
        List<Enemy> enemies = room.getLivingEnemies();
        boolean danger = enemies.stream().anyMatch(Enemy::isAboutToAttack);   // method reference

        if (!enemies.isEmpty()) {
            if (danger && p.hasAbility(AbilityType.DASH)) return Command.DASH;
            if (!danger && p.getHealth().getCurrent() <= 2 && p.getSoul() >= Focus.COST) return Command.FOCUS;
            if (p.hasAbility(AbilityType.LIGHT_BOLT) && p.getSoul() >= LightBolt.COST
                    && enemies.get(0).getHealth().getCurrent() > Player.NAIL_DAMAGE) return Command.BOLT;
            return Command.ATTACK;
        }
        int done = interactionsDone.getOrDefault(room, 0);
        if (done < room.getInteractables().size()) {
            interactionsDone.put(room, done + 1);
            return Command.INTERACT;
        }
        if (!p.getHealth().isFull() && p.getSoul() >= Focus.COST) return Command.FOCUS;
        return Command.NEXT_ROOM;
    }
}
