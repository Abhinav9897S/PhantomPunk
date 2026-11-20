package phantom.autoplay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import phantom.abilities.AbilityType;
import phantom.story.LoreLibrary;
import phantom.world.DoorData;
import phantom.world.RoomData;
import phantom.world.SpawnData;
import phantom.world.WorldData;

/**
 * The autoplay "walkthrough", worked out from the room files alone: a depth-first tour of the room
 * tree that explores side branches (secret rooms, shrines) before the branch that leads to a boss,
 * listing what to do in each room along the way.
 * <p>OOP concept: a pure data/analysis class; records for objectives.</p>
 */
final class Quest {
    /** What an objective asks the pilot to do. */
    enum Kind { SHRINE, TALK, BENCH, RELIC, CACHE, BOSS }

    /** One thing to do in one room, at a tile position. */
    record Objective(Kind kind, String roomId, int col, int row, String key, String label) { }

    private final WorldData world;
    private final Map<String, List<DoorData>> doors = new HashMap<>();
    private final List<Objective> objectives = new ArrayList<>();

    Quest(WorldData world) {
        this.world = world;
        for (RoomData r : world.all()) doors.put(r.getId(), r.getDoors());
        visit(world.getStartRoom(), new HashSet<>());
    }

    private void visit(String roomId, Set<String> seen) {
        if (!seen.add(roomId)) return;
        RoomData room = world.get(roomId);
        if (room == null) return;
        addObjectives(room);
        List<String> children = new ArrayList<>();
        for (DoorData d : room.getDoors()) {
            if (!seen.contains(d.targetRoom()) && !children.contains(d.targetRoom())) children.add(d.targetRoom());
        }
        // dead-end branches first (secret rooms, shrines), the way onward to the next boss last
        children.sort((a, b) -> Boolean.compare(leadsToBoss(a, roomId), leadsToBoss(b, roomId)));
        for (String child : children) visit(child, seen);
    }

    private boolean leadsToBoss(String roomId, String from) {
        Set<String> seen = new HashSet<>();
        seen.add(from);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(roomId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (!seen.add(id)) continue;
            RoomData r = world.get(id);
            if (r == null) continue;
            if (r.hasSpawn("boss")) return true;
            for (DoorData d : r.getDoors()) queue.add(d.targetRoom());
        }
        return false;
    }

    private void addObjectives(RoomData room) {
        List<Objective> boss = new ArrayList<>();
        for (SpawnData s : room.getSpawns()) {
            String id = s.id(room.getId());
            switch (s.type()) {
                case "npc": add(Kind.TALK, room, s, id, "Listen to " + LoreLibrary.nameOf(s.prop("id", ""))); break;
                case "shrine": add(Kind.SHRINE, room, s, id, "Claim the " + abilityName(s.prop("ability", "")) + " shrine"); break;
                case "bench": add(Kind.BENCH, room, s, id, "Rest at the bench"); break;
                case "cache": add(Kind.CACHE, room, s, id, "Break the shard cache"); break;
                case "relic": add(Kind.RELIC, room, s, id, "Take the Ember Relic"); break;
                case "boss": boss.add(new Objective(Kind.BOSS, room.getId(), s.col(), s.row(), s.prop("boss", ""),
                        "Defeat the " + titleCase(s.prop("boss", "guardian")))); break;
                default: break;
            }
        }
        List<Objective> here = new ArrayList<>();
        for (int i = objectives.size() - 1; i >= 0 && objectives.get(i).roomId().equals(room.getId()); i--) {
            here.add(0, objectives.remove(i));
        }
        here.sort((a, b) -> Integer.compare(a.kind().ordinal(), b.kind().ordinal()));
        objectives.addAll(here);
        objectives.addAll(boss);
    }

    private static String abilityName(String ability) {
        try {
            return AbilityType.valueOf(ability).getDisplayName();
        } catch (IllegalArgumentException e) {
            return "lost";
        }
    }

    /** "gilded_warden" -> "Gilded Warden". */
    private static String titleCase(String id) {
        StringBuilder b = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    private void add(Kind kind, RoomData room, SpawnData s, String id, String label) {
        objectives.add(new Objective(kind, room.getId(), s.col(), s.row(), id, label));
    }

    List<Objective> objectives() { return objectives; }

    /** The door to leave {@code from} by, to get one room closer to {@code to}; null if already there or no path. */
    DoorData nextDoor(String from, String to) {
        if (from.equals(to)) return null;
        Map<String, DoorData> firstDoor = new HashMap<>();
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        seen.add(from);
        for (DoorData d : doors.getOrDefault(from, List.of())) {
            if (seen.add(d.targetRoom())) {
                firstDoor.put(d.targetRoom(), d);
                queue.add(d.targetRoom());
            }
        }
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (id.equals(to)) return firstDoor.get(id);
            for (DoorData d : doors.getOrDefault(id, List.of())) {
                if (seen.add(d.targetRoom())) {
                    firstDoor.put(d.targetRoom(), firstDoor.get(id));
                    queue.add(d.targetRoom());
                }
            }
        }
        return null;
    }
}
