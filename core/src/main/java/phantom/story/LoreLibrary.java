package phantom.story;

import java.util.List;

import phantom.world.GameProgress;

/**
 * All NPC dialogue. Lines can change as the story advances.
 * <p>OOP concept: a data/lookup class; keeps story text out of entity code.</p>
 */
public final class LoreLibrary {
    private LoreLibrary() { }

    public static String nameOf(String npcId) {
        switch (npcId) {
            case "ossian": return "Ossian the Lamplighter";
            case "quill": return "Quill, Moth Merchant";
            case "seraphine": return "Seraphine, Drowned Cartographer";
            case "vesper": return "Old Vesper";
            default: return "Stranger";
        }
    }

    public static List<String> linesFor(String npcId, GameProgress progress) {
        switch (npcId) {
            case "ossian": return ossian(progress);
            case "seraphine": return seraphine(progress);
            case "vesper": return vesper(progress);
            default: return List.of("...");
        }
    }

    private static List<String> ossian(GameProgress p) {
        if (p.isBossDefeated("gnarlwarden")) {
            return List.of("The roots have gone quiet. You did that, little light.",
                    "East, past the Warden's den, the old cistern still turns its brass wheels.",
                    "Mind the gaps. Dark shadows can cross what feet cannot.");
        }
        return List.of("Another lantern, falling from Veyra... I had stopped counting.",
                "I am Ossian. I light the way for those who descend, though few return.",
                "The Hollow Mist rises from the Heart below. It eats colour, then memory, then names.",
                "Rest on the stone benches. They remember you, even when you forget yourself.",
                "Strike with your lantern-blade - each blow drinks a little Light. Hold F to mend.");
    }

    private static List<String> seraphine(GameProgress p) {
        if (p.isBossDefeated("gilded_warden")) {
            return List.of("The Warden's engine has stopped. The water is so quiet now.",
                    "Below the cistern lies only the Heart. My maps end there. Yours must not.");
        }
        return List.of("Careful, the floor is older than it looks. Everything here is.",
                "I drew this city once, every stair and sluice. Then the Mist drank the ink.",
                "The Gilded Warden still guards the floodgates. It was built to protect us.",
                "It no longer remembers who 'us' was.",
                "They say three Ember Relics were hidden when the Mist first rose. Find them, if you can.");
    }

    private static List<String> vesper(GameProgress p) {
        return List.of("So. The last lantern reaches the last lantern.",
                "Beyond this door beats the Hollow Heart. It is not evil. It is hungry, and it is tired.",
                "When it falls you must choose: seal the Mist inside yourself, or shatter it and let it go.",
                "...unless you carry all three Embers. Then, perhaps, there is a third way.",
                "Rest first. Whatever you choose, you will not climb back the same.");
    }
}
