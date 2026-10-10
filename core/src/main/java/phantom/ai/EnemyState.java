package phantom.ai;

/**
 * States shared by every enemy's state machine.
 * <p>OOP concept: enum used as the key of a {@link java.util.EnumMap} in {@link StateMachine}.</p>
 */
public enum EnemyState {
    IDLE, PATROL, ALERT, ATTACK, RECOVER, STUNNED, DEAD
}
