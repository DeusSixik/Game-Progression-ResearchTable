package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Default theme resolver that derives the base node theme from the node group and study type.
 */
public final class DefaultResearchNodeThemeResolver implements ResearchNodeThemeResolver {
    @Override
    public ResearchNodeTheme resolveTheme(ResearchNode node, ResearchNodeRenderContext context) {
        return ResearchNodeThemes.fromNode(node);
    }
}
