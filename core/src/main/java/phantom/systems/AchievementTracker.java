package phantom.systems;

import java.util.ArrayList;
import java.util.List;

import phantom.abilities.AbilityType;
import phantom.events.EventBus;
import phantom.events.Events;
import phantom.world.GameProgress;
import phantom.world.WorldContext;

/**
 * Awards achievements by listening to gameplay events. The code that kills an enemy or opens
 * a secret has no idea this class exists.
 * <p>OOP concept: a pure <b>Observer</b> - it only reacts to events published on the {@link EventBus}.</p>
 */
public final class AchievementTracker {
    private final EventBus bus;
    private final WorldContext world;
    private final List<Runnable> subscriptions = new ArrayList<>();

    public AchievementTracker(EventBus bus, WorldContext world) {
        this.bus = bus;
        this.world = world;
        subscriptions.add(bus.subscribe(Events.EnemyKilled.class, e -> onKill()));
        subscriptions.add(bus.subscribe(Events.BossDefeated.class, this::onBoss));
        subscriptions.add(bus.subscribe(Events.ItemCollected.class, this::onItem));
        subscriptions.add(bus.subscribe(Events.AbilityUnlocked.class, e -> onAbility()));
        subscriptions.add(bus.subscribe(Events.RoomEntered.class, this::onRoom));
        subscriptions.add(bus.subscribe(Events.ShadeRecovered.class, e -> award("Not Gone, Only Lost")));
    }

    private void onKill() {
        int kills = progress().getEnemiesKilled();
        if (kills >= 10) award("Lantern Bearer");
        if (kills >= 50) award("Bane of the Mist");
    }

    private void onBoss(Events.BossDefeated e) {
        switch (e.bossId()) {
            case "gnarlwarden": award("Rootbreaker"); break;
            case "gilded_warden": award("Engine Stopped"); break;
            case "hollow_heart": award("Heartfall"); break;
            default: break;
        }
    }

    private void onItem(Events.ItemCollected e) {
        if (world.getPlayer().getInventory().hasAllRelics()) award("Keeper of Embers");
    }

    private void onAbility() {
        for (AbilityType t : AbilityType.values()) {
            if (!world.getPlayer().getAbilities().has(t)) return;
        }
        award("Whole Again");
    }

    private void onRoom(Events.RoomEntered e) {
        if (e.firstVisit() && e.roomId().endsWith("_secret")) award("Secret Seeker");
        if (progress().getDiscoveredRooms().size() >= 18) award("Cartographer of the Deep");
    }

    private void award(String title) {
        if (progress().unlockAchievement(title)) {
            bus.publish(new Events.AchievementUnlocked(title));
        }
    }

    private GameProgress progress() {
        return world.getProgress();
    }

    public void detach() {
        for (Runnable r : subscriptions) r.run();
        subscriptions.clear();
    }
}
