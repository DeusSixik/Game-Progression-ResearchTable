package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Immutable view-model for the research details panel.
 * <p>
 * This model intentionally stores ready-to-render rich content:
 * title settings, basic text blocks, timed progress state and a list of
 * reusable sections made from visual entries.
 * </p>
 */
public final class ResearchInfoContent {
    public enum TitleAlign {
        LEFT,
        CENTER
    }

    private final String title;
    private final TitleAlign titleAlign;
    private final boolean titleLarge;
    private final String groupText;
    private final String modeText;
    private final String stateText;
    private final String description;
    private final int panelColor;
    private final boolean showTimedProgress;
    private final String timedProgressText;
    private final float timedProgress01;
    private final int timedProgressFillColor;
    private final boolean showResearchButton;
    private final String researchButtonText;
    private final List<ResearchInfoSection> sections;

    private ResearchInfoContent(Builder builder) {
        this.title = builder.title;
        this.titleAlign = builder.titleAlign;
        this.titleLarge = builder.titleLarge;
        this.groupText = builder.groupText;
        this.modeText = builder.modeText;
        this.stateText = builder.stateText;
        this.description = builder.description;
        this.panelColor = builder.panelColor;
        this.showTimedProgress = builder.showTimedProgress;
        this.timedProgressText = builder.timedProgressText;
        this.timedProgress01 = clamp01(builder.timedProgress01);
        this.timedProgressFillColor = builder.timedProgressFillColor;
        this.showResearchButton = builder.showResearchButton;
        this.researchButtonText = builder.researchButtonText;
        this.sections = List.copyOf(builder.sections);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .title(title)
                .titleAlign(titleAlign)
                .titleLarge(titleLarge)
                .groupText(groupText)
                .modeText(modeText)
                .stateText(stateText)
                .description(description)
                .panelColor(panelColor)
                .copyTimedProgress(showTimedProgress, timedProgressText, timedProgress01, timedProgressFillColor)
                .researchButton(researchButtonText, showResearchButton)
                .addSections(sections);
    }

    public String title() {
        return title;
    }

    public TitleAlign titleAlign() {
        return titleAlign;
    }

    public boolean titleLarge() {
        return titleLarge;
    }

    public String groupText() {
        return groupText;
    }

    public String modeText() {
        return modeText;
    }

    public String stateText() {
        return stateText;
    }

    public String description() {
        return description;
    }

    public int panelColor() {
        return panelColor;
    }

    public boolean showTimedProgress() {
        return showTimedProgress;
    }

    public String timedProgressText() {
        return timedProgressText;
    }

    public float timedProgress01() {
        return timedProgress01;
    }

    public int timedProgressFillColor() {
        return timedProgressFillColor;
    }

    public boolean showResearchButton() {
        return showResearchButton;
    }

    public String researchButtonText() {
        return researchButtonText;
    }

    public List<ResearchInfoSection> sections() {
        return sections;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static final class Builder {
        private String title = "Research";
        private TitleAlign titleAlign = TitleAlign.CENTER;
        private boolean titleLarge = true;
        private String groupText = "Group: -";
        private String modeText = "Mode: -";
        private String stateText = "Status: -";
        private String description = "Click a research node to open its info panel.";
        private int panelColor = 0xE6192432;
        private boolean showTimedProgress;
        private String timedProgressText = "Progress: -";
        private float timedProgress01;
        private int timedProgressFillColor = 0xFF67B7FF;
        private boolean showResearchButton;
        private String researchButtonText = "Research";
        private final List<ResearchInfoSection> sections = new ArrayList<>();

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder titleAlign(TitleAlign titleAlign) {
            this.titleAlign = titleAlign;
            return this;
        }

        public Builder titleCentered() {
            this.titleAlign = TitleAlign.CENTER;
            return this;
        }

        public Builder titleLarge(boolean titleLarge) {
            this.titleLarge = titleLarge;
            return this;
        }

        public Builder groupText(String groupText) {
            this.groupText = groupText;
            return this;
        }

        public Builder modeText(String modeText) {
            this.modeText = modeText;
            return this;
        }

        public Builder stateText(String stateText) {
            this.stateText = stateText;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder panelColor(int panelColor) {
            this.panelColor = panelColor;
            return this;
        }

        public Builder timedProgress(String timedProgressText, float timedProgress01, int timedProgressFillColor) {
            this.showTimedProgress = true;
            this.timedProgressText = timedProgressText;
            this.timedProgress01 = timedProgress01;
            this.timedProgressFillColor = timedProgressFillColor;
            return this;
        }

        private Builder copyTimedProgress(boolean showTimedProgress,
                                          String timedProgressText,
                                          float timedProgress01,
                                          int timedProgressFillColor
        ) {
            this.showTimedProgress = showTimedProgress;
            this.timedProgressText = timedProgressText;
            this.timedProgress01 = timedProgress01;
            this.timedProgressFillColor = timedProgressFillColor;
            return this;
        }

        public Builder hideTimedProgress() {
            this.showTimedProgress = false;
            this.timedProgressText = "Progress: -";
            this.timedProgress01 = 0f;
            return this;
        }

        public Builder researchButton(String researchButtonText, boolean showResearchButton) {
            this.researchButtonText = researchButtonText;
            this.showResearchButton = showResearchButton;
            return this;
        }

        public Builder hideResearchButton() {
            this.showResearchButton = false;
            return this;
        }

        public Builder addSection(ResearchInfoSection section) {
            if (section != null) {
                this.sections.add(section);
            }
            return this;
        }

        public Builder addSections(Iterable<ResearchInfoSection> sections) {
            for (ResearchInfoSection section : sections) {
                addSection(section);
            }
            return this;
        }

        public Builder section(String title, Consumer<ResearchInfoSection.Builder> builderConsumer) {
            ResearchInfoSection.Builder sectionBuilder = ResearchInfoSection.builder().title(title);
            builderConsumer.accept(sectionBuilder);
            return addSection(sectionBuilder.build());
        }

        public ResearchInfoContent build() {
            return new ResearchInfoContent(this);
        }
    }
}
