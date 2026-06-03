package dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets;

/**
 * Immutable view-model that describes how a research node widget should look.
 * <p>
 * This object intentionally contains only visual configuration and no runtime widget logic,
 * so screens can build styles declaratively and factories can render them consistently.
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

        public Builder titleVisible(boolean titleVisible) {
            this.titleVisible = titleVisible;
            return this;
        }

        public Builder showTitle(boolean titleVisible) {
            return titleVisible(titleVisible);
        }

        public Builder subtitleVisible(boolean subtitleVisible) {
            this.subtitleVisible = subtitleVisible;
            return this;
        }

        public Builder showSubtitle(boolean subtitleVisible) {
            return subtitleVisible(subtitleVisible);
        }

        public Builder iconVisible(boolean iconVisible) {
            this.iconVisible = iconVisible;
            return this;
        }

        public Builder showIcon(boolean iconVisible) {
            return iconVisible(iconVisible);
        }

        public Builder progressVisible(boolean progressVisible) {
            this.progressVisible = progressVisible;
            return this;
        }

        public Builder showProgress(boolean progressVisible) {
            return progressVisible(progressVisible);
        }

        public Builder badgeVisible(boolean badgeVisible) {
            this.badgeVisible = badgeVisible;
            return this;
        }

        public Builder showBadge(boolean badgeVisible) {
            return badgeVisible(badgeVisible);
        }

        public Builder backgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder borderColor(int borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public Builder accentColor(int accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder badgeColor(int badgeColor) {
            this.badgeColor = badgeColor;
            return this;
        }

        public Builder progressBarColor(int progressBarColor) {
            this.progressBarColor = progressBarColor;
            return this;
        }

        public Builder progressBarBackgroundColor(int progressBarBackgroundColor) {
            this.progressBarBackgroundColor = progressBarBackgroundColor;
            return this;
        }

        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath == null ? "" : iconPath;
            if (!this.iconPath.isBlank()) {
                this.iconVisible = true;
            }
            return this;
        }

        public Builder icon(String iconPath) {
            return iconPath(iconPath);
        }

        public Builder badgeText(String badgeText) {
            this.badgeText = badgeText == null ? "" : badgeText;
            this.badgeVisible = !this.badgeText.isBlank();
            return this;
        }

        public Builder shapeStyle(ShapeStyle shapeStyle) {
            if (shapeStyle != null) {
                this.shapeStyle = shapeStyle;
            }
            return this;
        }

        public Builder sizePreset(SizePreset sizePreset) {
            if (sizePreset != null) {
                this.sizePreset = sizePreset;
            }
            return this;
        }

        public Builder titleAlignment(TitleAlignment titleAlignment) {
            if (titleAlignment != null) {
                this.titleAlignment = titleAlignment;
            }
            return this;
        }

        public ResearchNodeVisualDefinition build() {
            return new ResearchNodeVisualDefinition(this);
        }
    }
}
