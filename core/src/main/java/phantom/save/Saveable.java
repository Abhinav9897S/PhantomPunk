package phantom.save;

/**
 * Something that stores part of its state in a {@link SaveData} snapshot and restores it.
 * <p>OOP concept: <b>interface</b> - the save manager can persist inventory, abilities and world
 * progress uniformly without knowing their internals.</p>
 */
public interface Saveable {
    void writeTo(SaveData data);

    void readFrom(SaveData data);
}
