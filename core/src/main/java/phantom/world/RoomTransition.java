package phantom.world;

/**
 * Fade-out, switch room, fade-in when the player walks through a door.
 * <p>OOP concept: a tiny state machine object (encapsulation of the transition timing).</p>
 */
public final class RoomTransition {
    private static final float OUT = 0.22f;
    private static final float IN = 0.3f;

    private DoorData door;
    private float time;
    private boolean switched;

    public boolean isActive() {
        return door != null;
    }

    public void begin(DoorData through) {
        door = through;
        time = 0;
        switched = false;
    }

    /** Advances the fade; performs the room switch at the darkest moment. */
    public void update(float dt, GameWorld world) {
        if (door == null) return;
        time += dt;
        if (!switched && time >= OUT) {
            switched = true;
            world.enterRoom(door.targetRoom(), door.targetDoor());
        }
        if (time >= OUT + IN) door = null;
    }

    /** 0 = clear, 1 = black. */
    public float getFade() {
        if (door == null) return 0f;
        if (time < OUT) return time / OUT;
        return Math.max(0f, 1f - (time - OUT) / IN);
    }
}
