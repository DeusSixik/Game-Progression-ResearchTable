package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

/**
 * Immutable view-model that describes how a research node widget should look.
 * <p>
 * This object intentionally contains only visual configuration and no runtime widget logic,
 * so screens can build styles declaratively and factories can render them consistently.
 * Think of it as the final set of "paint/layout hints" for one node widget.
 * </p>
 */
public final class ResearchNodeVisualDefinition {
    public enum ShapeStyle {
        RECTANGLE,
        ROUNDED_RECTANGLE
    }

    public enum SizePreset {
        SMALL,
        MEDIUM,
        LARGE
    }

    public enum TitleAlignment {
        LEFT,
        CENTER
    }

    private final boolean titleVisible;
    private final boolean subtitleVisible;
    private final boolean iconVisible;
    private final boolean progressVisible;
    private final boolean badgeVisible;
    private final int backgroundColor;
    private final int borderColor;
    private final int accentColor;
    private final int badgeColor;
    private final int progressBarColor;
    private final int progressBarBackgroundColor;
    private final String iconPath;
    private final String badgeText;
    private final ShapeStyle shapeStyle;
    private final SizePreset sizePreset;
    private final TitleAlignment titleAlignment;

    private ResearchNodeVisualDefinition(Builder builder) {
        this.titleVisible = builder.titleVisible;
        this.subtitleVisible = builder.subtitleVisible;
        this.iconVisible = builder.iconVisible;
        this.progressVisible = builder.progressVisible;
        this.badgeVisible = builder.badgeVisible;
        this.backgroundColor = builder.backgroundColor;
        this.borderColor = builder.borderColor;
        this.accentColor = builder.accentColor;
        this.badgeColor = builder.badgeColor;
        this.progressBarColor = builder.progressBarColor;
        this.progressBarBackgroundColor = builder.progressBarBackgroundColor;
        this.iconPath = builder.iconPath;
        this.badgeText = builder.badgeText;
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
                .badgeVisible(badgeVisible)
                .backgroundColor(backgroundColor)
                .borderColor(borderColor)
                .accentColor(accentColor)
                .badgeColor(badgeColor)
                .progressBarColor(progressBarColor)
                .progressBarBackgroundColor(progressBarBackgroundColor)
                .iconPath(iconPath)
                .badgeText(badgeText)
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

    public boolean isBadgeVisible() {
        return badgeVisible;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getBadgeColor() {
        return badgeColor;
    }

    public int getProgressBarColor() {
        return progressBarColor;
    }

    public int getProgressBarBackgroundColor() {
        return progressBarBackgroundColor;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public ShapeStyle getShapeStyle() {
        return shapeStyle;
    }

    public SizePreset getSizePreset() {
        return sizePreset;
    }

    public TitleAlignment getTitleAlignment() {
        return titleAlignment;
    }

    /**
     * Fluent builder for {@link ResearchNodeVisualDefinition}.
     * <p>
     * Use this builder when you want direct low-level control over the final node visuals:
     * colors, icon path, badge text, progress-bar colors, title visibility and simple layout hints.
     * In most real screens this builder is filled indirectly by a theme + resolver pipeline, but it
     * is also valid to build definitions by hand for fully custom node systems.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #titleVisible(boolean)}, {@link #subtitleVisible(boolean)}, {@link #iconVisible(boolean)},
     *     {@link #progressVisible(boolean)}, {@link #badgeVisible(boolean)} - feature toggles;</li>
     *     <li>{@link #backgroundColor(int)}, {@link #borderColor(int)}, {@link #accentColor(int)} -
     *     base palette;</li>
     *     <li>{@link #badgeColor(int)}, {@link #badgeText(String)} - badge presentation;</li>
     *     <li>{@link #progressBarColor(int)}, {@link #progressBarBackgroundColor(int)} - progress styling;</li>
     *     <li>{@link #iconPath(String)}, {@link #shapeStyle(ShapeStyle)}, {@link #sizePreset(SizePreset)},
     *     {@link #titleAlignment(TitleAlignment)} - layout and icon hints;</li>
     *     <li>{@link #build()} - final immutable visual description.</li>
     * </ul>
     */
    public static final class Builder {
        private boolean titleVisible = true;
        private boolean subtitleVisible;
        private boolean iconVisible;
        private boolean progressVisible;
        private boolean badgeVisible;
        private int backgroundColor = 0xFF223344;
        private int borderColor = 0xFF67B7FF;
        private int accentColor = 0xFF67B7FF;
        private int badgeColor = 0xFF67B7FF;
        private int progressBarColor = 0xFF67B7FF;
        private int progressBarBackgroundColor = 0x55232D39;
        private String iconPath = "";
        private String badgeText = "";
        private ShapeStyle shapeStyle = ShapeStyle.RECTANGLE;
        private SizePreset sizePreset = SizePreset.MEDIUM;
        private TitleAlignment titleAlignment = TitleAlignment.CENTER;

        /**
         * Controls whether the main title should be rendered.
         */
        public Builder titleVisible(boolean titleVisible) {
            this.titleVisible = titleVisible;
            return this;
        }

        /**
         * Friendly alias for {@link #titleVisible(boolean)}.
         */
        public Builder showTitle(boolean titleVisible) {
            return titleVisible(titleVisible);
        }

        /**
         * Controls whether the subtitle/status line should be rendered.
         */
        public Builder subtitleVisible(boolean subtitleVisible) {
            this.subtitleVisible = subtitleVisible;
            return this;
        }

        /**
         * Friendly alias for {@link #subtitleVisible(boolean)}.
         */
        public Builder showSubtitle(boolean subtitleVisible) {
            return subtitleVisible(subtitleVisible);
        }

        /**
         * Controls whether the icon slot may be rendered.
         */
        public Builder iconVisible(boolean iconVisible) {
            this.iconVisible = iconVisible;
            return this;
        }

        /**
         * Friendly alias for {@link #iconVisible(boolean)}.
         */
        public Builder showIcon(boolean iconVisible) {
            return iconVisible(iconVisible);
        }

        /**
         * Controls whether the node-level progress bar may be rendered.
         * <p>
         * Final runtime visibility still depends on current node state and whether a timed-progress
         * snapshot exists.
         * </p>
         */
        public Builder progressVisible(boolean progressVisible) {
            this.progressVisible = progressVisible;
            return this;
        }

        /**
         * Friendly alias for {@link #progressVisible(boolean)}.
         */
        public Builder showProgress(boolean progressVisible) {
            return progressVisible(progressVisible);
        }

        /**
         * Controls whether the badge area may be rendered.
         */
        public Builder badgeVisible(boolean badgeVisible) {
            this.badgeVisible = badgeVisible;
            return this;
        }

        /**
         * Friendly alias for {@link #badgeVisible(boolean)}.
         */
        public Builder showBadge(boolean badgeVisible) {
            return badgeVisible(badgeVisible);
        }

        /**
         * Sets the main card background color.
         */
        public Builder backgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        /**
         * Sets the outer frame/border color.
         */
        public Builder borderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        /**
         * Sets the accent color used for stripes, bars and other emphasis details.
         */
        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        /**
         * Sets the badge background color.
         */
        public Builder badgeColor(int badgeColor) {
            this.badgeColor = badgeColor;
            return this;
        }

        /**
         * Sets the timed-progress fill color.
         */
        public Builder progressBarColor(int progressBarColor) {
            this.progressBarColor = progressBarColor;
            return this;
        }

        /**
         * Sets the timed-progress track/background color.
         */
        public Builder progressBarBackgroundColor(int progressBarBackgroundColor) {
            this.progressBarBackgroundColor = progressBarBackgroundColor;
            return this;
        }

        /**
         * Sets the sprite path used as the node icon.
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
         * Sets the badge text and auto-enables badge visibility when the text is non-blank.
         */
        public Builder badgeText(String badgeText) {
            this.badgeText = badgeText == null ? "" : badgeText;
            this.badgeVisible = !this.badgeText.isBlank();
            return this;
        }

        /**
         * Selects the coarse outer silhouette/style family.
         */
        public Builder shapeStyle(ShapeStyle shapeStyle) {
            if (shapeStyle != null) {
                this.shapeStyle = shapeStyle;
            }
            return this;
        }

        /**
         * Selects the coarse size preset used by widget factories for layout density and fonts.
         */
        public Builder sizePreset(SizePreset sizePreset) {
            if (sizePreset != null) {
                this.sizePreset = sizePreset;
            }
            return this;
        }

        /**
         * Selects the default title alignment hint for widget factories.
         */
        public Builder titleAlignment(TitleAlignment titleAlignment) {
            if (titleAlignment != null) {
                this.titleAlignment = titleAlignment;
            }
            return this;
        }

        /**
         * Builds the immutable visual definition.
         */
        public ResearchNodeVisualDefinition build() {
            return new ResearchNodeVisualDefinition(this);
        }
    }
}
