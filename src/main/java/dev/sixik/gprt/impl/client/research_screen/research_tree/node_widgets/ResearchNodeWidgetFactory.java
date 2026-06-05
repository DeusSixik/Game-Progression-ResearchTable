package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Creates and refreshes UI widgets used to render research nodes inside the graph.
 * <p>
 * This is the last step of the node rendering pipeline:
 * </p>
 * <ol>
 *     <li>{@code ResearchNode} supplies logical research data.</li>
 *     <li>{@link ResearchNodeRenderContext} supplies volatile runtime state and the effective
 *     runtime wrapper/bounds.</li>
 *     <li>{@link ResearchNodeVisualDefinition} supplies the resolved visual style.</li>
 *     <li>{@link ResearchNodeWidgetFactory} turns those inputs into actual {@link UIElement}s.</li>
 * </ol>
 *
 * <p>
 * The interface is split into create/update on purpose. The graph owns node lifetime, while the
 * factory can now also influence the effective runtime size/placement through the wrapper stored
 * in {@link ResearchNodeRenderContext}. This makes style-specific cards possible without forcing
 * those visual bounds back into the logical node model.
 * </p>
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #prepareNodeWrapper(ResearchNode, ResearchNodeRenderContext)} -
 *     optional runtime geometry adjustment before visuals are resolved;</li>
 *     <li>{@link #createNodeWidget(ResearchNode, ResearchNodeRenderContext, ResearchNodeVisualDefinition, Runnable)} -
 *     initial widget assembly;</li>
 *     <li>{@link #updateNodeWidget(UIElement, ResearchNode, ResearchNodeRenderContext, ResearchNodeVisualDefinition)} -
 *     in-place refresh for state/style changes.</li>
 * </ul>
 */
public interface ResearchNodeWidgetFactory {
    /**
     * Allows the factory to adjust the effective runtime bounds before visuals are resolved.
     * <p>
     * This is the hook that lets a style say "my card should be wider/taller/shifted a bit"
     * while keeping {@link ResearchNode} itself unchanged. The wrapper inside the supplied
     * context is mutable on purpose.
     * </p>
     *
     * <p>
     * Default implementation does nothing.
     * </p>
     */
    default void prepareNodeWrapper(ResearchNode node, ResearchNodeRenderContext context) {
    }

    /**
     * Creates a brand-new widget for one logical research node.
     * <p>
     * The returned widget should already reflect the provided context and visual definition.
     * The outer graph screen will position it on the canvas; the factory is responsible only for
     * the widget's internal composition and the supplied click callback.
     * </p>
     */
    UIElement createNodeWidget(ResearchNode node,
                               ResearchNodeRenderContext context,
                               ResearchNodeVisualDefinition visualDefinition,
                               Runnable onClick);

    /**
     * Reapplies state and visuals to an existing widget instance.
     * <p>
     * Implementations should prefer updating child elements in place instead of rebuilding the
     * entire widget tree. This method may be called frequently because selection, timed progress,
     * group highlighting and unlock state can all change while the node remains on screen.
     * </p>
     */
    void updateNodeWidget(UIElement widget,
                          ResearchNode node,
                          ResearchNodeRenderContext context,
                          ResearchNodeVisualDefinition visualDefinition);
}
