/**
 * Root of everything that can be placed in a room (player, enemies, benches, NPCs, shrines).
 *
 * OOP: ABSTRACTION - an abstract class. You cannot write "new GameObject()";
 *      every subclass must say what it is by implementing describe().
 * OOP: ENCAPSULATION - the name is private and read-only (getter, no setter).
 */
public abstract class GameObject {
    private final String name;

    protected GameObject(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** One line shown in the room list. Each subclass writes its own (polymorphism). */
    public abstract String describe();
}
