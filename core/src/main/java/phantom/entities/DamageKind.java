package phantom.entities;

/**
 * The source of a hit, used for knockback, pogo and soul gain rules.
 * <p>OOP concept: enum.</p>
 */
public enum DamageKind {
    SLASH, DOWN_SLASH, BOLT, CONTACT, PROJECTILE, HAZARD;

    public boolean isPlayerAttack() {
        return this == SLASH || this == DOWN_SLASH || this == BOLT;
    }
}
