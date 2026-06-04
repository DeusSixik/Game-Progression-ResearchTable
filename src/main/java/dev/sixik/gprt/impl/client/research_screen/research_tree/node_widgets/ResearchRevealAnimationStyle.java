package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

/**
 * High-level style preset for how a research node should appear during the
 * unlock/reveal animation sequence.
 */
public enum ResearchRevealAnimationStyle {
    /**
     * Large drop-in with an exaggerated bounce, suited for heavier or more
     * dramatic branches.
     */
    DROP_BOUNCE,

    /**
     * Softer pop-in that settles more gently, suited for lighter card styles.
     */
    SOFT_POP,

    /**
     * Horizontal reveal that enters from the left and settles into place with
     * a quick deceleration.
     */
    SLIDE_FROM_LEFT,

    /**
     * Minimal reveal that mostly relies on scale settling, suited for calmer
     * or more precise UI themes.
     */
    FADE_SCALE,

    /**
     * Curved drop that approaches from the upper-left and lands with a smooth
     * arc before connecting its links.
     */
    ARC_DROP,

    /**
     * Cinematic reveal where the node is assembled from two halves.
     * <p>
     * The first implementation may use a simplified custom reveal layer, while
     * future versions are expected to switch to snapshot/FBO-based rendering.
     * </p>
     */
    SPLIT_FUSE
}
