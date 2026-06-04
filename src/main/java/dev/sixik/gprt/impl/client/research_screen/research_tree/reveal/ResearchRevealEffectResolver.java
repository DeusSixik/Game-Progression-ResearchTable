package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves which reveal effect should be used for a given node/style pair.
 * <p>
 * Returning {@code null} means the style is handled by the regular live-widget transform path and
 * does not need a detached snapshot-based reveal layer.
 * </p>
 */
public interface ResearchRevealEffectResolver {
    @Nullable ResearchRevealEffect resolve(ResearchNode node, ResearchRevealAnimationStyle style);
}
