package phantom.entities;

/**
 * Immutable description of one hit.
 * <p>OOP concept: record used as a parameter object.</p>
 */
public record DamageInfo(int amount, float sourceX, float sourceY, DamageKind kind) {
    /** -1 or +1: the horizontal direction pointing away from the source. */
    public float awayFrom(float targetCenterX) {
        return targetCenterX >= sourceX ? 1f : -1f;
    }
}
