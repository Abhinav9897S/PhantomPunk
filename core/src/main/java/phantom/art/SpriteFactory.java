package phantom.art;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Runs every {@link SpritePainter} once at startup, packs the results into texture atlases
 * and hands out cached {@link SpriteSheet}s by name.
 * <p>OOP concepts: <b>Factory</b> for sprites, implements {@link SpriteRegistry}, owns GPU
 * resources and releases them in {@link #dispose()}.</p>
 */
public final class SpriteFactory implements SpriteRegistry, Disposable {
    /** Painters draw at 4 buffer pixels per logical unit... */
    public static final float SUPERSAMPLE = 4f;
    /** ...and textures end up at 2 texels per logical unit (crisp on a zoomed camera). */
    private static final float TEXELS_PER_UNIT = 2f;

    private record Meta(int frames, float width, float height, float foot, boolean flash) { }

    private final PixmapPacker packer = new PixmapPacker(2048, 2048, Pixmap.Format.RGBA8888, 2, false);
    private final Map<String, Meta> metas = new HashMap<>();
    private final Map<String, SpriteSheet> sheets = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final List<Texture> owned = new ArrayList<>();
    private final PaletteLibrary palettes = new PaletteLibrary();
    private TextureAtlas atlas;
    private SpriteSheet missing;

    /** Paints and uploads everything. Must run on the GL thread. */
    public void build() {
        List<SpritePainter> painters = List.of(new CharacterPainter(), new CreaturePainter(), new BossPainter(),
                new PropPainter(), new NpcPainter(), new TilePainter(palettes), new BackgroundPainter(palettes), new UiPainter());
        for (SpritePainter painter : painters) painter.paint(this);
        atlas = packer.generateTextureAtlas(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear, false);
        packer.dispose();
        for (Map.Entry<String, Meta> e : metas.entrySet()) {
            Meta m = e.getValue();
            TextureRegion[] frames = new TextureRegion[m.frames()];
            TextureRegion[] flashes = new TextureRegion[m.flash() ? m.frames() : 1];
            for (int i = 0; i < m.frames(); i++) {
                frames[i] = atlas.findRegion(e.getKey() + "#" + i);
                if (m.flash()) flashes[i] = atlas.findRegion(e.getKey() + "#" + i + "!");
            }
            sheets.put(e.getKey(), new SpriteSheet(frames, flashes, m.width(), m.height(), m.foot()));
        }
        missing = makeMissing();
    }

    @Override
    public Canvas canvas(float logicalWidth, float logicalHeight) {
        return new Canvas(logicalWidth, logicalHeight, SUPERSAMPLE);
    }

    @Override
    public Canvas canvas(float logicalWidth, float logicalHeight, float scale) {
        return new Canvas(logicalWidth, logicalHeight, scale);
    }

    @Override
    public void sprite(String name, float footOffset, boolean flash, Canvas... frames) {
        float w = 0;
        float h = 0;
        for (int i = 0; i < frames.length; i++) {
            Canvas source = frames[i];
            w = source.getPixelWidth() / source.getScale();
            h = source.getPixelHeight() / source.getScale();
            int factor = Math.max(1, Math.round(source.getScale() / TEXELS_PER_UNIT));
            Canvas finalCanvas = factor > 1 ? source.downsample(factor) : source;
            pack(name + "#" + i, finalCanvas);
            if (flash) pack(name + "#" + i + "!", finalCanvas.silhouette());
        }
        metas.put(name, new Meta(frames.length, w, h, footOffset, flash));
    }

    private void pack(String key, Canvas canvas) {
        Pixmap pixmap = canvas.toPixmap();
        packer.pack(key, pixmap);
        pixmap.dispose();
    }

    @Override
    public void texture(String name, Canvas canvas, int downsample, boolean repeat) {
        Canvas c = downsample > 1 ? canvas.downsample(downsample) : canvas;
        Pixmap pixmap = c.toPixmap();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        if (repeat) texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        textures.put(name, texture);
        owned.add(texture);
    }

    /** Never returns null: unknown names give a visible magenta placeholder. */
    public SpriteSheet sheet(String name) {
        SpriteSheet s = sheets.get(name);
        return s != null ? s : missing;
    }

    public Texture texture(String name) {
        return textures.get(name);
    }

    public TextureRegion region(String name) {
        return sheet(name).frame(0);
    }

    public PaletteLibrary getPalettes() {
        return palettes;
    }

    private SpriteSheet makeMissing() {
        Pixmap p = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        p.setColor(Color.MAGENTA);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        owned.add(t);
        TextureRegion r = new TextureRegion(t);
        return new SpriteSheet(new TextureRegion[] {r}, new TextureRegion[] {r}, 16, 16, 0);
    }

    @Override
    public void dispose() {
        if (atlas != null) atlas.dispose();
        for (Texture t : owned) t.dispose();
        owned.clear();
        textures.clear();
        sheets.clear();
    }
}
