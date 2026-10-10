package phantom.ui;

import com.badlogic.gdx.graphics.Color;

import phantom.util.Constants;
import phantom.world.GameProgress;
import phantom.world.GameWorld;
import phantom.world.Region;
import phantom.world.RoomData;
import phantom.world.WorldData;

/**
 * Draws the discovered part of the map: each visited room as a tinted block at its world
 * position, benches, boss lairs and a blinking marker for the player.
 * <p>OOP concept: a view over {@link WorldData} and {@link GameProgress}; the map "fills in"
 * because only discovered rooms are drawn.</p>
 */
public final class MapRenderer {
    private static final Color LABEL = new Color(0.85f, 0.88f, 0.94f, 1f);

    public void render(UiKit ui, GameWorld world, float alpha, float time) {
        WorldData data = world.getWorldData();
        GameProgress progress = world.getProgress();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (RoomData r : data.all()) {
            minX = Math.min(minX, r.getMapX());
            minY = Math.min(minY, r.getMapY());
            maxX = Math.max(maxX, r.getMapX() + r.getCols());
            maxY = Math.max(maxY, r.getMapY() + r.getRows());
        }
        float areaW = 1120;
        float areaH = 500;
        float scale = Math.min(areaW / (maxX - minX), areaH / (maxY - minY));
        float ox = (Constants.UI_WIDTH - (maxX - minX) * scale) / 2f;
        float oy = 120 + (areaH - (maxY - minY) * scale) / 2f;

        ui.rect(0, 0, Constants.UI_WIDTH, Constants.UI_HEIGHT, 0.01f, 0.01f, 0.02f, 0.88f * alpha);
        ui.centered(ui.fonts().large(), "Map of the Descent", Constants.UI_WIDTH / 2f, Constants.UI_HEIGHT - 36, LABEL, alpha);
        ui.ornament(Constants.UI_WIDTH / 2f, Constants.UI_HEIGHT - 96, 380, alpha * 0.8f);

        for (RoomData r : data.all()) {
            if (!progress.isDiscovered(r.getId())) continue;
            float x = ox + (r.getMapX() - minX) * scale;
            float y = oy + (r.getMapY() - minY) * scale;
            float w = r.getCols() * scale;
            float h = r.getRows() * scale;
            Color c = tint(r.getRegion());
            boolean here = r.getId().equals(world.getRoomId());
            ui.rect(x, y, w, h, c.r, c.g, c.b, (here ? 0.75f : 0.45f) * alpha);
            ui.rect(x, y, w, 1.5f, 0.9f, 0.9f, 0.95f, 0.7f * alpha);
            ui.rect(x, y + h - 1.5f, w, 1.5f, 0.9f, 0.9f, 0.95f, 0.7f * alpha);
            ui.rect(x, y, 1.5f, h, 0.9f, 0.9f, 0.95f, 0.7f * alpha);
            ui.rect(x + w - 1.5f, y, 1.5f, h, 0.9f, 0.9f, 0.95f, 0.7f * alpha);
            if (r.hasSpawn("bench")) ui.diamond(x + w / 2f, y + h / 2f, 10, alpha);
            if (r.hasSpawn("boss")) ui.rect(x + w / 2f - 5, y + h / 2f - 5, 10, 10, 0.9f, 0.3f, 0.3f, alpha);
            if (here) {
                float px = x + world.getPlayer().centerX() / Constants.TILE * scale;
                float py = y + world.getPlayer().centerY() / Constants.TILE * scale;
                float blink = 0.6f + 0.4f * (float) Math.sin(time * 6f);
                ui.rect(px - 4, py - 4, 8, 8, 1f, 0.95f, 0.8f, blink * alpha);
            }
        }
        float ly = 62;
        float lx = Constants.UI_WIDTH / 2f - 300;
        ui.diamond(lx, ly - 8, 10, alpha);
        ui.text(ui.fonts().small(), "Bench", lx + 16, ly, LABEL, alpha * 0.8f);
        ui.rect(lx + 120, ly - 13, 10, 10, 0.9f, 0.3f, 0.3f, alpha);
        ui.text(ui.fonts().small(), "Guardian", lx + 138, ly, LABEL, alpha * 0.8f);
        ui.rect(lx + 270, ly - 12, 8, 8, 1f, 0.95f, 0.8f, alpha);
        ui.text(ui.fonts().small(), "You", lx + 286, ly, LABEL, alpha * 0.8f);
        ui.text(ui.fonts().small(), "Tab / M: close", lx + 400, ly, LABEL, alpha * 0.6f);
    }

    private static Color tint(Region region) {
        switch (region) {
            case WHISPERING_ROOTS: return new Color(0.25f, 0.55f, 0.45f, 1f);
            case GILDED_CISTERN: return new Color(0.6f, 0.45f, 0.25f, 1f);
            default: return new Color(0.45f, 0.35f, 0.65f, 1f);
        }
    }
}
