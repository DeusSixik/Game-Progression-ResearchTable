package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

/**
 * Resolves the effective runtime bounds of a {@link ResearchNode}.
 * <p>
 * This is the dedicated geometry/presentation layer for node wrappers. Its job is intentionally
 * narrow: take the logical node plus the current runtime context and mutate the supplied
 * {@link ResearchNodeWrapper} so the visual layer can use custom bounds without touching the
 * underlying graph model.
 * </p>
 *
 * <p><b>Why this exists:</b></p>
 * <ul>
 *     <li>widget factories should focus on composing UI, not deciding graph-space bounds;</li>
 *     <li>camera focus, links, glow and animations also need the same effective rectangle;</li>
 *     <li>some screens may even want auto-layout spacing to react to styled card size.</li>
 * </ul>
 *
 * <p>
 * The resolver mutates the wrapper in place because creating many short-lived wrapper copies adds
 * no value here and the current tree already rebuilds runtime snapshots frequently.
 * </p>
 */
@FunctionalInterface
public interface ResearchNodeWrapperResolver {
    /**
     * Applies runtime bounds adjustments to the supplied wrapper.
     */
    void resolveWrapper(ResearchNode node, ResearchNodeRenderContext context);

    /**
     * No-op resolver that keeps logical and runtime bounds identical.
     */
    static ResearchNodeWrapperResolver identity() {
        return (node, context) -> {
        };
    }
}
