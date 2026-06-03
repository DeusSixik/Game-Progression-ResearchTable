package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

import org.jetbrains.annotations.Nullable;

/**
 * Reusable semantic theme preset for one research-node family.
 * <p>
 * Unlike {@link ResearchNodeVisualDefinition}, this model is not the final rendered style.
 * It stores higher-level defaults such as branch colors, icon, shape and optional preset badge.
 * A visual resolver can then combine this theme with runtime state and produce the final node look.
 * </p>
 */
public final class ResearchNodeTheme {
    private final boolean titleVisible;
    private final boolean subtitleVisible;
    private final boolean iconVisible;
    private final boolean progressVisible;
    private final boolean titleTextShadow;
    private final boolean subtitleTextShadow;
    private final boolean badgeTextShadow;
    private final int primaryColor;
    private final int secondaryColor;
    private final @Nullable Integer accentColor;
    private final @Nullable Integer badgeColor;
    private final @Nullable Integer progressBarColor;
    private final @Nullable Integer progressBarBackgroundColor;
    private final String iconPath;
    private final String badgeText;
    private final ResearchRevealAnimationStyle revealAnimationStyle;
    private final ResearchNodeVisualDefinition.ShapeStyle shapeStyle;
    private final @Nullable ResearchNodeVisualDefinition.SizePreset sizePreset;
    private final @Nullable ResearchNodeVisualDefinition.TitleAlignment titleAlignment;

    private ResearchNodeTheme(Builder builder) {
        this.titleVisible = builder.titleVisible;
        this.subtitleVisible = builder.subtitleVisible;
        this.iconVisible = builder.iconVisible;
        this.progressVisible = builder.progressVisible;
        this.titleTextShadow = builder.titleTextShadow;
        this.subtitleTextShadow = builder.subtitleTextShadow;
        this.badgeTextShadow = builder.badgeTextShadow;
        this.primaryColor = builder.primaryColor;
        this.secondaryColor = builder.secondaryColor;
        this.accentColor = builder.accentColor;
        this.badgeColor = builder.badgeColor;
        this.progressBarColor = builder.progressBarColor;
        this.progressBarBackgroundColor = builder.progressBarBackgroundColor;
        this.iconPath = builder.iconPath;
        this.badgeText = builder.badgeText;
        this.revealAnimationStyle = builder.revealAnimationStyle;
        this.shapeStyle = builder.shapeStyle;
        this.sizePreset = builder.sizePreset;
        this.titleAlignment = builder.titleAlignment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .titleVisible(titleVisible)
                .subtitleVisible(subtitleVisible)
                .iconVisible(iconVisible)
                .progressVisible(progressVisible)
                .titleTextShadow(titleTextShadow)
                .subtitleTextShadow(subtitleTextShadow)
                .badgeTextShadow(badgeTextShadow)
                .primaryColor(primaryColor)
                .secondaryColor(secondaryColor)
                .accentColor(accentColor)
                .badgeColor(badgeColor)
                .progressBarColor(progressBarColor)
                .progressBarBackgroundColor(progressBarBackgroundColor)
                .iconPath(iconPath)
                .badgeText(badgeText)
                .revealAnimationStyle(revealAnimationStyle)
                .shapeStyle(shapeStyle)
                .sizePreset(sizePreset)
                .titleAlignment(titleAlignment);
    }

    public boolean isTitleVisible() {
        return titleVisible;
    }

    public boolean isSubtitleVisible() {
        return subtitleVisible;
    }

    public boolean isIconVisible() {
        return iconVisible;
    }

    public boolean isProgressVisible() {
        return progressVisible;
    }

    public boolean hasTitleTextShadow() {
        return titleTextShadow;
    }

    public boolean hasSubtitleTextShadow() {
        return subtitleTextShadow;
    }

    public boolean hasBadgeTextShadow() {
        return badgeTextShadow;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public @Nullable Integer getAccentColor() {
        return accentColor;
    }

    public @Nullable Integer getBadgeColor() {
        return badgeColor;
    }

    public @Nullable Integer getProgressBarColor() {
        return progressBarColor;
    }

    public @Nullable Integer getProgressBarBackgroundColor() {
        return progressBarBackgroundColor;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public ResearchRevealAnimationStyle getRevealAnimationStyle() {
        return revealAnimationStyle;
    }

    public ResearchNodeVisualDefinition.ShapeStyle getShapeStyle() {
        return shapeStyle;
    }

    public @Nullable ResearchNodeVisualDefinition.SizePreset getSizePreset() {
        return sizePreset;
    }

    public @Nullable ResearchNodeVisualDefinition.TitleAlignment getTitleAlignment() {
        return titleAlignment;
    }

    /**
     * Fluent builder for {@link ResearchNodeTheme}.
     * <p>
     * A theme is a reusable semantic preset, not the final widget paint result. You normally use
     * this builder to define one family style per branch/group/type of research and then let
     * {@link ResearchNodeVisualResolver} combine that preset with runtime state.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #titleVisible(boolean)}, {@link #subtitleVisible(boolean)}, {@link #iconVisible(boolean)} -
     *     content visibility defaults;</li>
     *     <li>{@link #textShadow(boolean)}, {@link #titleTextShadow(boolean)},
     *     {@link #subtitleTextShadow(boolean)}, {@link #badgeTextShadow(boolean)} -
     *     text-shadow controls;</li>
     *     <li>{@link #primaryColor(int)}, {@link #secondaryColor(int)}, {@link #accentColor(Integer)} -
     *     semantic palette;</li>
     *     <li>{@link #iconPath(String)}, {@link #badgeText(String)}, {@link #badge(String, int)} -
     *     reusable decorations;</li>
     *     <li>{@link #progressBarColor(Integer)}, {@link #progressBarBackgroundColor(Integer)} - timed style overrides;</li>
     *     <li>{@link #shapeStyle(ResearchNodeVisualDefinition.ShapeStyle)}, {@link #sizePreset(ResearchNodeVisualDefinition.SizePreset)},
     *     {@link #titleAlignment(ResearchNodeVisualDefinition.TitleAlignment)} - layout hints;</li>
     *     <li>{@link #build()} - final immutable theme preset.</li>
     * </ul>
     */
    public static final class Builder {
        private boolean titleVisible = true;
        private boolean subtitleVisible = true;
        private boolean iconVisible;
        private boolean progressVisible = true;
        private boolean titleTextShadow = true;
        private boolean subtitleTextShadow = true;
        private boolean badgeTextShadow = true;
        private int primaryColor = 0xFF67B7FF;
        private int secondaryColor = 0xFF67B7FF;
        private @Nullable Integer accentColor;
        private @Nullable Integer badgeColor;
        private @Nullable Integer progressBarColor;
        private @Nullable Integer progressBarBackgroundColor;
        private String iconPath = "";
        private String badgeText = "";
        private ResearchRevealAnimationStyle revealAnimationStyle = ResearchRevealAnimationStyle.DROP_BOUNCE;
        private ResearchNodeVisualDefinition.ShapeStyle shapeStyle = ResearchNodeVisualDefinition.ShapeStyle.ROUNDED_RECTANGLE;
        private @Nullable ResearchNodeVisualDefinition.SizePreset sizePreset;
        private @Nullable ResearchNodeVisualDefinition.TitleAlignment titleAlignment;

        /**
         * Controls whether titles are normally visible for this theme family.
         */
        public Builder titleVisible(boolean titleVisible) {
            this.titleVisible = titleVisible;
            return this;
        }

        public Builder showTitle(boolean titleVisible) {
            return titleVisible(titleVisible);
        }

        /**
         * Controls whether subtitles are normally visible for this theme family.
         */
        public Builder subtitleVisible(boolean subtitleVisible) {
            this.subtitleVisible = subtitleVisible;
            return this;
        }

        public Builder showSubtitle(boolean subtitleVisible) {
            return subtitleVisible(subtitleVisible);
        }

        /**
         * Controls whether icons are normally visible for this theme family.
         */
        public Builder iconVisible(boolean iconVisible) {
            this.iconVisible = iconVisible;
            return this;
        }

        public Builder showIcon(boolean iconVisible) {
            return iconVisible(iconVisible);
        }

        /**
         * Controls whether timed progress is allowed to appear for this theme family.
         */
        public Builder progressVisible(boolean progressVisible) {
            this.progressVisible = progressVisible;
            return this;
        }

        public Builder showProgress(boolean progressVisible) {
            return progressVisible(progressVisible);
        }

        /**
         * Convenience helper that applies one shadow flag to title, subtitle and badge text.
         * <p>
         * Use the more specific setters when you want fine-grained control per text role.
         * </p>
         */
        public Builder textShadow(boolean textShadow) {
            this.titleTextShadow = textShadow;
            this.subtitleTextShadow = textShadow;
            this.badgeTextShadow = textShadow;
            return this;
        }

        /**
         * Friendly alias for {@link #textShadow(boolean)} that disables the shadow in one call.
         */
        public Builder noTextShadow() {
            return textShadow(false);
        }

        /**
         * Controls whether the title should use LDLib's drop shadow.
         */
        public Builder titleTextShadow(boolean titleTextShadow) {
            this.titleTextShadow = titleTextShadow;
            return this;
        }

        /**
         * Controls whether the subtitle should use LDLib's drop shadow.
         */
        public Builder subtitleTextShadow(boolean subtitleTextShadow) {
            this.subtitleTextShadow = subtitleTextShadow;
            return this;
        }

        /**
         * Controls whether the badge text should use LDLib's drop shadow.
         */
        public Builder badgeTextShadow(boolean badgeTextShadow) {
            this.badgeTextShadow = badgeTextShadow;
            return this;
        }

        /**
         * Sets the main branch/family color.
         */
        public Builder primaryColor(int primaryColor) {
            this.primaryColor = primaryColor;
            return this;
        }

        /**
         * Sets the secondary branch/family color, often used for studied-state variants.
         */
        public Builder secondaryColor(int secondaryColor) {
            this.secondaryColor = secondaryColor;
            return this;
        }

        /**
         * Convenience helper that sets primary and secondary colors together.
         */
        public Builder colors(int primaryColor, int secondaryColor) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            return this;
        }

        /**
         * Optional explicit accent override.
         */
        public Builder accentColor(@Nullable Integer accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        /**
         * Optional explicit badge color override.
         */
        public Builder badgeColor(@Nullable Integer badgeColor) {
            this.badgeColor = badgeColor;
            return this;
        }

        /**
         * Optional explicit timed-progress fill color override.
         */
        public Builder progressBarColor(@Nullable Integer progressBarColor) {
            this.progressBarColor = progressBarColor;
            return this;
        }

        /**
         * Optional explicit timed-progress track color override.
         */
        public Builder progressBarBackgroundColor(@Nullable Integer progressBarBackgroundColor) {
            this.progressBarBackgroundColor = progressBarBackgroundColor;
            return this;
        }

        /**
         * Sets the default icon path for this theme family.
         * <p>
         * Non-blank values automatically enable icon visibility.
         * </p>
         */
        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath == null ? "" : iconPath;
            if (!this.iconPath.isBlank()) {
                this.iconVisible = true;
            }
            return this;
        }

        /**
         * Friendly alias for {@link #iconPath(String)}.
         */
        public Builder icon(String iconPath) {
            return iconPath(iconPath);
        }

        /**
         * Sets the preset badge text for this theme family.
         */
        public Builder badgeText(String badgeText) {
            this.badgeText = badgeText == null ? "" : badgeText;
            return this;
        }

        /**
         * Convenience helper that sets both badge text and badge color.
         */
        public Builder badge(String badgeText, int badgeColor) {
            this.badgeText = badgeText == null ? "" : badgeText;
            this.badgeColor = badgeColor;
            return this;
        }

        /**
         * Selects how nodes using this theme should appear during unlock reveal.
         */
        public Builder revealAnimationStyle(ResearchRevealAnimationStyle revealAnimationStyle) {
            if (revealAnimationStyle != null) {
                this.revealAnimationStyle = revealAnimationStyle;
            }
            return this;
        }

        /**
         * Selects the default outer silhouette/style family for nodes using this theme.
         */
        public Builder shapeStyle(ResearchNodeVisualDefinition.ShapeStyle shapeStyle) {
            if (shapeStyle != null) {
                this.shapeStyle = shapeStyle;
            }
            return this;
        }

        /**
         * Optional default size preset hint.
         */
        public Builder sizePreset(@Nullable ResearchNodeVisualDefinition.SizePreset sizePreset) {
            this.sizePreset = sizePreset;
            return this;
        }

        /**
         * Optional default title alignment hint.
         */
        public Builder titleAlignment(@Nullable ResearchNodeVisualDefinition.TitleAlignment titleAlignment) {
            this.titleAlignment = titleAlignment;
            return this;
        }

        /**
         * Builds the immutable semantic theme preset.
         */
        public ResearchNodeTheme build() {
            return new ResearchNodeTheme(this);
        }
    }
}
