/** The states an enemy can be in.   OOP: ENUM used by a simple State machine in Enemy. */
public enum EnemyState {
    PATROL,     // wandering, harmless this turn
    WINDUP,     // telegraphing: it will attack next turn
    ATTACK,     // hits the player
    STUNNED     // just got hit and loses its turn
}
