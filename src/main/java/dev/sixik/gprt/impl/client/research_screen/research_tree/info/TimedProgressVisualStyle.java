package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

/**
 * Visual presets for the timed-progress block inside research info panels.
 * <p>
 * These styles are intentionally presentation-only: they do not affect gameplay,
 * timers or progress math. They only change how the existing timed label/bar is
 * emphasized so different UI treatments can be previewed quickly.
 * </p>
 */
public enum TimedProgressVisualStyle {
    /**
     * Default neutral presentation.
     */
    DEFAULT("Default"),

    /**
     * Remaining duration becomes more urgent as time runs out.
     */
    DYNAMIC_TEXT("Dynamic Text"),

    /**
     * Adds a subtle capsule-like background behind the duration line.
     */
    SUBTLE_BACKGROUND("Subtle Background"),

    /**
     * Combines dynamic text color with the subtle background treatment.
     */
    DYNAMIC_TEXT_WITH_BACKGROUND("Dynamic + Background");

    private final String displayName;

    TimedProgressVisualStyle(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public TimedProgressVisualStyle next() {
        TimedProgressVisualStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
