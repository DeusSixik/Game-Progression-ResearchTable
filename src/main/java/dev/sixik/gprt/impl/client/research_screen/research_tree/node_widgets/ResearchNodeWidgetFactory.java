package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Creates and refreshes UI widgets used to render research nodes inside the graph.
 */
public interface ResearchNodeWidgetFactory {
    UIElement createNodeWidget(ResearchNode node,
                               ResearchNodeRenderContext context,
                               ResearchNodeVisualDefinition visualDefinition,
                               Runnable onClick);

    void updateNodeWidget(UIElement widget,
                          ResearchNode node,
                          ResearchNodeRenderContext context,
                          ResearchNodeVisualDefinition visualDefinition);
}
