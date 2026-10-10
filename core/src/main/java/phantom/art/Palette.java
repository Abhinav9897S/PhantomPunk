package phantom.art;

import com.badlogic.gdx.graphics.Color;

/**
 * A limited, desaturated colour set for one region with one glowing accent colour.
 * <p>OOP concept: immutable value object (all colours are copied in and out).</p>
 */
public final class Palette {
    private final Color skyTop;
    private final Color skyBottom;
    private final Color fog;
    private final Color far;
    private final Color mid;
    private final Color near;
    private final Color ground;
    private final Color groundEdge;
    private final Color rim;
    private final Color accent;
    private final Color ambient;

    public Palette(Color skyTop, Color skyBottom, Color fog, Color far, Color mid, Color near,
                   Color ground, Color groundEdge, Color rim, Color accent, Color ambient) {
        this.skyTop = new Color(skyTop);
        this.skyBottom = new Color(skyBottom);
        this.fog = new Color(fog);
        this.far = new Color(far);
        this.mid = new Color(mid);
        this.near = new Color(near);
        this.ground = new Color(ground);
        this.groundEdge = new Color(groundEdge);
        this.rim = new Color(rim);
        this.accent = new Color(accent);
        this.ambient = new Color(ambient);
    }

    public Color skyTop() { return new Color(skyTop); }
    public Color skyBottom() { return new Color(skyBottom); }
    public Color fog() { return new Color(fog); }
    public Color far() { return new Color(far); }
    public Color mid() { return new Color(mid); }
    public Color near() { return new Color(near); }
    public Color ground() { return new Color(ground); }
    public Color groundEdge() { return new Color(groundEdge); }
    public Color rim() { return new Color(rim); }
    public Color accent() { return new Color(accent); }
    public Color ambient() { return new Color(ambient); }

    /** Read-only access without allocation, for per-frame rendering. Do not modify. */
    public Color ambientRef() { return ambient; }
    public Color fogRef() { return fog; }
    public Color skyBottomRef() { return skyBottom; }
    public Color accentRef() { return accent; }
}
