package phantom.entities.props;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;

import phantom.audio.SoundId;
import phantom.components.Inventory;
import phantom.components.Upgrade;
import phantom.entities.player.Player;
import phantom.events.Events;
import phantom.render.RenderContext;
import phantom.world.WorldContext;

/**
 * Quill the Moth Merchant: sells permanent upgrades for Shards.
 * <p>OOP concept: subclass of {@link Npc} overriding {@link #interact} with shop logic;
 * a dialogue with choices and a callback (lambda).</p>
 */
public final class Shopkeeper extends Npc {
    private static final Color LAMP = new Color(1f, 0.75f, 0.45f, 1f);

    public Shopkeeper(float x, float y, String npcId) {
        super(x, y, 52f, 56f, npcId);
    }

    @Override
    public void interact(Player player, WorldContext world) {
        Inventory inv = player.getInventory();
        List<Upgrade> stock = new ArrayList<>();
        List<String> options = new ArrayList<>();
        for (Upgrade u : Upgrade.values()) {
            if (!inv.hasUpgrade(u)) {
                stock.add(u);
                options.add(u.getDisplayName() + "  -  " + u.getPrice() + " shards");
            }
        }
        options.add("Leave");
        List<String> lines = stock.isEmpty()
                ? List.of("Sold out, sold out! You have bought every trinket I own.", "Come back when the world ends. Hm. Hm.")
                : List.of("Ah, a customer! Light and shards, shards and light...",
                          "You carry " + inv.getShards() + " shards. What will it be?");
        world.getEvents().publish(new Events.DialogueRequest(getDisplayName(), lines, options,
                choice -> buy(choice, stock, player, world)));
    }

    private void buy(int choice, List<Upgrade> stock, Player player, WorldContext world) {
        if (choice < 0 || choice >= stock.size()) return;
        Upgrade u = stock.get(choice);
        if (!player.getInventory().spendShards(u.getPrice())) {
            world.getEvents().publish(new Events.Toast("Not enough shards"));
            world.getEvents().publish(new Events.Sfx(SoundId.UI_DENY));
            return;
        }
        player.getInventory().addUpgrade(u);
        player.applyUpgrades();
        player.getHealth().healFull();
        world.getEvents().publish(new Events.Toast("Bought " + u.getDisplayName() + ": " + u.getDescription()));
        world.getEvents().publish(new Events.ItemCollected("upgrade:" + u.name(), u.getDisplayName()));
        world.getEvents().publish(new Events.Sfx(SoundId.ABILITY));
    }

    @Override
    public String getPrompt() { return "Trade"; }

    @Override
    protected void renderExtras(RenderContext ctx, float alpha) {
        float flicker = 0.9f + 0.1f * (float) Math.sin(ctx.time() * 7f);
        ctx.glow(centerX() - getFacing().dx() * 20f, getY() + 36f, 8f * flicker, LAMP, 0.9f);
        ctx.light(centerX(), getY() + 36f, 230f * flicker, LAMP, 0.9f);
        ctx.glow(centerX() + getFacing().dx() * 15f - 3f, getY() + 42f, 5f, LAMP, 0.8f);
        ctx.glow(centerX() + getFacing().dx() * 15f + 3f, getY() + 42f, 5f, LAMP, 0.8f);
    }
}
