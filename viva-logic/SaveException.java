/**
 * Thrown when the save file is missing, unreadable or corrupted.
 *
 * OOP: CUSTOM CHECKED EXCEPTION - extends Exception (not RuntimeException), so the
 *      compiler forces every caller to catch it or declare it with "throws".
 */
public class SaveException extends Exception {
    private static final long serialVersionUID = 1L;

    public SaveException(String message) {
        super(message);
    }

    /** Exception chaining: keeps the original low-level error as the "cause". */
    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
