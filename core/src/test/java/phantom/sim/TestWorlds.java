package phantom.sim;

import java.util.HashMap;
import java.util.Map;

import phantom.events.EventBus;
import phantom.input.ScriptedInput;
import phantom.util.Constants;
import phantom.world.GameWorld;
import phantom.world.LevelFormatException;
import phantom.world.RoomData;
import phantom.world.RoomLoader;
import phantom.world.WorldData;

/** Builds tiny single-room worlds from ASCII rows for physics and combat tests. */
public final class TestWorlds {
    private TestWorlds() { }

    public static WorldData single(String region, String... rows) {
        StringBuilder json = new StringBuilder("{\"id\":\"test\",\"name\":\"Test\",\"region\":\"" + region + "\",\"tiles\":[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) json.append(',');
            json.append('"').append(rows[i]).append('"');
        }
        json.append("]}");
        try {
            RoomData room = new RoomLoader().parse(json.toString(), "test");
            Map<String, RoomData> map = new HashMap<>();
            map.put("test", room);
            return new WorldData(map, "test");
        } catch (LevelFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** A running world plus its scripted input. */
    public static final class Harness {
        public final ScriptedInput input = new ScriptedInput();
        public final EventBus bus = new EventBus();
        public final GameWorld world;

        public Harness(WorldData data) {
            world = new GameWorld(data, bus, input, null);
            world.start();
        }

        public void tick(int n) {
            for (int i = 0; i < n; i++) {
                input.poll();
                world.update(Constants.STEP);
            }
        }

        public void seconds(float s) {
            tick(Math.round(s / Constants.STEP));
        }
    }

    /** A 30-wide row: walls at both ends, {@code content} from column 1, the rest empty. */
    public static String line(String content) {
        StringBuilder b = new StringBuilder("#").append(content);
        while (b.length() < 29) b.append('.');
        return b.append('#').toString();
    }

    public static Harness harness(String... rows) {
        return new Harness(single("WHISPERING_ROOTS", rows));
    }
}
