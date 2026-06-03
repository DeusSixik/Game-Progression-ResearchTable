package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared callback/context object passed to custom info-panel widgets.
 * <p>
 * A custom panel can keep this object and wire its own buttons or interactive elements
 * without depending on {@code ResearchTreeScreenMainScreen} internals directly.
 * </p>
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #close()} - request closing the panel.</li>
 *     <li>{@link #startResearch()} - request the current node research action.</li>
 *     <li>{@link #jumpToResearch(String)} - request focus on another research by key.</li>
 * </ul>
 */
public final class ResearchInfoPanelContext {
    private final Runnable onClose;
    private final Runnable onResearch;
    private final Consumer<String> onResearchJump;

    public ResearchInfoPanelContext(Runnable onClose, Runnable onResearch, Consumer<String> onResearchJump) {
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        this.onResearch = Objects.requireNonNull(onResearch, "onResearch");
        this.onResearchJump = Objects.requireNonNull(onResearchJump, "onResearchJump");
    }

    public void close() {
        onClose.run();
    }

    public void startResearch() {
        onResearch.run();
    }

    public void jumpToResearch(String researchKey) {
        if (researchKey != null && !researchKey.isBlank()) {
            onResearchJump.accept(researchKey);
        }
    }
}
