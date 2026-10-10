package phantom.save;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain snapshot of everything that is persisted to the JSON save file.
 * <p>OOP concept: <b>encapsulation</b> - private fields, defensive copies in the accessors.
 * libGDX's {@code Json} serializer reads the private fields by reflection.</p>
 */
public final class SaveData {
    public static final int CURRENT_VERSION = 3;

    private int version = CURRENT_VERSION;
    private ArrayList<String> abilities = new ArrayList<>();
    private ArrayList<String> defeatedBosses = new ArrayList<>();
    private ArrayList<String> collected = new ArrayList<>();
    private ArrayList<String> brokenWalls = new ArrayList<>();
    private ArrayList<String> discoveredRooms = new ArrayList<>();
    private ArrayList<String> relics = new ArrayList<>();
    private ArrayList<String> upgrades = new ArrayList<>();
    private ArrayList<String> achievements = new ArrayList<>();
    private int shards;
    private String benchRoom;
    private String benchId;
    private boolean shadeActive;
    private String shadeRoom;
    private float shadeX;
    private float shadeY;
    private int shadeShards;
    private int deaths;
    private int enemiesKilled;
    private float playTime;
    private String ending;

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public List<String> getAbilities() { return copy(abilities); }
    public void setAbilities(List<String> v) { abilities = copy(v); }
    public List<String> getDefeatedBosses() { return copy(defeatedBosses); }
    public void setDefeatedBosses(List<String> v) { defeatedBosses = copy(v); }
    public List<String> getCollected() { return copy(collected); }
    public void setCollected(List<String> v) { collected = copy(v); }
    public List<String> getBrokenWalls() { return copy(brokenWalls); }
    public void setBrokenWalls(List<String> v) { brokenWalls = copy(v); }
    public List<String> getDiscoveredRooms() { return copy(discoveredRooms); }
    public void setDiscoveredRooms(List<String> v) { discoveredRooms = copy(v); }
    public List<String> getRelics() { return copy(relics); }
    public void setRelics(List<String> v) { relics = copy(v); }
    public List<String> getUpgrades() { return copy(upgrades); }
    public void setUpgrades(List<String> v) { upgrades = copy(v); }
    public List<String> getAchievements() { return copy(achievements); }
    public void setAchievements(List<String> v) { achievements = copy(v); }

    public int getShards() { return shards; }
    public void setShards(int shards) { this.shards = Math.max(0, shards); }
    public String getBenchRoom() { return benchRoom; }
    public String getBenchId() { return benchId; }
    public void setBench(String room, String id) { benchRoom = room; benchId = id; }

    public boolean isShadeActive() { return shadeActive; }
    public String getShadeRoom() { return shadeRoom; }
    public float getShadeX() { return shadeX; }
    public float getShadeY() { return shadeY; }
    public int getShadeShards() { return shadeShards; }
    public void setShade(boolean active, String room, float x, float y, int shardsHeld) {
        shadeActive = active;
        shadeRoom = room;
        shadeX = x;
        shadeY = y;
        shadeShards = Math.max(0, shardsHeld);
    }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = Math.max(0, deaths); }
    public int getEnemiesKilled() { return enemiesKilled; }
    public void setEnemiesKilled(int v) { enemiesKilled = Math.max(0, v); }
    public float getPlayTime() { return playTime; }
    public void setPlayTime(float playTime) { this.playTime = Math.max(0f, playTime); }
    public String getEnding() { return ending; }
    public void setEnding(String ending) { this.ending = ending; }

    /** Replaces any missing lists (e.g. from a hand-edited file) with empty ones. */
    void sanitize() {
        if (abilities == null) abilities = new ArrayList<>();
        if (defeatedBosses == null) defeatedBosses = new ArrayList<>();
        if (collected == null) collected = new ArrayList<>();
        if (brokenWalls == null) brokenWalls = new ArrayList<>();
        if (discoveredRooms == null) discoveredRooms = new ArrayList<>();
        if (relics == null) relics = new ArrayList<>();
        if (upgrades == null) upgrades = new ArrayList<>();
        if (achievements == null) achievements = new ArrayList<>();
        shards = Math.max(0, shards);
        playTime = Float.isFinite(playTime) ? Math.max(0f, playTime) : 0f;
    }

    private static ArrayList<String> copy(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
