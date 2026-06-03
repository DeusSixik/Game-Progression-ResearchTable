package dev.sixik.gprt.api;

import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;

import java.util.Objects;

/**
 * Public immutable group definition used by addon and script integrations.
 * <p>
 * A group describes one logical branch/category of researches and exposes the
 * colors that UI code can use for nodes, links and category accents.
 * </p>
 *
 * <p><b>Typical usage:</b></p>
 * <pre>{@code
 * ResearchGroupDefinition metallurgy = Researches.group("metallurgy")
 *         .title("Metallurgy")
 *         .primaryColor(0xFFD49A3A)
 *         .secondaryColor(0xFFFFD37A)
 *         .build();
 * }</pre>
 */
public final class ResearchGroupDefinition {
    private final String id;
    private final String title;
    private final int primaryColor;
    private final int secondaryColor;

    private ResearchGroupDefinition(String id, String title, int primaryColor, int secondaryColor) {
        this.id = normalizeId(id);
        this.title = title == null || title.isBlank() ? this.id : title;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
    }

    /**
     * Creates a new builder for one logical research group.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Alias for script/addon-style usage.
     */
    public static Builder register(String id) {
        return builder(id);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    /**
     * Converts this public API group into the current internal runtime group model.
     */
    public ResearchGroup toInternalGroup() {
        return ResearchGroup.of(id, title, primaryColor, secondaryColor);
    }

    private static String normalizeId(String id) {
        String trimmed = Objects.requireNonNull(id, "id").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Research group id cannot be blank");
        }
        return trimmed;
    }

    /**
     * Fluent builder for addon-facing research group declarations.
     */
    public static final class Builder {
        private final String id;
        private String title;
        private int primaryColor = ResearchGroup.DEFAULT.getPrimaryColor();
        private int secondaryColor = ResearchGroup.DEFAULT.getSecondaryColor();

        private Builder(String id) {
            this.id = normalizeId(id);
        }

        /**
         * Sets the user-facing title of the group/category.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets both primary and secondary colors to the same value.
         */
        public Builder color(int color) {
            this.primaryColor = color;
            this.secondaryColor = color;
            return this;
        }

        /**
         * Sets the main color used by this group.
         */
        public Builder primaryColor(int primaryColor) {
            this.primaryColor = primaryColor;
            return this;
        }

        /**
         * Sets the accent/secondary color used by this group.
         */
        public Builder secondaryColor(int secondaryColor) {
            this.secondaryColor = secondaryColor;
            return this;
        }

        /**
         * Builds the immutable public group definition.
         */
        public ResearchGroupDefinition build() {
            return new ResearchGroupDefinition(id, title, primaryColor, secondaryColor);
        }
    }
}
