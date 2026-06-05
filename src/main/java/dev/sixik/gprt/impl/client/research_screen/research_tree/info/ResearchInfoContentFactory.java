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
                .title("ui.game_progression_research_table.research_info.empty.title")
                .titleCentered()
                .titleLarge(true)
                .groupText("ui.game_progression_research_table.research_info.empty.group")
                .modeText("ui.game_progression_research_table.research_info.empty.mode")
                .stateText("ui.game_progression_research_table.research_info.empty.state")
                .description("ui.game_progression_research_table.research_info.empty.description")
                .hideTimedProgress()
                .hideResearchButton()
                .build();
    }

    /**
     * High-level fluent builder for standard details-panel content.
     * <p>
     * Use this builder when you want a mostly conventional info panel without manually assembling
     * every section through raw {@link ResearchInfoContent.Builder} calls.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #modeText(String)} / {@link #stateText(String)} / {@link #visibilityText(String)} -
     *     standard metadata labels;</li>
     *     <li>{@link #conditions(List)} / {@link #noConditions()} - prerequisites block;</li>
     *     <li>{@link #unlocks(List, String)} / {@link #noUnlocks()} - unlock preview block;</li>
     *     <li>{@link #timedProgress(String, float, int)} / {@link #hideTimedProgress()} - timed mode row;</li>
     *     <li>{@link #researchButton(String)} / {@link #hideResearchButton()} - bottom action button;</li>
     *     <li>{@link #build()} - final immutable content snapshot.</li>
     * </ul>
     */
    public static final class Builder {
        private final ResearchNode node;
        private String modeText = "ui.game_progression_research_table.research_info.empty.mode";
        private String stateText = "ui.game_progression_research_table.research_info.empty.state";
        private String visibilityText = "ui.game_progression_research_table.research_info.visibility.always_visible";
        private int panelColor = 0xE6192432;
        private boolean includeConditions;
        private boolean includeUnlocks;
        private List<ResearchNode> parentNodes = List.of();
        private List<ResearchNode> unlockedChildren = List.of();
        private String unlocksFallbackText = "ui.game_progression_research_table.research_info.unlocks.none";
        private boolean showTimedProgress;
        private String timedProgressText = "ui.game_progression_research_table.research_info.progress.empty";
        private float timedProgress01;
        private int timedProgressFillColor = 0xFF67B7FF;
        private boolean showResearchButton;
        private boolean researchButtonEnabled = true;
        private String researchButtonText = "ui.game_progression_research_table.research_info.button.available.instant";

        private Builder(ResearchNode node) {
            this.node = node;
        }

        /**
         * Sets the short mode text shown near the panel header.
         */
        public Builder modeText(String modeText) {
            this.modeText = modeText;
            return this;
        }

        /**
         * Sets the short state/status text shown near the panel header.
         */
        public Builder stateText(String stateText) {
            this.stateText = stateText;
            return this;
        }

        /**
         * Sets the visibility/explanation text used by the standard metadata section.
         */
        public Builder visibilityText(String visibilityText) {
            this.visibilityText = visibilityText;
            return this;
        }

        /**
         * Overrides the panel background tint used by the standard preset.
         */
        public Builder panelColor(int panelColor) {
            this.panelColor = panelColor;
            return this;
        }

        /**
         * Enables the standard prerequisites section and supplies the parent nodes to render.
         */
        public Builder conditions(List<ResearchNode> parentNodes) {
            this.includeConditions = true;
            this.parentNodes = parentNodes == null ? List.of() : List.copyOf(parentNodes);
            return this;
        }

        /**
         * Disables the standard prerequisites section entirely.
         */
        public Builder noConditions() {
            this.includeConditions = false;
            this.parentNodes = List.of();
            return this;
        }

        /**
         * Enables the standard unlocks/rewards section and supplies the child nodes to render.
         */
        public Builder unlocks(List<ResearchNode> unlockedChildren, String fallbackText) {
            this.includeUnlocks = true;
            this.unlockedChildren = unlockedChildren == null ? List.of() : List.copyOf(unlockedChildren);
            this.unlocksFallbackText = fallbackText == null ? "ui.game_progression_research_table.research_info.unlocks.none" : fallbackText;
            return this;
        }

        /**
         * Disables the standard unlocks/rewards section entirely.
         */
        public Builder noUnlocks() {
            this.includeUnlocks = false;
            this.unlockedChildren = List.of();
            return this;
        }

        /**
         * Shows the timed-progress block with preformatted text and fill state.
         */
        public Builder timedProgress(String timedProgressText, float timedProgress01, int timedProgressFillColor) {
            this.showTimedProgress = true;
            this.timedProgressText = timedProgressText;
            this.timedProgress01 = timedProgress01;
            this.timedProgressFillColor = timedProgressFillColor;
            return this;
        }

        /**
         * Hides the timed-progress block.
         */
        public Builder hideTimedProgress() {
            this.showTimedProgress = false;
            this.timedProgressText = "ui.game_progression_research_table.research_info.progress.empty";
            this.timedProgress01 = 0f;
            return this;
        }

        /**
         * Shows the main action button and sets its label.
         */
        public Builder researchButton(String researchButtonText) {
            return researchButton(researchButtonText, true);
        }

        /**
         * Shows the main action button, sets its label and controls whether it can be clicked.
         */
        public Builder researchButton(String researchButtonText, boolean enabled) {
            this.showResearchButton = true;
            this.researchButtonText = researchButtonText;
            this.researchButtonEnabled = enabled;
            return this;
        }

        /**
         * Hides the main action button completely.
         */
        public Builder hideResearchButton() {
            this.showResearchButton = false;
            this.researchButtonEnabled = false;
            return this;
        }

        /**
         * Builds the final standard panel content snapshot.
         */
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
                builder.researchButton(researchButtonText, true, researchButtonEnabled);
            } else {
                builder.hideResearchButton();
            }

            return builder.build();
        }
    }
}
