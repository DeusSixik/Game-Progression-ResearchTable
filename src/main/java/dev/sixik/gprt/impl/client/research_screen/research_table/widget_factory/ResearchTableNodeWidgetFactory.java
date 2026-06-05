package dev.sixik.gprt.impl.client.research_screen.research_table.widget_factory;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeRenderContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWrapper;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

public class ResearchTableNodeWidgetFactory implements ResearchNodeWidgetFactory {

    @Override
    public UIElement createNodeWidget(ResearchNode node, ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition, Runnable onClick) {
        NodeWidget nodeWidget = new NodeWidget(onClick);
        nodeWidget.apply(node, context, visualDefinition);
        return nodeWidget;
    }

    @Override
    public void updateNodeWidget(UIElement widget, ResearchNode node, ResearchNodeRenderContext context, ResearchNodeVisualDefinition visualDefinition) {
        if (widget instanceof NodeWidget showcaseNodeWidget) {
            showcaseNodeWidget.apply(node, context, visualDefinition);
        }
    }

    public static class NodeWidget extends Button {
        
        protected NodeWidget(Runnable onClick) {
            setOnClick((event) -> onClick.run());
        }

        protected void apply(ResearchNode node,
                           ResearchNodeRenderContext context,
                           ResearchNodeVisualDefinition visualDefinition
        ) {
            ResearchNodeWrapper wrapper = context.getNodeWrapper();
            setDisplay(context.isVisible());
            setActive(context.isVisible() && !context.isInteractionLocked());

            wrapper.setSize(wrapper.getWidth() + 100, wrapper.getHeight());

            float width = wrapper.getWidth();
            float height = wrapper.getHeight();
            /*
            DebugResearchNodeWidgetShowcase.StyleMode styleMode = styleModeSupplier.get();
            DebugResearchNodeWidgetShowcase.LayoutVariant variant = resolveVariant(node, styleMode);

            applyButtonFrame(context, visualDefinition, styleMode);
            applyDecor(width, height, variant, visualDefinition, styleMode);
            applyIcon(node, width, height, variant, visualDefinition, styleMode);
            applyTitle(node, context, width, height, variant, visualDefinition, styleMode);
            applyBadge(width, height, variant, visualDefinition, styleMode);
            applyProgress(node, context, width, height, visualDefinition, styleMode);
            applyRevealAccent(context);
            */
        }
    }
}
