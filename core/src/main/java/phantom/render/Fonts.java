package phantom.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

/**
 * The game's typography: Liberation Serif (SIL Open Font License) rendered at a few sizes.
 * If the font file or FreeType is unavailable it falls back to libGDX's built-in bitmap font.
 * <p>OOP concept: owns disposable resources; exception handling keeps a missing font from crashing.</p>
 */
public final class Fonts implements Disposable {
    private BitmapFont title;
    private BitmapFont large;
    private BitmapFont medium;
    private BitmapFont small;
    private BitmapFont italic;

    public Fonts() {
        try {
            FileHandle regular = find("fonts/LiberationSerif-Regular.ttf");
            FileHandle oblique = find("fonts/LiberationSerif-Italic.ttf");
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(regular);
            title = make(gen, 96, 2.5f);
            large = make(gen, 46, 1.6f);
            medium = make(gen, 28, 1f);
            small = make(gen, 20, 1f);
            gen.dispose();
            FreeTypeFontGenerator genItalic = new FreeTypeFontGenerator(oblique);
            italic = make(genItalic, 26, 1f);
            genItalic.dispose();
        } catch (Throwable t) {
            Gdx.app.error("Fonts", "Falling back to the default bitmap font: " + t.getMessage());
            title = fallback(4.5f);
            large = fallback(2.6f);
            medium = fallback(1.6f);
            small = fallback(1.2f);
            italic = fallback(1.5f);
        }
    }

    private static FileHandle find(String path) {
        FileHandle f = Gdx.files.internal(path);
        if (!f.exists()) f = Gdx.files.classpath(path);
        if (!f.exists()) throw new IllegalStateException("font not found: " + path);
        return f;
    }

    private static BitmapFont make(FreeTypeFontGenerator gen, int size, float spacing) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.color = Color.WHITE;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.spaceX = (int) spacing;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "—…’";
        BitmapFont font = gen.generateFont(p);
        font.setUseIntegerPositions(false);
        return font;
    }

    private static BitmapFont fallback(float scale) {
        BitmapFont f = new BitmapFont();
        f.getData().setScale(scale);
        f.setUseIntegerPositions(false);
        return f;
    }

    public BitmapFont title() { return title; }
    public BitmapFont large() { return large; }
    public BitmapFont medium() { return medium; }
    public BitmapFont small() { return small; }
    public BitmapFont italic() { return italic; }

    @Override
    public void dispose() {
        title.dispose();
        large.dispose();
        medium.dispose();
        small.dispose();
        italic.dispose();
    }
}
