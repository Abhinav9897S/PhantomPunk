/**
 * A living GameObject: has health and can be damaged. Base of Player and Enemy.
 *
 * OOP: INHERITANCE - Character IS-A GameObject (extends).
 * OOP: "multiple inheritance" through an interface - it also IMPLEMENTS Damageable.
 * OOP: ABSTRACT CLASS - still abstract, because describe() is left to subclasses.
 */
public abstract class Character extends GameObject implements Damageable {
    private final HealthComponent health;   // composition (has-a)

    protected Character(String name, int maxHealth) {
        super(name);                          // calls the GameObject constructor
        this.health = new HealthComponent(maxHealth);
    }

    @Override
    public boolean takeDamage(int amount) {
        return health.damage(amount);
    }

    @Override
    public boolean isAlive() {
        return !health.isDead();
    }

    public HealthComponent getHealth() {
        return health;
    }
}
