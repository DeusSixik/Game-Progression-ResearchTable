package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable runtime snapshot passed into node widget factories.
 * <p>
 * Unlike {@link ResearchNodeVisualDefinition}, this model carries stateful information that can
 * change every tick or after user interaction: progress, selection and temporary interaction locks.
 * Think of it as the answer to the question "what is happening to this node right now?".
 * </p>
 *
 * <p><b>Important distinction:</b></p>
 * <ul>
 *     <li>{@link ResearchNodeVisualDefinition} = how the node should look.</li>
 *     <li>{@link ResearchNodeRenderContext} = what runtime conditions the widget should react to.</li>
 * </ul>
 *
 * <p>
 * For example, a node can keep the same theme/icon/colors while still becoming selected,
 * highlighted, temporarily non-clickable during unlock cinematics, or updated with live timed
 * progress. Those volatile values belong here, not in the visual definition.
 * </p>
 */
public final class ResearchNodeRenderContext {
    private final ResearchNodeWrapper nodeWrapper;
    private final ResearchState state;
    private final boolean visible;
    private final boolean highlighted;
    private final boolean interactionLocked;
    private final boolean selected;
    private final boolean hasNewUnlockMarker;
    private final long nowMs;
    private final @Nullable ClientResearchProgress progress;
    private final boolean unlockAnimating;
    private final float unlockNodeProgress01;
    private final float unlockLinkProgress01;
    private final float unlockCurrentScale;
    private final float unlockCurrentTranslateX;
    private final float unlockCurrentTranslateY;
    private final ResearchRevealAnimationStyle unlockRevealAnimationStyle;

    private ResearchNodeRenderContext(Builder builder) {
        this.nodeWrapper = builder.nodeWrapper;
        this.state = builder.state;
        this.visible = builder.visible;
        this.highlighted = builder.highlighted;
        this.interactionLocked = builder.interactionLocked;
        this.selected = builder.selected;
        this.hasNewUnlockMarker = builder.hasNewUnlockMarker;
        this.nowMs = builder.nowMs;
        this.progress = builder.progress;
        this.unlockAnimating = builder.unlockAnimating;
        this.unlockNodeProgress01 = builder.unlockNodeProgress01;
        this.unlockLinkProgress01 = builder.unlockLinkProgress01;
        this.unlockCurrentScale = builder.unlockCurrentScale;
        this.unlockCurrentTranslateX = builder.unlockCurrentTranslateX;
        this.unlockCurrentTranslateY = builder.unlockCurrentTranslateY;
        this.unlockRevealAnimationStyle = builder.unlockRevealAnimationStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the effective runtime bounds used by the current visual/widget layer.
     * <p>
     * This wrapper starts from the logical {@code ResearchNode} bounds, but widget factories or
     * screen-level styling code may expand or shift it to fit a custom card style without changing
     * the graph model itself.
     * </p>
     */
    public ResearchNodeWrapper getNodeWrapper() {
        return nodeWrapper;
    }

    public ResearchState getState() {
        return state;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Returns whether the node should currently reject user interaction.
     * <p>
     * This is primarily an input/runtime lock, not a visual state by itself. Widget factories are
     * expected to use it to disable clicks/hover actions. They may also add custom visuals if the
     * design wants that, but the flag does not imply dimming by default.
     * </p>
     *
     * <p>
     * In the current tree this flag is usually driven by unlock cinematics, so the player cannot
     * click nodes while the camera and unlock animation are still running.
     * </p>
     */
    public boolean isInteractionLocked() {
        return interactionLocked;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean hasNewUnlockMarker() {
        return hasNewUnlockMarker;
    }

    public long getNowMs() {
        return nowMs;
    }

    public @Nullable ClientResearchProgress getProgress() {
        return progress;
    }

    public boolean isUnlockAnimating() {
        return unlockAnimating;
    }

    public float getUnlockNodeProgress01() {
        return unlockNodeProgress01;
    }

    public float getUnlockLinkProgress01() {
        return unlockLinkProgress01;
    }

    public float getUnlockCurrentScale() {
        return unlockCurrentScale;
    }

    public float getUnlockCurrentTranslateX() {
        return unlockCurrentTranslateX;
    }

    public float getUnlockCurrentTranslateY() {
        return unlockCurrentTranslateY;
    }

    public ResearchRevealAnimationStyle getUnlockRevealAnimationStyle() {
        return unlockRevealAnimationStyle;
    }

    /**
     * Fluent builder for {@link ResearchNodeRenderContext}.
     * <p>
     * This builder is intended for the runtime/screen layer. A screen collects the current node
     * state, visibility, highlighting and progress snapshot, then hands the built context to a
     * {@link ResearchNodeWidgetFactory}.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #state(ResearchState)} - progression state;</li>
     *     <li>{@link #visible(boolean)}, {@link #highlighted(boolean)}, {@link #selected(boolean)} -
     *     graph/UI state;</li>
     *     <li>{@link #interactionLocked(boolean)} - temporary input lock during animations;</li>
     *     <li>{@link #hasNewUnlockMarker(boolean)} - "new" badge state;</li>
     *     <li>{@link #nowMs(long)}, {@link #progress(ClientResearchProgress)} - timed progress context;</li>
     *     <li>{@link #build()} - immutable runtime snapshot.</li>
     * </ul>
     */
    public static final class Builder {
        private ResearchNodeWrapper nodeWrapper;
        private ResearchState state = ResearchState.LOCKED;
        private boolean visible = true;
        private boolean highlighted;
        private boolean interactionLocked;
        private boolean selected;
        private boolean hasNewUnlockMarker;
        private long nowMs;
        private @Nullable ClientResearchProgress progress;
        private boolean unlockAnimating;
        private float unlockNodeProgress01;
        private float unlockLinkProgress01;
        private float unlockCurrentScale = 1f;
        private float unlockCurrentTranslateX;
        private float unlockCurrentTranslateY;
        private ResearchRevealAnimationStyle unlockRevealAnimationStyle = ResearchRevealAnimationStyle.DROP_BOUNCE;

        /**
         * Supplies the mutable runtime wrapper that carries effective node bounds for rendering.
         */
        public Builder nodeWrapper(ResearchNodeWrapper nodeWrapper) {
            this.nodeWrapper = nodeWrapper;
            return this;
        }

        /**
         * Sets the logical progression state of the node.
         */
        public Builder state(ResearchState state) {
            if (state != null) {
                this.state = state;
            }
            return this;
        }

        /**
         * Controls whether the node widget should currently be rendered at all.
         */
        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        /**
         * Marks the node as belonging to the currently highlighted/focused branch.
         */
        public Builder highlighted(boolean highlighted) {
            this.highlighted = highlighted;
            return this;
        }

        /**
         * Marks the node as temporarily non-interactable.
         * <p>
         * Typical example: an unlock animation is active, so the node should ignore clicks until
         * the sequence has finished.
         * </p>
         */
        public Builder interactionLocked(boolean interactionLocked) {
            this.interactionLocked = interactionLocked;
            return this;
        }

        /**
         * Marks the node as the one currently selected by the details/info panel.
         */
        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        /**
         * Marks the node as freshly unlocked/new so the visual layer can show a badge like `NEW`.
         */
        public Builder hasNewUnlockMarker(boolean hasNewUnlockMarker) {
            this.hasNewUnlockMarker = hasNewUnlockMarker;
            return this;
        }

        /**
         * Supplies the current timestamp used for live timed-progress calculations.
         */
        public Builder nowMs(long nowMs) {
            this.nowMs = nowMs;
            return this;
        }

        /**
         * Supplies the current client progress snapshot for timed research, if one exists.
         */
        public Builder progress(@Nullable ClientResearchProgress progress) {
            this.progress = progress;
            return this;
        }

        public Builder unlockAnimating(boolean unlockAnimating) {
            this.unlockAnimating = unlockAnimating;
            return this;
        }

        public Builder unlockNodeProgress01(float unlockNodeProgress01) {
            this.unlockNodeProgress01 = unlockNodeProgress01;
            return this;
        }

        public Builder unlockLinkProgress01(float unlockLinkProgress01) {
            this.unlockLinkProgress01 = unlockLinkProgress01;
            return this;
        }

        public Builder unlockCurrentScale(float unlockCurrentScale) {
            this.unlockCurrentScale = unlockCurrentScale;
            return this;
        }

        public Builder unlockCurrentTranslateX(float unlockCurrentTranslateX) {
            this.unlockCurrentTranslateX = unlockCurrentTranslateX;
            return this;
        }

        public Builder unlockCurrentTranslateY(float unlockCurrentTranslateY) {
            this.unlockCurrentTranslateY = unlockCurrentTranslateY;
            return this;
        }

        public Builder unlockRevealAnimationStyle(ResearchRevealAnimationStyle unlockRevealAnimationStyle) {
            if (unlockRevealAnimationStyle != null) {
                this.unlockRevealAnimationStyle = unlockRevealAnimationStyle;
            }
            return this;
        }

        /**
         * Builds the immutable runtime context snapshot.
         */
        public ResearchNodeRenderContext build() {
            if (nodeWrapper == null) {
                throw new IllegalStateException("ResearchNodeRenderContext requires a nodeWrapper");
            }
            return new ResearchNodeRenderContext(this);
        }
    }
}
