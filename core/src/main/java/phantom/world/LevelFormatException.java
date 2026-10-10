package phantom.world;

/**
 * Thrown when a room file is malformed (bad JSON, ragged rows, unknown symbols, broken doors).
 * <p>OOP concept: <b>custom checked exception</b> carrying the file name for clear error messages.</p>
 */
public class LevelFormatException extends Exception {
    private static final long serialVersionUID = 1L;

    public LevelFormatException(String source, String message) {
        super(source + ": " + message);
    }

    public LevelFormatException(String source, String message, Throwable cause) {
        super(source + ": " + message, cause);
    }
}
