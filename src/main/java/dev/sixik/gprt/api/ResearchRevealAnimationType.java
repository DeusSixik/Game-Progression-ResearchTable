package dev.sixik.gprt.api;

/**
 * Public reveal-animation enum for addon-facing research definitions.
 * <p>
 * This enum lets integrations choose how a research node should appear when it
 * becomes newly available, without depending on internal client classes.
 * </p>
 *
 * <p>
 * The value is optional. When a research does not set it, the screen is free to
 * fall back to group presets or its own default animation rules.
 * </p>
 */
public enum ResearchRevealAnimationType {
    /**
     * Large drop-in with an exaggerated bounce.
     */
    DROP_BOUNCE,

    /**
     * Softer pop-in with a gentler settle.
     */
    SOFT_POP,

    /**
     * Horizontal reveal that enters from the left.
     */
    SLIDE_FROM_LEFT,

    /**
     * Minimal reveal focused on a calm scale settle.
     */
    FADE_SCALE,

    /**
     * Curved drop that approaches from the upper-left.
     */
    ARC_DROP,

    /**
     * Cinematic reveal where the node assembles from two halves.
     */
    SPLIT_FUSE;

    /**
     * Converts the public API enum into the current internal client enum.
     */
    public dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle toInternalType() {
        return switch (this) {
            case DROP_BOUNCE -> dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle.DROP_BOUNCE;
            case SOFT_POP -> dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle.SOFT_POP;
            case SLIDE_FROM_LEFT -> dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle.SLIDE_FROM_LEFT;
            case FADE_SCALE -> dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle.FADE_SCALE;
            case ARC_DROP -> dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle.ARC_DROP;
            case SPLIT_FUSE -> dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle.SPLIT_FUSE;
        };
    }
}
