package phantom.autoplay;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import phantom.components.Upgrade;
import phantom.entities.Interactable;
import phantom.entities.Shade;
import phantom.entities.player.Player;
import phantom.entities.props.AbilityShrine;
import phantom.entities.props.Bench;
import phantom.entities.props.BreakableWall;
import phantom.entities.props.LoreNpc;
import phantom.entities.props.ShardCache;
import phantom.entities.props.Shopkeeper;
import phantom.input.GameAction;
import phantom.util.Constants;
import phantom.world.DoorData;
import phantom.world.GameWorld;
import phantom.world.WorldData;

/**
 * The autoplay brain. Once per tick it looks at the world exactly as a player would (positions,
 * health, enemies, the map) and decides which buttons to hold. It follows a {@link Quest} worked out
 * from the room files, travels with the {@link Navigator}, and hands over to the {@link Fighter}
 * whenever enemies or a guardian need dealing with.
 * <p>The pilot never moves Phantom directly: it only presses the same buttons a person would, so every
 * jump, hit and death goes through the normal game rules.</p>
 * <p>OOP concepts: <b>composition</b> (Quest, Navigator, Fighter) and <b>encapsulation</b> - the game
 * only sees {@link #update} and {@link #buttons()}.</p>
 */
public final class AutoPilot {
    private static final int T = Constants.TILE;

    private final Quest quest;
    private final Dangers dangers = new Dangers();
    private final Navigator navigator = new Navigator(dangers);
    private final Fighter fighter;
    private final Set<GameAction> out = EnumSet.noneOf(GameAction.class);
    private final Set<GameAction> scratch = EnumSet.noneOf(GameAction.class);
    private final Set<String> talked = new HashSet<>();
    private final Set<String> rested = new HashSet<>();
    private final Set<String> skipped = new HashSet<>();
    private final Set<String> shopsDone = new HashSet<>();
    private String status = "Waking up";
    private int restTicks;
    private int interactWait;
    private int shards;
    private boolean pressedLastTick;
    /** A cracked wall that blocks the way, being broken open. */
    private BreakableWall wallTarget;

    public AutoPilot(WorldData world) {
        quest = new Quest(world);
        fighter = new Fighter(navigator, dangers);
    }

    /** Buttons to hold during the coming tick. */
    public Set<GameAction> buttons() {
        return out;
    }

    /** What the pilot is currently trying to do, for the HUD. */
    public String status() {
        return status;
    }

    /** Decides this tick's buttons. Call once per fixed step, before the world updates. */
    public void update(GameWorld world) {
        Player p = world.getPlayer();
        shards = p.getInventory().getShards();
        out.clear();
        if (p.isDead()) return;
        navigator.sync(world);
        if (world.getTransitionFade() > 0f) return;
        if (p.isResting()) {
            // sit for a moment, then stand up by walking off
            if (++restTicks < 75) return;
        }
        restTicks = 0;
        if (interactWait > 0) interactWait--;

        if (fighter.inBossFight(world)) {
            status = "Fighting " + fighter.bossName(world);
            fighter.bossFight(world, out);
            navigator.replan();
            return;
        }
        if (fighter.skirmish(world, out)) {
            status = fighter.targetName().isEmpty() ? "Dodging" : "Fighting a " + fighter.targetName();
            navigator.replan();
            return;
        }
        if (fighter.recover(world, out)) {
            status = "Recovering from a hit";
            return;
        }
        if (fighter.heal(world, out)) {
            status = "Focusing to heal";
            return;
        }
        pursueObjective(world);
    }

    // ------------------------------------------------------------------ objectives

    private Quest.Objective current(GameWorld world) {
        for (Quest.Objective o : quest.objectives()) {
            if (!isDone(o, world) && !skipped.contains(o.key())) return o;
        }
        return null;
    }

    private boolean isDone(Quest.Objective o, GameWorld world) {
        switch (o.kind()) {
            case TALK: return talked.contains(o.key());
            case BENCH: return rested.contains(o.key());
            case BOSS: return world.getProgress().isBossDefeated(o.key());
            default: return world.getProgress().isCollected(o.key());
        }
    }

    private void pursueObjective(GameWorld world) {
        Player p = world.getPlayer();
        String room = world.getRoomId();
        // opportunistic side jobs in the current room
        Shade shade = find(world, Shade.class);
        String shadeKey = "shade:" + room + ":" + world.getProgress().getDeaths();
        if (shade != null && !skipped.contains(shadeKey)) {
            status = "Recovering lost shards";
            Navigator.Drive d = goTo(world, cellsAround(shade.centerX(), shade.getY(), 0), "shade");
            if (d == Navigator.Drive.UNREACHABLE) {
                skipped.add(shadeKey); // out of reach: let the shards go
            } else if (d == Navigator.Drive.ARRIVED) {
                out.clear();
                Navigator.walkTowards(p, shade.centerX() - p.centerX(), out);
            }
            return;
        }
        Shopkeeper shop = find(world, Shopkeeper.class);
        String shopVisit = room + ":" + shards;
        if (shop != null && !shopsDone.contains(shopVisit) && bestUpgrade(p) != null) {
            status = "Visiting the merchant";
            interactWith(world, shop, "shop:" + room, () -> shopsDone.add(shopVisit));
            return;
        }

        if (wallTarget != null && !wallTarget.isRemoved() && world.getEntities().view().contains(wallTarget)) {
            status = "Breaking a cracked wall";
            breakWall(world, wallTarget);
            return;
        }
        wallTarget = null;
        Quest.Objective o = current(world);
        if (o == null) {
            status = "Journey complete";
            return;
        }
        if (!o.roomId().equals(room)) {
            DoorData door = quest.nextDoor(room, o.roomId());
            if (door == null) {
                skipped.add(o.key());
                return;
            }
            status = o.label() + " (" + world.getWorldData().get(o.roomId()).getName() + ")";
            List<int[]> cells = new ArrayList<>();
            for (int r = door.row() - 1; r <= door.row() + 2; r++) cells.add(new int[] {door.col(), r});
            if (goTo(world, cells, "door:" + room + ":" + door.id()) == Navigator.Drive.UNREACHABLE) blocked(world, o);
            return;
        }
        status = o.label();
        switch (o.kind()) {
            case RELIC:
                if (goTo(world, cellsAround(RoomNav.centerOf(o.col()), o.row() * T, 0), o.key()) == Navigator.Drive.UNREACHABLE) {
                    blocked(world, o);
                }
                break;
            case CACHE: {
                ShardCache cache = find(world, ShardCache.class);
                strike(world, o, cache == null ? RoomNav.centerOf(o.col()) : cache.centerX(), o.row());
                break;
            }
            case BOSS: {
                List<int[]> cells = new ArrayList<>();
                int cols = world.getTileMap().getCols();
                for (int c = 9; c < cols - 9; c++) cells.add(new int[] {c, o.row()});
                if (goTo(world, cells, o.key()) == Navigator.Drive.UNREACHABLE) blocked(world, o);
                break;
            }
            case BENCH:
                interactWith(world, find(world, Bench.class), o.key(), () -> rested.add(o.key()));
                break;
            case TALK:
                interactWith(world, find(world, LoreNpc.class), o.key(), () -> talked.add(o.key()));
                break;
            case SHRINE:
                interactWith(world, find(world, AbilityShrine.class), o.key(), () -> { });
                break;
            default:
                break;
        }
    }

    /** No route: open a cracked wall if there is one, otherwise give up on optional objectives. */
    private void blocked(GameWorld world, Quest.Objective o) {
        BreakableWall wall = find(world, BreakableWall.class);
        if (wall != null) {
            status = "Breaking a cracked wall";
            wallTarget = wall;
            breakWall(world, wall);
            return;
        }
        if (o.kind() != Quest.Kind.BOSS) skipped.add(o.key());
    }

    private void breakWall(GameWorld world, BreakableWall wall) {
        int c0 = Navigator.col(wall.getX());
        int c1 = Navigator.col(wall.getX() + wall.getWidth() - 1);
        int r0 = (int) (wall.getY() / T);
        int r1 = (int) ((wall.getY() + wall.getHeight() - 1) / T);
        List<int[]> cells = new ArrayList<>();
        for (int r = r0; r <= r1; r++) {
            cells.add(new int[] {c0 - 1, r});
            cells.add(new int[] {c1 + 1, r});
        }
        Navigator.Drive d = navigator.drive(world, RoomNav.cells(cells), "wall", out);
        if (d == Navigator.Drive.ARRIVED) {
            Player p = world.getPlayer();
            attackTowards(p, wall.centerX() > p.centerX() ? 1 : -1);
        }
    }

    /** Walk next to a target and slash it (shard caches). */
    private void strike(GameWorld world, Quest.Objective o, float targetX, int row) {
        List<int[]> cells = new ArrayList<>();
        int c = Navigator.col(targetX);
        cells.add(new int[] {c - 1, row});
        cells.add(new int[] {c + 1, row});
        cells.add(new int[] {c, row});
        Navigator.Drive d = navigator.drive(world, RoomNav.cells(cells), o.key(), out);
        if (d == Navigator.Drive.UNREACHABLE) {
            blocked(world, o);
        } else if (d == Navigator.Drive.ARRIVED) {
            Player p = world.getPlayer();
            attackTowards(p, targetX >= p.centerX() ? 1 : -1);
        }
    }

    /** Turn to face {@code dir} and slash, releasing the button between swings. */
    private void attackTowards(Player p, int dir) {
        if (p.getFacing().dx() != dir) out.add(dir > 0 ? GameAction.RIGHT : GameAction.LEFT);
        if (p.getAttack().isReady() && !pressedLastTick) {
            out.add(GameAction.ATTACK);
            pressedLastTick = true;
        } else {
            pressedLastTick = false;
        }
    }

    /** Walk up to an object and press Interact once (benches, NPCs, shrines, the merchant). */
    private void interactWith(GameWorld world, Interactable target, String key, Runnable onUse) {
        if (target == null) {
            onUse.run();
            return;
        }
        float cx = target.getHitbox().centerX();
        List<int[]> cells = cellsAround(cx, target.getHitbox().getY(), 0);
        Navigator.Drive d = navigator.drive(world, RoomNav.cells(cells), key, scratch);
        out.addAll(scratch);
        if (d == Navigator.Drive.UNREACHABLE) {
            onUse.run(); // cannot get there: treat as done
            return;
        }
        if (d != Navigator.Drive.ARRIVED) return;
        Player p = world.getPlayer();
        out.clear();
        if (p.getNearbyInteractable() == target && interactWait == 0) {
            out.add(GameAction.INTERACT);
            interactWait = 20;
            onUse.run();
        } else if (p.getNearbyInteractable() != target) {
            // nudge into the object's reach
            out.add(cx > p.centerX() ? GameAction.RIGHT : GameAction.LEFT);
        }
    }

    /** First live object of a type, including ones spawned this tick (the room may have just loaded). */
    private static <E> E find(GameWorld world, Class<E> type) {
        List<E> all = world.getEntities().getAll(type);
        return all.isEmpty() ? null : all.get(0);
    }

    private Navigator.Drive goTo(GameWorld world, List<int[]> cells, Object key) {
        return navigator.drive(world, RoomNav.cells(cells), key, out);
    }

    private static List<int[]> cellsAround(float centerX, float feetY, int spread) {
        List<int[]> cells = new ArrayList<>();
        int c = Navigator.col(centerX);
        int r = Math.round(feetY / T);
        for (int dc = -spread; dc <= spread; dc++) {
            for (int dr = 0; dr <= 1; dr++) cells.add(new int[] {c + dc, r - dr});
        }
        return cells;
    }

    // ------------------------------------------------------------------ dialogue

    /** The upgrade the pilot would buy now, or null if none is affordable. */
    private Upgrade bestUpgrade(Player p) {
        for (Upgrade u : new Upgrade[] {Upgrade.KEEN_EDGE, Upgrade.MASK_SHARD, Upgrade.EMBER_VESSEL}) {
            if (!p.getInventory().hasUpgrade(u) && p.getInventory().getShards() >= u.getPrice()) return u;
        }
        return null;
    }

    /**
     * Picks an answer when a dialogue offers choices: the hidden ending if all relics were found,
     * the best affordable upgrade in a shop, otherwise the first option.
     */
    public int chooseOption(List<String> choices) {
        if (choices.isEmpty()) return -1;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).startsWith("Kindle")) return i;
        }
        for (Upgrade u : new Upgrade[] {Upgrade.KEEN_EDGE, Upgrade.MASK_SHARD, Upgrade.EMBER_VESSEL}) {
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).startsWith(u.getDisplayName()) && shards >= u.getPrice()) return i;
            }
        }
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).equals("Leave")) return i;
        }
        return 0;
    }
}
