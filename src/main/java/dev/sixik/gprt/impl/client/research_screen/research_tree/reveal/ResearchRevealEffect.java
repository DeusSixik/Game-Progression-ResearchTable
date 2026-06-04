package dev.sixik.gprt.impl.client.research_screen.research_tree.reveal;

/**
 * Factory entry point for one reveal-animation style.
 * <p>
 * Effects are lightweight definitions. Each unlock animation creates its own
 * {@link RevealEffectInstance} so per-run state such as cached fragments, random seeds,
 * temporary targets or timing offsets can live outside the screen itself.
 * </p>
 */
public interface ResearchRevealEffect {
    RevealEffectInstance createInstance(RevealInitContext context);
}
