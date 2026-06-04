package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.effects.FadeScaleRevealEffect;
import dev.sixik.gprt.impl.client.research_screen.research_tree.reveal.effects.SplitFuseRevealEffect;
import org.jetbrains.annotations.Nullable;

/**
 * Default reveal-effect registry used by the research tree.
 */
public final class DefaultResearchRevealEffectResolver implements ResearchRevealEffectResolver {
    private final ResearchRevealEffect splitFuseEffect = new SplitFuseRevealEffect();
    private final ResearchRevealEffect fadeScaleEffect = new FadeScaleRevealEffect();

    @Override
    public @Nullable ResearchRevealEffect resolve(ResearchNode node, ResearchRevealAnimationStyle style) {
        return switch (style) {
            case SPLIT_FUSE -> splitFuseEffect;
            case FADE_SCALE -> fadeScaleEffect;
            default -> null;
        };
    }
}
