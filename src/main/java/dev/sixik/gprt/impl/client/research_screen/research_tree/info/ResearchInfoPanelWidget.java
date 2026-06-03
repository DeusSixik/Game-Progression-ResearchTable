package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

/**
 * Base widget contract for the research details/info panel.
 * <p>
 * This layer exists so screens can replace the default panel widget with a fully custom one
 * without changing {@code ResearchTreeScreenMainScreen} internals. A custom implementation only
 * needs to render {@link ResearchInfoContent} and support the open/close slide progress.
 * </p>
 *
 * <p><b>Typical workflow:</b></p>
 * <ol>
 *     <li>Create your own widget by extending this class.</li>
 *     <li>Use {@link ResearchInfoPanelContext} to wire close / research / jump actions.</li>
 *     <li>Override {@code createInfoPanelWidget(...)} in the screen and return your widget.</li>
 * </ol>
 *
 * <p><b>Animation hooks:</b></p>
 * <ul>
 *     <li>{@link #onOpenAnimationStart()} - panel begins opening.</li>
 *     <li>{@link #onOpenAnimationEnd()} - panel fully opened.</li>
 *     <li>{@link #onCloseAnimationStart()} - panel begins closing.</li>
 *     <li>{@link #onCloseAnimationEnd()} - panel fully closed.</li>
 * </ul>
 */
public abstract class ResearchInfoPanelWidget extends UIElement {
    private final ResearchInfoPanelContext context;

    protected ResearchInfoPanelWidget(ResearchInfoPanelContext context) {
        this.context = context;
    }

    protected final ResearchInfoPanelContext context() {
        return context;
    }

    /**
     * Applies a new content snapshot to the panel.
     */
    public abstract void applyContent(ResearchInfoContent content);

    /**
     * Updates the panel open/close animation progress in range {@code [0..1]}.
     */
    public abstract void setSlideProgress(float progress01);

    /**
     * Called exactly when the screen starts the opening animation.
     * <p>
     * Override this when a custom panel needs to start secondary effects such as
     * label fades, sound effects or temporary interaction locks.
     * </p>
     */
    public void onOpenAnimationStart() {
    }

    /**
     * Called once the panel reaches the fully opened state.
     */
    public void onOpenAnimationEnd() {
    }

    /**
     * Called exactly when the screen starts the closing animation.
     */
    public void onCloseAnimationStart() {
    }

    /**
     * Called once the panel reaches the fully closed state.
     */
    public void onCloseAnimationEnd() {
    }
}
