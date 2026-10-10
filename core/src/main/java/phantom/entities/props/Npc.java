package phantom.entities.props;

import phantom.entities.Interactable;
import phantom.entities.player.Player;
import phantom.render.RenderContext;
import phantom.story.LoreLibrary;
import phantom.util.Direction;
import phantom.world.WorldContext;

/**
 * Base class for characters you can talk to. Subclasses decide what talking does.
 * <p>OOP concept: <b>abstract class</b> implementing {@link Interactable}; {@code LoreNpc}
 * and {@code Shopkeeper} override {@link #interact}.</p>
 */
public abstract class Npc extends Prop implements Interactable {
    private final String npcId;

    protected Npc(float x, float y, float w, float h, String npcId) {
        super(x - w / 2f, y, w, h);
        this.npcId = npcId;
    }

    @Override
    public void update(float dt, WorldContext world) {
        face(world.getPlayer().centerX());
    }

    @Override
    public boolean canInteract(Player player) {
        return !player.isDead();
    }

    @Override
    public String getPrompt() { return "Listen"; }

    public String getNpcId() { return npcId; }

    public String getDisplayName() { return LoreLibrary.nameOf(npcId); }

    @Override
    public void render(RenderContext ctx, float alpha) {
        float bob = (float) Math.sin(ctx.time() * 1.6f + getX()) * 0.015f;
        ctx.sprite(ctx.art().sheet("npc_" + npcId), 0, centerX(), getY(), getFacing() == Direction.LEFT,
                1f, 1f + bob, 0f, 0f, 1f);
        renderExtras(ctx, alpha);
    }

    /** Lights, eyes, lanterns. */
    protected abstract void renderExtras(RenderContext ctx, float alpha);
}
