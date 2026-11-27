import java.util.ArrayList;
import java.util.List;

/**
 * One room of the world and the objects in it.
 *
 * OOP: COLLECTIONS - an ArrayList<GameObject> holds objects of many different classes.
 * OOP: POLYMORPHISM + instanceof - the room finds its enemies and interactables by type.
 * Ability gate: some rooms can only be entered with an ability (DASH or DOUBLE_JUMP).
 */
public class Room {
    private final String name;
    private final AbilityType requiredAbility;   // null = anyone can enter
    private final String gateText;
    private final List<GameObject> objects = new ArrayList<>();
    private int interactIndex;

    public Room(String name, AbilityType requiredAbility, String gateText) {
        this.name = name;
        this.requiredAbility = requiredAbility;
        this.gateText = gateText;
    }

    /** Fluent add: room.add(a).add(b) */
    public Room add(GameObject object) {
        objects.add(object);
        return this;
    }

    public boolean canEnter(Player player) {
        return requiredAbility == null || player.hasAbility(requiredAbility);
    }

    public List<Enemy> getLivingEnemies() {
        List<Enemy> result = new ArrayList<>();
        for (GameObject o : objects) {
            if (o instanceof Enemy enemy && enemy.isAlive()) result.add(enemy);   // pattern matching instanceof
        }
        return result;
    }

    public List<Interactable> getInteractables() {
        List<Interactable> result = new ArrayList<>();
        for (GameObject o : objects) {
            if (o instanceof Interactable i) result.add(i);
        }
        return result;
    }

    /** The next interactable in turn (bench, then NPC, ...). */
    public Interactable nextInteractable() {
        List<Interactable> list = getInteractables();
        if (list.isEmpty()) return null;
        return list.get(interactIndex++ % list.size());
    }

    public void removeObject(GameObject o) {
        objects.remove(o);
    }

    /** Enemies heal back up when the player dies here. */
    public void resetEnemies() {
        for (Enemy e : getLivingEnemies()) e.reset();
    }

    public List<GameObject> getObjects() { return List.copyOf(objects); }
    public String getName() { return name; }
    public AbilityType getRequiredAbility() { return requiredAbility; }
    public String getGateText() { return gateText; }
}
