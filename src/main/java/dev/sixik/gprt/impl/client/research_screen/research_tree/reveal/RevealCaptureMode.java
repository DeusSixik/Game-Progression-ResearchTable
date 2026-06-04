package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

/**
 * Controls which snapshot source the reveal system should use.
 * <p>
 * This is primarily useful for debugging capture issues:
 * screen capture preserves the final composed result but can accidentally include nearby widgets,
 * while offscreen capture isolates the widget but can expose render-path issues in custom UI trees.
 * </p>
 */
public enum RevealCaptureMode {
    AUTO_OFFSCREEN_FIRST("Auto: Offscreen"),
    FORCE_OFFSCREEN("Force: Offscreen"),
    FORCE_SCREEN("Force: Screen");

    private final String debugLabel;

    RevealCaptureMode(String debugLabel) {
        this.debugLabel = debugLabel;
    }

    public String debugLabel() {
        return debugLabel;
    }

    public RevealCaptureMode next() {
        return switch (this) {
            case AUTO_OFFSCREEN_FIRST -> FORCE_OFFSCREEN;
            case FORCE_OFFSCREEN -> FORCE_SCREEN;
            case FORCE_SCREEN -> AUTO_OFFSCREEN_FIRST;
        };
    }
}
