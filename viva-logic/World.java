import java.util.ArrayList;
import java.util.List;

/**
 * Builds the rooms of the game in order.
 * OOP: FACTORY - create() hides how the world is put together and returns ready objects.
 * OOP: STATIC method + private constructor (a utility class, never instantiated).
 */
public final class World {
    private World() { }

    public static List<Room> create() {
        List<Room> rooms = new ArrayList<>();
        rooms.add(new Room("Fallen Gate", null, null)
                .add(new Bench())
                .add(new Npc("Ossian the Lamplighter",
                        "Another lantern, falling from Veyra...",
                        "Strike with your blade - each blow drinks a little Light. Focus to mend.")));
        rooms.add(new Room("Mossy Hollow", null, null).add(new Crawler()).add(new Crawler()));
        rooms.add(new Room("Root Shrine", null, null).add(new Sentinel()).add(new Shrine(AbilityType.LIGHT_BOLT)));
        rooms.add(new Room("Thornway", null, null).add(new Crawler()).add(new Bench()));
        rooms.add(new Room("Gnarled Den", null, null).add(new Gnarlwarden()));
        rooms.add(new Room("Cistern Steps", AbilityType.DASH, "A pit of spikes is too wide to jump. You need to dash across.")
                .add(new Sentinel()).add(new Crawler()));
        rooms.add(new Room("Drowned Chapel", null, null)
                .add(new Bench())
                .add(new Npc("Seraphine", "The Gilded Warden still guards the floodgates.",
                        "It no longer remembers who it was built to protect.")));
        rooms.add(new Room("Warden's Gallery", null, null).add(new GildedWarden()));
        rooms.add(new Room("Mist Verge", AbilityType.DOUBLE_JUMP, "The ledges float too high. You need a second jump.")
                .add(new Sentinel()));
        rooms.add(new Room("Last Lantern", null, null)
                .add(new Bench())
                .add(new Npc("Old Vesper", "Beyond this door beats the Hollow Heart.",
                        "It is not evil. It is hungry, and it is tired.")));
        rooms.add(new Room("The Umbral Heart", null, null).add(new HollowHeart()));
        return rooms;
    }
}
