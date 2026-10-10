package phantom.art;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Developer tool: paints every sprite headlessly and writes contact sheets as PNG files.
 * Run with {@code ./gradlew :core:artPreview -PoutDir=...}.
 */
public final class ArtPreview {
    private ArtPreview() { }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "build/art-preview");
        out.mkdirs();
        PaletteLibrary palettes = new PaletteLibrary();
        List<SpritePainter> painters = List.of(new CharacterPainter(), new CreaturePainter(), new BossPainter(),
                new PropPainter(), new NpcPainter(), new TilePainter(palettes), new BackgroundPainter(palettes), new UiPainter());
        for (SpritePainter p : painters) {
            RecordingRegistry reg = new RecordingRegistry();
            p.paint(reg);
            writeSheet(reg.getSprites(), new File(out, p.getClass().getSimpleName() + ".png"));
            for (Map.Entry<String, Canvas> t : reg.getTextures().entrySet()) {
                write(t.getValue(), new File(out, t.getKey() + ".png"), 0xff202020);
            }
        }
    }

    private static void writeSheet(Map<String, List<Canvas>> sprites, File file) throws Exception {
        if (sprites.isEmpty()) return;
        int pad = 8;
        int width = 0;
        int height = pad;
        for (List<Canvas> frames : sprites.values()) {
            int rowW = pad;
            int rowH = 0;
            for (Canvas c : frames) {
                Canvas d = c.getScale() > 2 ? c.downsample(Math.round(c.getScale() / 2)) : c;
                rowW += d.getPixelWidth() + pad;
                rowH = Math.max(rowH, d.getPixelHeight());
            }
            width = Math.max(width, rowW);
            height += rowH + pad;
        }
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < width * height; i++) img.setRGB(i % width, i / width, 0xff3a4048);
        int y = pad;
        for (List<Canvas> frames : sprites.values()) {
            int x = pad;
            int rowH = 0;
            for (Canvas c : frames) {
                Canvas d = c.getScale() > 2 ? c.downsample(Math.round(c.getScale() / 2)) : c;
                blit(img, d, x, y);
                x += d.getPixelWidth() + pad;
                rowH = Math.max(rowH, d.getPixelHeight());
            }
            y += rowH + pad;
        }
        ImageIO.write(img, "png", file);
    }

    private static void blit(BufferedImage img, Canvas c, int ox, int oy) {
        int[] px = c.toArgb();
        for (int j = 0; j < c.getPixelHeight(); j++) {
            for (int i = 0; i < c.getPixelWidth(); i++) {
                int src = px[j * c.getPixelWidth() + i];
                int dst = img.getRGB(ox + i, oy + j);
                img.setRGB(ox + i, oy + j, over(src, dst));
            }
        }
    }

    private static void write(Canvas c, File file, int background) throws Exception {
        BufferedImage img = new BufferedImage(c.getPixelWidth(), c.getPixelHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < c.getPixelWidth() * c.getPixelHeight(); i++) img.setRGB(i % c.getPixelWidth(), i / c.getPixelWidth(), background);
        blit(img, c, 0, 0);
        ImageIO.write(img, "png", file);
    }

    private static int over(int src, int dst) {
        float a = (src >>> 24) / 255f;
        int r = (int) (((src >> 16) & 255) * a + ((dst >> 16) & 255) * (1 - a));
        int g = (int) (((src >> 8) & 255) * a + ((dst >> 8) & 255) * (1 - a));
        int b = (int) ((src & 255) * a + (dst & 255) * (1 - a));
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}
