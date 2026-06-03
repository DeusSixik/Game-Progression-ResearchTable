package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Resolves a reusable theme preset for a research node.
 */
public interface ResearchNodeThemeResolver {
    ResearchNodeTheme resolveTheme(ResearchNode node, ResearchNodeRenderContext context);
}
