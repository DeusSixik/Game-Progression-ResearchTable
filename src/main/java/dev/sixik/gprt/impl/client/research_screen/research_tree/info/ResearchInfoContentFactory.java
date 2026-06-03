package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

import java.util.List;

/**
 * High-level factory for assembling standard research info-panel content.
 * <p>
 * This class is the main entry point when a screen wants to build a full details panel
 * declaratively instead of manually pushing every section into a raw
 * {@link ResearchInfoContent.Builder}.
 * </p>
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #forNode(ResearchNode)} -
 *     starts building a standard node-bound panel.</li>
 *     <li>{@link #emptySelection()} -
 *     returns the default content used when no research is selected.</li>
 *     <li>{@link Builder#conditions(List)} / {@link Builder#noConditions()} -
 *     controls the standard prerequisites section.</li>
 *     <li>{@link Builder#unlocks(List, String)} / {@link Builder#noUnlocks()} -
 *     controls the standard unlocks section.</li>
 *     <li>{@link Builder#timedProgress(String, float, int)} -
 *     configures the timed progress block.</li>
 *     <li>{@link Builder#researchButton(String)} -
 *     configures the bottom action button.</li>
 * </ul>
 *
 * <p><b>When to use this class:</b></p>
 * <ul>
 *     <li>Use it when you want to assemble most or all of the panel in one fluent builder.</li>
 *     <li>Use it together with {@link ResearchInfoPresentationRules} when the screen wants
 *     shared formatting/state rules for progress text and buttons.</li>
 *     <li>Use {@link ResearchInfoContentPresets} when you only need to append individual
 *     reusable blocks to an existing content builder.</li>
 * </ul>
 */
public final class ResearchInfoContentFactory {
    private ResearchInfoContentFactory() {
    }

    /**
     * Starts building a standard node details panel.
     */
    public static Builder forNode(ResearchNode node) {
        return new Builder(node);
    }

    /**
     * Creates the default empty-state content used when no node is selected.
     */
    public static ResearchInfoContent emptySelection() {
        return ResearchInfoContent.builder()
                .title("Research")
                .titleCentered()
                .titleLarge(true)
                .groupText("Group: -")
                .modeText("Mode: -")
                .stateText("Status: -")
                .description("Click a research node to open its info panel.")
                .hideTimedProgress()
                .hideResearchButton()
                .build();
    }

    public static final class Builder {
        private final ResearchNode node;
        private String modeText = "Mode: -";
        private String stateText = "Status: -";
        private String visibilityText = "Always visible";
        private int panelColor = 0xE6192432;
        private boolean includeConditions;
        private boolean includeUnlocks;
        private List<ResearchNode> parentNodes = List.of();
        private List<ResearchNode> unlockedChildren = List.of();
        private String unlocksFallbackText = "No direct follow-up research";
        private boolean showTimedProgress;
        private String timedProgressText = "Progress: -";
        private float timedProgress01;
        private int timedProgressFillColor = 0xFF67B7FF;
        private boolean showResearchButton;
        private String researchButtonText = "Research";

        private Builder(ResearchNode node) {
            this.node = node;
        }

        public Builder modeText(String modeText) {
            this.modeText = modeText;
            return this;
        }

        public Builder stateText(String stateText) {
            this.stateText = stateText;
            return this;
        }

        public Builder visibilityText(String visibilityText) {
            this.visibilityText = visibilityText;
            return this;
        }

        public Builder panelColor(int panelColor) {
            this.panelColor = panelColor;
            return this;
        }

        public Builder conditions(List<ResearchNode> parentNodes) {
            this.includeConditions = true;
            this.parentNodes = parentNodes == null ? List.of() : List.copyOf(parentNodes);
            return this;
        }

        public Builder noConditions() {
            this.includeConditions = false;
            this.parentNodes = List.of();
            return this;
        }

        public Builder unlocks(List<ResearchNode> unlockedChildren, String fallbackText) {
            this.includeUnlocks = true;
            this.unlockedChildren = unlockedChildren == null ? List.of() : List.copyOf(unlockedChildren);
            this.unlocksFallbackText = fallbackText == null ? "No direct follow-up research" : fallbackText;
            return this;
        }

        public Builder noUnlocks() {
            this.includeUnlocks = false;
            this.unlockedChildren = List.of();
            return this;
        }

        public Builder timedProgress(String timedProgressText, float timedProgress01, int timedProgressFillColor) {
            this.showTimedProgress = true;
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

        public Builder researchButton(String researchButtonText) {
            this.showResearchButton = true;
            this.researchButtonText = researchButtonText;
            return this;
        }

        public Builder hideResearchButton() {
            this.showResearchButton = false;
            return this;
        }

        public ResearchInfoContent build() {
            ResearchInfoContent.Builder builder = ResearchInfoContentPresets.defaultNodeContent(
                    node,
                    modeText,
                    stateText,
                    panelColor
            );
            ResearchInfoContentPresets.appendMetadataSection(builder, node, visibilityText);
            if (includeConditions) {
                ResearchInfoContentPresets.appendParentConditions(builder, node, parentNodes);
            }
            if (includeUnlocks) {
                ResearchInfoContentPresets.appendUnlockedRewards(builder, unlockedChildren, unlocksFallbackText);
            }

            if (showTimedProgress) {
                builder.timedProgress(timedProgressText, timedProgress01, timedProgressFillColor);
            } else {
                builder.hideTimedProgress();
            }

            if (showResearchButton) {
                builder.researchButton(researchButtonText, true);
            } else {
                builder.hideResearchButton();
            }

            return builder.build();
        }
    }
}
