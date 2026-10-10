package phantom.world.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.world.DoorData;
import phantom.world.EnemyFactory;
import phantom.world.Region;
import phantom.world.RoomData;
import phantom.world.SpawnData;
import phantom.world.TileMap;
import phantom.world.TileType;
import phantom.world.WorldData;

/**
 * Proves the world can be completed. Per room it checks that spawns and doors are not inside
 * walls and that every door links back; over the whole map it simulates a player starting with
 * no abilities, collecting shrines and boss rewards as they become reachable, until every
 * region and the final boss are reachable. It also checks there is a bench next to every boss.
 * <p>OOP concept: an analysis class returning an immutable {@link Report} record.</p>
 */
public final class LevelValidator {
    private static final Set<String> PROP_TYPES = Set.of("bench", "npc", "shop", "light", "rock", "shrine", "relic", "cache");

    /** Result of a validation run. */
    public record Report(List<String> errors, Set<String> reachableRooms, Set<AbilityType> abilities) {
        public boolean isValid() { return errors.isEmpty(); }
    }

    public Report validate(WorldData world) {
        List<String> errors = new ArrayList<>();
        for (RoomData room : world.all()) checkRoom(world, room, errors);
        Set<AbilityType> abilities = EnumSet.of(AbilityType.FOCUS);
        Exploration ex = explore(world, abilities, true);
        while (abilities.addAll(ex.rewards)) {
            ex = explore(world, abilities, true);
        }
        Set<String> reached = ex.rooms;
        for (RoomData room : world.all()) {
            if (!reached.contains(room.getId())) errors.add("Room '" + room.getId() + "' cannot be reached from the start");
        }
        for (Region region : Region.values()) {
            boolean any = world.all().stream().anyMatch(r -> r.getRegion() == region && reached.contains(r.getId()));
            if (!any) errors.add("Region " + region + " is unreachable");
        }
        boolean finalReachable = world.all().stream().anyMatch(r -> r.hasSpawn("boss") && reached.contains(r.getId())
                && "hollow_heart".equals(bossOf(r)));
        if (!finalReachable) errors.add("The final boss cannot be reached");
        return new Report(List.copyOf(errors), Set.copyOf(reached), Set.copyOf(abilities));
    }

    /** Rooms reachable with a fixed ability set (no collecting), used by gating tests. */
    public Set<String> reachableRooms(WorldData world, Set<AbilityType> abilities) {
        return explore(world, abilities, false).rooms;
    }

    private static final class Exploration {
        private final Set<String> rooms = new LinkedHashSet<>();
        private final Set<AbilityType> rewards = EnumSet.noneOf(AbilityType.class);
    }

    private Exploration explore(WorldData world, Set<AbilityType> abilities, boolean collect) {
        Exploration ex = new Exploration();
        MovementModel model = new MovementModel(abilities);
        Deque<String[]> queue = new ArrayDeque<>();
        Set<String> seenEntries = new HashSet<>();
        queue.add(new String[] {world.getStartRoom(), null});
        while (!queue.isEmpty()) {
            String[] entry = queue.poll();
            if (!seenEntries.add(entry[0] + "|" + entry[1])) continue;
            RoomData room = world.get(entry[0]);
            if (room == null) continue;
            ex.rooms.add(room.getId());
            TileMap map = room.createTileMap();
            RoomReachability reach = new RoomReachability(map, model);
            int sc;
            int sr;
            if (entry[1] == null) {
                sc = room.getStartCol();
                sr = room.getStartRow();
            } else {
                DoorData d = room.findDoor(entry[1]);
                if (d == null) continue;
                sc = d.arrivalCol();
                sr = d.row();
            }
            Set<Integer> nodes = reach.explore(sc, sr);
            for (DoorData d : room.getDoors()) {
                if (touchesDoor(reach, nodes, d)) queue.add(new String[] {d.targetRoom(), d.targetDoor()});
            }
            if (collect) collectRewards(room, reach, nodes, ex.rewards);
        }
        return ex;
    }

    private boolean touchesDoor(RoomReachability reach, Set<Integer> nodes, DoorData d) {
        for (int k = 0; k < DoorData.HEIGHT - 1; k++) {
            if (reach.canTouch(nodes, d.col(), d.row() + k)) return true;
        }
        return false;
    }

    private void collectRewards(RoomData room, RoomReachability reach, Set<Integer> nodes, Set<AbilityType> rewards) {
        for (SpawnData s : room.getSpawns()) {
            String ability = null;
            if (s.type().equals("shrine")) ability = s.prop("ability", "");
            if (s.type().equals("boss")) ability = s.prop("grants", "");
            if (ability == null || ability.isEmpty()) continue;
            if (reach.canTouch(nodes, s.col(), s.row())) {
                try {
                    rewards.add(AbilityType.valueOf(ability));
                } catch (IllegalArgumentException ignored) {
                    // reported by checkRoom
                }
            }
        }
    }

    private void checkRoom(WorldData world, RoomData room, List<String> errors) {
        String id = room.getId();
        TileMap map = room.createTileMap();
        if (room.hasStart() && blocked(map, room.getStartCol(), room.getStartRow())) {
            errors.add(id + ": player start is inside a solid tile");
        }
        if (room.getCols() < 30 || room.getRows() < 17) errors.add(id + ": room is smaller than one screen (30x17)");
        for (SpawnData s : room.getSpawns()) {
            if (blocked(map, s.col(), s.row())) errors.add(id + ": spawn '" + s.type() + "' is inside a solid tile");
            if (!PROP_TYPES.contains(s.type()) && !EnemyFactory.isEnemyType(s.type())) {
                errors.add(id + ": unknown spawn type '" + s.type() + "'");
            }
            if (s.type().equals("boss") && !EnemyFactory.isBossId(s.prop("boss", ""))) {
                errors.add(id + ": unknown boss '" + s.prop("boss", "") + "'");
            }
            checkAbility(id, s, "shrine", "ability", errors);
            checkAbility(id, s, "boss", "grants", errors);
        }
        for (DoorData d : room.getDoors()) checkDoor(world, room, map, d, errors);
        if (room.hasSpawn("boss") && !benchNear(world, room)) {
            errors.add(id + ": no bench in or next to this boss room");
        }
    }

    private void checkAbility(String id, SpawnData s, String type, String key, List<String> errors) {
        if (!s.type().equals(type)) return;
        String v = s.prop(key, "");
        if (v.isEmpty()) return;
        try {
            AbilityType.valueOf(v);
        } catch (IllegalArgumentException e) {
            errors.add(id + ": unknown ability '" + v + "'");
        }
    }

    private void checkDoor(WorldData world, RoomData room, TileMap map, DoorData d, List<String> errors) {
        String id = room.getId() + "." + d.id();
        for (int k = 0; k < DoorData.HEIGHT; k++) {
            if (blocked(map, d.col(), d.row() + k)) errors.add(id + ": door cell is solid");
        }
        if (blocked(map, d.arrivalCol(), d.row()) || blocked(map, d.arrivalCol(), d.row() + 1)) {
            errors.add(id + ": arrival point is inside a wall");
        }
        RoomData target = world.get(d.targetRoom());
        if (target == null) {
            errors.add(id + ": leads to missing room '" + d.targetRoom() + "'");
            return;
        }
        DoorData back = target.findDoor(d.targetDoor());
        if (back == null) {
            errors.add(id + ": target door '" + d.targetRoom() + "." + d.targetDoor() + "' does not exist");
        } else if (!back.targetRoom().equals(room.getId()) || !back.targetDoor().equals(d.id())) {
            errors.add(id + ": target door does not lead back here");
        } else if (back.side() == d.side()) {
            errors.add(id + ": both ends of the door are on the same side");
        }
    }

    private boolean benchNear(WorldData world, RoomData bossRoom) {
        if (bossRoom.hasSpawn("bench")) return true;
        for (DoorData d : bossRoom.getDoors()) {
            RoomData next = world.get(d.targetRoom());
            if (next != null && next.hasSpawn("bench")) return true;
        }
        return false;
    }

    private static String bossOf(RoomData r) {
        for (SpawnData s : r.getSpawns()) {
            if (s.type().equals("boss")) return s.prop("boss", "");
        }
        return "";
    }

    private static boolean blocked(TileMap map, int c, int r) {
        return map.get(c, r) == TileType.SOLID;
    }
}
