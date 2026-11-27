/** Final boss. No reward - defeating it ends the game. */
public final class HollowHeart extends Boss {
    public HollowHeart() {
        super("The Hollow Heart", 80, 0);
    }

    @Override
    public AbilityType getReward() {
        return null;
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    protected String attackName(int phase) {
        return phase == 1 ? "gathers a ring of void orbs" : "pulls the Mist into a crushing wave";
    }
}
