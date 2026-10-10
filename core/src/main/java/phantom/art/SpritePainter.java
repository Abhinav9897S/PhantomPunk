package phantom.art;

/**
 * A family of procedurally painted sprites (characters, creatures, tiles...).
 * <p>OOP concept: <b>interface + polymorphism</b> - the factory runs a list of painters
 * without knowing what each one draws.</p>
 */
public interface SpritePainter {
    void paint(SpriteRegistry registry);
}
