package phantom.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import phantom.save.SaveData;
import phantom.save.Saveable;

/**
 * World-level progress: bosses beaten, items taken, walls broken, rooms discovered,
 * the last bench, the shade left on death, and play statistics.
 * <p>OOP concept: {@link Saveable} implementation; {@link Set}s guarantee no duplicates.</p>
 */
public final class GameProgress implements Saveable {
    private final Set<String> defeatedBosses = new LinkedHashSet<>();
    private final Set<String> collected = new LinkedHashSet<>();
    private final Set<String> brokenWalls = new LinkedHashSet<>();
    private final Set<String> discoveredRooms = new LinkedHashSet<>();
    private final Set<String> achievements = new LinkedHashSet<>();
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

    public boolean isBossDefeated(String id) { return defeatedBosses.contains(id); }
    public void markBossDefeated(String id) { defeatedBosses.add(id); }
    public int getBossesDefeated() { return defeatedBosses.size(); }

    public boolean isCollected(String id) { return collected.contains(id); }
    public boolean markCollected(String id) { return collected.add(id); }

    public boolean isWallBroken(String id) { return brokenWalls.contains(id); }
    public void markWallBroken(String id) { brokenWalls.add(id); }

    /** Records a visit and returns true if the room was new. */
    public boolean discover(String roomId) { return discoveredRooms.add(roomId); }
    public boolean isDiscovered(String roomId) { return discoveredRooms.contains(roomId); }
    public Set<String> getDiscoveredRooms() { return Collections.unmodifiableSet(discoveredRooms); }

    public boolean unlockAchievement(String title) { return achievements.add(title); }
    public Set<String> getAchievements() { return Collections.unmodifiableSet(achievements); }

    public void setBench(String roomId, String id) {
        benchRoom = roomId;
        benchId = id;
    }

    public String getBenchRoom() { return benchRoom; }
    public String getBenchId() { return benchId; }

    public void leaveShade(String roomId, float x, float y, int shards) {
        shadeActive = shards > 0;
        shadeRoom = roomId;
        shadeX = x;
        shadeY = y;
        shadeShards = shards;
    }

    public void clearShade() {
        shadeActive = false;
        shadeShards = 0;
    }

    public boolean hasShadeIn(String roomId) { return shadeActive && roomId.equals(shadeRoom); }
    public float getShadeX() { return shadeX; }
    public float getShadeY() { return shadeY; }
    public int getShadeShards() { return shadeShards; }

    public void addDeath() { deaths++; }
    public int getDeaths() { return deaths; }
    public void addKill() { enemiesKilled++; }
    public int getEnemiesKilled() { return enemiesKilled; }
    public void addPlayTime(float dt) { playTime += dt; }
    public float getPlayTime() { return playTime; }
    public String getEnding() { return ending; }
    public void setEnding(String ending) { this.ending = ending; }

    @Override
    public void writeTo(SaveData data) {
        data.setDefeatedBosses(new ArrayList<>(defeatedBosses));
        data.setCollected(new ArrayList<>(collected));
        data.setBrokenWalls(new ArrayList<>(brokenWalls));
        data.setDiscoveredRooms(new ArrayList<>(discoveredRooms));
        data.setAchievements(new ArrayList<>(achievements));
        data.setBench(benchRoom, benchId);
        data.setShade(shadeActive, shadeRoom, shadeX, shadeY, shadeShards);
        data.setDeaths(deaths);
        data.setEnemiesKilled(enemiesKilled);
        data.setPlayTime(playTime);
        data.setEnding(ending);
    }

    @Override
    public void readFrom(SaveData data) {
        reset(defeatedBosses, data.getDefeatedBosses());
        reset(collected, data.getCollected());
        reset(brokenWalls, data.getBrokenWalls());
        reset(discoveredRooms, data.getDiscoveredRooms());
        reset(achievements, data.getAchievements());
        benchRoom = data.getBenchRoom();
        benchId = data.getBenchId();
        shadeActive = data.isShadeActive() && data.getShadeRoom() != null;
        shadeRoom = data.getShadeRoom();
        shadeX = data.getShadeX();
        shadeY = data.getShadeY();
        shadeShards = data.getShadeShards();
        deaths = data.getDeaths();
        enemiesKilled = data.getEnemiesKilled();
        playTime = data.getPlayTime();
        ending = data.getEnding();
    }

    private static void reset(Set<String> target, java.util.List<String> values) {
        target.clear();
        target.addAll(values);
    }
}
