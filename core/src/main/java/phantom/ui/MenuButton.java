package phantom.ui;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * One menu entry: a (possibly dynamic) label, an action, an optional left/right adjuster.
 * <p>OOP concept: composition of functional interfaces (lambdas) instead of a subclass per button.</p>
 */
public final class MenuButton {
    private final Supplier<String> label;
    private final Runnable action;
    private final BooleanSupplier enabled;
    private final IntConsumer adjust;

    public MenuButton(String label, Runnable action) {
        this(() -> label, action, () -> true, null);
    }

    public MenuButton(Supplier<String> label, Runnable action, BooleanSupplier enabled, IntConsumer adjust) {
        this.label = label;
        this.action = action;
        this.enabled = enabled;
        this.adjust = adjust;
    }

    public String getLabel() { return label.get(); }
    public boolean isEnabled() { return enabled.getAsBoolean(); }
    public void press() { if (isEnabled() && action != null) action.run(); }
    public boolean canAdjust() { return adjust != null; }
    public void adjust(int dir) { if (adjust != null) adjust.accept(dir); }
}
