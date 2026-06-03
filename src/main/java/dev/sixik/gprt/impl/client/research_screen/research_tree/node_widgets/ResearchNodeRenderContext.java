package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable runtime snapshot passed into node widget factories.
 * <p>
 * Unlike {@link ResearchNodeVisualDefinition}, this model carries stateful information that can
 * change every tick or after user interaction: progress, selection and temporary interaction locks.
 * </p>
 */
public final class ResearchNodeRenderContext {
    private final ResearchState state;
    private final boolean visible;
    private final boolean highlighted;
    private final boolean revealLocked;
    private final boolean selected;
    private final boolean hasNewUnlockMarker;
    private final long nowMs;
    private final @Nullable ClientResearchProgress progress;

    private ResearchNodeRenderContext(Builder builder) {
        this.state = builder.state;
        this.visible = builder.visible;
        this.highlighted = builder.highlighted;
        this.revealLocked = builder.revealLocked;
        this.selected = builder.selected;
        this.hasNewUnlockMarker = builder.hasNewUnlockMarker;
        this.nowMs = builder.nowMs;
        this.progress = builder.progress;
    }

    public static Builder builder() {
        return new Builder();
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

    public boolean isRevealLocked() {
        return revealLocked;
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

    public static final class Builder {
        private ResearchState state = ResearchState.LOCKED;
        private boolean visible = true;
        private boolean highlighted;
        private boolean revealLocked;
        private boolean selected;
        private boolean hasNewUnlockMarker;
        private long nowMs;
        private @Nullable ClientResearchProgress progress;

        public Builder state(ResearchState state) {
            if (state != null) {
                this.state = state;
            }
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Builder highlighted(boolean highlighted) {
            this.highlighted = highlighted;
            return this;
        }

        public Builder revealLocked(boolean revealLocked) {
            this.revealLocked = revealLocked;
            return this;
        }

        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public Builder hasNewUnlockMarker(boolean hasNewUnlockMarker) {
            this.hasNewUnlockMarker = hasNewUnlockMarker;
            return this;
        }

        public Builder nowMs(long nowMs) {
            this.nowMs = nowMs;
            return this;
        }

        public Builder progress(@Nullable ClientResearchProgress progress) {
            this.progress = progress;
            return this;
        }

        public ResearchNodeRenderContext build() {
            return new ResearchNodeRenderContext(this);
        }
    }
}
