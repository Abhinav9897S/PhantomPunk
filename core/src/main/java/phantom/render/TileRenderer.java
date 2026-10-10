package phantom.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import phantom.art.SpriteFactory;
import phantom.art.SpriteSheet;
import phantom.art.TilePainter;
import phantom.util.Constants;
import phantom.util.GameMath;
import phantom.world.Region;
import phantom.world.TileMap;
import phantom.world.TileType;

/**
 * Draws the visible part of a tile map: rock interiors first, then decorated edges
 * (moss fringes on top, roots hanging underneath, rim light on the sides), ledges,
 * spikes, gates, water and mist pools.
 * <p>OOP concept: a focused renderer class (single responsibility); camera culling keeps it fast.</p>
 */
public final class TileRenderer {
    private static final int T = Constants.TILE;
    private final SpriteFactory art;

    public TileRenderer(SpriteFactory art) {
        this.art = art;
    }

    public void render(SpriteBatch batch, TileMap map, Region region, float camX, float camY,
                       float viewW, float viewH, float time) {
        int c0 = Math.max(0, TileMap.col(camX - viewW / 2f) - 1);
        int c1 = Math.min(map.getCols() - 1, TileMap.col(camX + viewW / 2f) + 1);
        int r0 = Math.max(0, TileMap.row(camY - viewH / 2f) - 1);
        int r1 = Math.min(map.getRows() - 1, TileMap.row(camY + viewH / 2f) + 1);
        SpriteSheet solid = art.sheet(TilePainter.name(region, "solid"));
        SpriteSheet breakable = art.sheet(TilePainter.name(region, "breakable"));
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                TileType t = map.get(c, r);
                float x = c * T;
                float y = r * T;
                if (t == TileType.SOLID) {
                    batch.draw(solid.frame(variant(c, r, solid.count())), x, y, T, T);
                } else if (t == TileType.BREAKABLE) {
                    batch.draw(breakable.frame(0), x, y, T, T);
                }
            }
        }
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                drawDetail(batch, map, region, c, r, time);
            }
        }
    }

    private void drawDetail(SpriteBatch batch, TileMap map, Region region, int c, int r, float time) {
        TileType t = map.get(c, r);
        float x = c * T;
        float y = r * T;
        switch (t) {
            case SOLID:
            case BREAKABLE:
                drawEdges(batch, map, region, c, r, x, y);
                break;
            case ONE_WAY:
                batch.draw(art.sheet(TilePainter.name(region, "oneway")).frame(0), x, y + T - 14, T, 14);
                break;
            case SPIKES:
                SpriteSheet spikes = art.sheet(TilePainter.name(region, "spikes"));
                batch.draw(spikes.frame(variant(c, r, spikes.count())), x, y, T, T);
                break;
            case GATE:
                if (map.areGatesClosed()) batch.draw(art.sheet(TilePainter.name(region, "gate")).frame(0), x, y, T, T);
                break;
            case WATER:
                drawLiquid(batch, map, c, r, x, y, time, 0.1f, 0.2f, 0.32f, 0.5f, 0.45f, 0.7f, 0.9f);
                break;
            case MIST:
                drawLiquid(batch, map, c, r, x, y, time, 0.06f, 0.03f, 0.1f, 0.92f, 0.75f, 0.6f, 1f);
                break;
            default:
                break;
        }
    }

    private void drawEdges(SpriteBatch batch, TileMap map, Region region, int c, int r, float x, float y) {
        if (!solidish(map, c, r + 1)) {
            SpriteSheet top = art.sheet(TilePainter.name(region, "top"));
            batch.draw(top.frame(variant(c, r, top.count())), x, y + T - 8, T, 20);
        }
        if (!solidish(map, c, r - 1)) {
            SpriteSheet bottom = art.sheet(TilePainter.name(region, "bottom"));
            batch.draw(bottom.frame(variant(c, r + 3, bottom.count())), x, y - 11, T, 14);
        }
        TextureRegion side = art.sheet(TilePainter.name(region, "side")).frame(0);
        if (!solidish(map, c - 1, r)) batch.draw(side, x, y, 6, T);
        if (!solidish(map, c + 1, r)) batch.draw(side, x + T, y, -6, T);
    }

    private void drawLiquid(SpriteBatch batch, TileMap map, int c, int r, float x, float y, float time,
                            float cr, float cg, float cb, float ca, float sr, float sg, float sb) {
        TextureRegion px = art.region("pixel");
        batch.setColor(cr, cg, cb, ca);
        batch.draw(px, x, y, T, T);
        if (map.get(c, r + 1) != map.get(c, r)) {
            for (int i = 0; i < 4; i++) {
                float wave = (float) Math.sin(time * 2.2f + (x + i * 8) * 0.07f) * 2f;
                batch.setColor(sr, sg, sb, 0.55f);
                batch.draw(px, x + i * 8, y + T - 3 + wave, 8, 2);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private static boolean solidish(TileMap map, int c, int r) {
        if (!map.inBounds(c, r)) return true;
        TileType t = map.get(c, r);
        return t == TileType.SOLID || t == TileType.BREAKABLE;
    }

    private static int variant(int c, int r, int count) {
        return (int) (GameMath.hash(c, r) * count) % count;
    }
}
