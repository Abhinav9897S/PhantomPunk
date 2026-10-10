package phantom.entities.props;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.story.LoreLibrary;
import phantom.world.WorldContext;

/**
 * An NPC who tells part of the story. Lines come from {@link LoreLibrary} and change with progress.
 * <p>OOP concept: concrete subclass of {@link Npc}; talks through the event bus (observer).</p>
 */
public final class LoreNpc extends Npc {
    private static final Color WARM = new Color(1f, 0.85f, 0.6f, 1f);
    private static final Color COLD = new Color(0.7f, 0.85f, 1f, 1f);

    public LoreNpc(float x, float y, String npcId) {
        super(x, y, 36f, 64f, npcId);
    }

    @Override
    public void interact(Player player, WorldContext world) {
        List<String> lines = LoreLibrary.linesFor(getNpcId(), world.getProgress());
        world.getEvents().publish(new Events.DialogueRequest(getDisplayName(), lines, List.of(), null));
    }

    @Override
    protected void renderExtras(RenderContext ctx, float alpha) {
        Color c = getNpcId().equals("seraphine") ? COLD : WARM;
        float dir = getFacing().dx();
        switch (getNpcId()) {
            case "ossian":
                float lx = centerX() + dir * 22f;
                ctx.glow(lx, getY() + 86f, 9f, c, 0.95f);
                ctx.light(lx, getY() + 86f, 260f, c, 1f);
                break;
            case "vesper":
                ctx.glow(centerX(), getY() + 78f, 7f, c, 0.9f);
                ctx.light(centerX(), getY() + 78f, 230f, c, 0.9f);
                break;
            default:
                ctx.light(centerX(), getY() + 40f, 150f, c, 0.7f);
                break;
        }
        float eyeY = getY() + (getNpcId().equals("seraphine") ? 44f : 52f);
        ctx.glow(centerX() + dir * 4f - 2.5f, eyeY, 4f, c, 0.8f);
        ctx.glow(centerX() + dir * 4f + 2.5f, eyeY, 4f, c, 0.8f);
    }
}
