package phantom.save;

/**
 * Raised when a save file is missing, unreadable, corrupted or from an unsupported version.
 * <p>OOP concept: <b>custom checked exception</b> - callers must decide how to recover.</p>
 */
public class SaveException extends Exception {
    private static final long serialVersionUID = 1L;

    public SaveException(String message) {
        super(message);
    }

    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
