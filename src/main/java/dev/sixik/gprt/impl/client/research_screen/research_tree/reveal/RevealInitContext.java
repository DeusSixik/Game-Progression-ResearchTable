package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable data bundle used when a new reveal-animation instance is created.
 */
public final class RevealInitContext {
    private final ResearchNode node;
    private final UIElement widget;
    private final RevealSnapshot snapshot;
    private final @Nullable ResearchNodeVisualDefinition visualDefinition;
    private final RevealRenderBackend backend;

    public RevealInitContext(ResearchNode node,
                             UIElement widget,
                             RevealSnapshot snapshot,
                             @Nullable ResearchNodeVisualDefinition visualDefinition,
                             RevealRenderBackend backend
    ) {
        this.node = node;
        this.widget = widget;
        this.snapshot = snapshot;
        this.visualDefinition = visualDefinition;
        this.backend = backend;
    }

    public ResearchNode node() {
        return node;
    }

    public UIElement widget() {
        return widget;
    }

    public RevealSnapshot snapshot() {
        return snapshot;
    }

    public @Nullable ResearchNodeVisualDefinition visualDefinition() {
        return visualDefinition;
    }

    public RevealRenderBackend backend() {
        return backend;
    }
}
