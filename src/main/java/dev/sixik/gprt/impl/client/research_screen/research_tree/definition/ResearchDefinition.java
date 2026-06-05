package dev.sixik.gprt.impl.client.research_screen.research_tree.definition;

import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable metadata of one research entry.
 * <p>
 * This object intentionally stores only the "what is this research" part:
 * id, title, description and study mode. Runtime progression state stays in
 * the progress manager, while the graph node only references this definition.
 * That separation makes it easier to move definitions into a registry later.
 * </p>
 */
public final class ResearchDefinition {
    private final String key;
    private final String title;
    private final String description;
    private final @Nullable Object iconTexture;
    private final ResearchStudyType studyType;
    private final @Nullable ResearchRevealAnimationStyle revealAnimationStyle;
    private final long studyDurationMs;

    public ResearchDefinition(String key,
                              String title,
                              String description,
                              @Nullable Object iconTexture,
                              ResearchStudyType studyType,
                              @Nullable ResearchRevealAnimationStyle revealAnimationStyle,
                              long studyDurationMs
    ) {
        this.key = Objects.requireNonNull(key, "key");
        this.title = title == null || title.isBlank() ? key : title;
        this.description = description;
        this.iconTexture = iconTexture;
        this.studyType = studyType == null ? ResearchStudyType.INSTANT : studyType;
        this.revealAnimationStyle = revealAnimationStyle;
        this.studyDurationMs = Math.max(0L, studyDurationMs);
    }

    public static Builder builder(String key) {
        return new Builder(key);
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public @Nullable Object getIconTexture() {
        return iconTexture;
    }

    public ResearchStudyType getStudyType() {
        return studyType;
    }

    public @Nullable ResearchRevealAnimationStyle getRevealAnimationStyle() {
        return revealAnimationStyle;
    }

    public long getStudyDurationMs() {
        return studyDurationMs;
    }

    public boolean isTimed() {
        return studyType == ResearchStudyType.TIMED;
    }

    public boolean isTable() {
        return studyType == ResearchStudyType.TABLE;
    }

    public boolean isInstant() {
        return studyType == ResearchStudyType.INSTANT;
    }

    public Builder toBuilder() {
        return new Builder(key)
                .title(title)
                .description(description)
                .iconTexture(iconTexture)
                .studyType(studyType)
                .revealAnimationStyle(revealAnimationStyle)
                .studyDurationMs(studyDurationMs);
    }

    /**
     * Fluent builder for immutable {@link ResearchDefinition} objects.
     * <p>
     * This builder describes the static identity of one research entry: title, description and
     * study mode. It intentionally does not contain unlock/studied runtime flags because those
     * belong to progress managers and graph nodes.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #title(String)} - human-readable title;</li>
     *     <li>{@link #description(String)} - long description shown in UI;</li>
     *     <li>{@link #iconTexture(Object)} - optional icon payload later applied to the node widget;</li>
     *     <li>{@link #studyType(ResearchStudyType)} - explicit study mode;</li>
     *     <li>{@link #revealAnimationStyle(ResearchRevealAnimationStyle)} - optional per-node reveal override;</li>
     *     <li>{@link #studyDurationMs(long)} - duration payload for timed mode;</li>
     *     <li>{@link #timed(long)} - convenience helper for timed research;</li>
     *     <li>{@link #build()} - create the immutable definition snapshot.</li>
     * </ul>
     */
    public static final class Builder {
        private final String key;
        private String title;
        private String description;
        private @Nullable Object iconTexture;
        private ResearchStudyType studyType = ResearchStudyType.INSTANT;
        private @Nullable ResearchRevealAnimationStyle revealAnimationStyle;
        private long studyDurationMs;

        private Builder(String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        /**
         * Sets the human-readable title of the research.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the long-form descriptive text used by details panels and tooltips.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Stores an optional icon payload that {@code ResearchNode#setIconTexture(...)} can consume later.
         */
        public Builder iconTexture(@Nullable Object iconTexture) {
            this.iconTexture = iconTexture;
            return this;
        }

        /**
         * Sets the explicit study mode.
         */
        public Builder studyType(ResearchStudyType studyType) {
            if (studyType != null) {
                this.studyType = studyType;
            }
            return this;
        }

        /**
         * Sets an optional reveal-animation override for this definition.
         */
        public Builder revealAnimationStyle(@Nullable ResearchRevealAnimationStyle revealAnimationStyle) {
            this.revealAnimationStyle = revealAnimationStyle;
            return this;
        }

        /**
         * Sets the study duration payload in milliseconds.
         * <p>
         * The value is always clamped to a non-negative number. It matters primarily for
         * {@link ResearchStudyType#TIMED}, but keeping it here also makes serialization simpler.
         * </p>
         */
        public Builder studyDurationMs(long studyDurationMs) {
            this.studyDurationMs = Math.max(0L, studyDurationMs);
            return this;
        }

        /**
         * Convenience helper that switches the definition into timed mode and assigns its duration.
         */
        public Builder timed(long studyDurationMs) {
            this.studyType = ResearchStudyType.TIMED;
            this.studyDurationMs = Math.max(0L, studyDurationMs);
            return this;
        }

        /**
         * Builds the immutable definition snapshot.
         */
        public ResearchDefinition build() {
            return new ResearchDefinition(key, title, description, iconTexture, studyType, revealAnimationStyle, studyDurationMs);
        }
    }
}
