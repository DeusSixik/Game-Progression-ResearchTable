package dev.sixik.gprt.impl.client.research_screen.research_tree.info;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Shared presets and post-processing helpers for research info-panel content.
 * <p>
 * This class stores reusable "content blocks" for the info panel: standard metadata,
 * auto-generated conditions, auto-generated unlocks and post-processing for jump buttons.
 * The goal is to keep screen classes focused on tree/progression logic while repetitive
 * panel patterns live in one place.
 * </p>
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #defaultNodeContent(ResearchNode, String, String, int)} -
 *     creates the common base content for a research node.</li>
 *     <li>{@link #appendMetadataSection(ResearchInfoContent.Builder, ResearchNode, String)} -
 *     appends the standard "Info" section.</li>
 *     <li>{@link #appendParentConditions(ResearchInfoContent.Builder, ResearchNode, List)} -
 *     appends the auto-generated prerequisites section.</li>
 *     <li>{@link #appendUnlockedRewards(ResearchInfoContent.Builder, List, String)} -
 *     appends the auto-generated unlocks section.</li>
 *     <li>{@link #sanitizeJumps(ResearchInfoContent, java.util.function.BiPredicate)} -
 *     hides or removes jump buttons that should not be rendered right now.</li>
 * </ul>
 *
 * <p><b>When to use this class:</b></p>
 * <ul>
 *     <li>Use it when you already have a {@link ResearchInfoContent.Builder} and want to append
 *     one of the standard panel sections.</li>
 *     <li>Use it when a screen still wants manual control over builder flow, but does not want
 *     to rewrite the common section layout again.</li>
 *     <li>Use {@link ResearchInfoContentFactory} instead when you want a higher-level,
 *     more declarative way to assemble the whole panel.</li>
 * </ul>
 */
public final class ResearchInfoContentPresets {
    private ResearchInfoContentPresets() {
    }

    /**
     * Creates the common base layout for a node details panel.
     */
    public static ResearchInfoContent.Builder defaultNodeContent(ResearchNode node,
                                                                 String modeText,
                                                                 String stateText,
                                                                 int panelColor
    ) {
        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        String group = node.getGroup() != null ? node.getGroup().getTitle() : "Unknown";
        String description = node.getDescription();
        if (description == null || description.isBlank()) {
            description = "ui.game_progression_research_table.research_info.description.missing";
        }

        return ResearchInfoContent.builder()
                .title(title)
                .titleCentered()
                .titleLarge(true)
                .groupText(ResearchInfoTextResolver.resolveLabeledValue("ui.game_progression_research_table.research_info.meta.group", group))
                .modeText(modeText)
                .stateText(stateText)
                .description(description)
                .panelColor(panelColor);
    }

    /**
     * Adds a compact metadata section with key and visibility information.
     */
    public static void appendMetadataSection(ResearchInfoContent.Builder builder,
                                             ResearchNode node,
                                             String visibilityText
    ) {
        builder.section("ui.game_progression_research_table.research_info.section.info", section -> {
            section.infoLine("ui.game_progression_research_table.research_info.meta.key",
                    node.getResearchKey() != null ? node.getResearchKey() : ("node_" + node.getId()));
            section.infoLine("ui.game_progression_research_table.research_info.meta.visibility", visibilityText);
        });
    }

    /**
     * Adds the standard prerequisites section for the given parent nodes.
     */
    public static void appendParentConditions(ResearchInfoContent.Builder builder,
                                              ResearchNode node,
                                              List<ResearchNode> parents
    ) {
        builder.section("ui.game_progression_research_table.research_info.section.required_researches", section -> {
            if (parents.isEmpty()) {
                section.conditionText("ui.game_progression_research_table.research_info.condition.none", true);
                return;
            }

            appendConditionModeSummary(section, node.getVisibilityMode(), parents);

            for (ResearchNode parent : parents) {
                String parentTitle = parent.getTitle() != null ? parent.getTitle() : ("Node " + parent.getId());
                section.condition(entry -> {
                    entry.text(ResearchInfoTextResolver.resolveText("ui.game_progression_research_table.research_info.condition.study") + " " + ResearchInfoTextResolver.resolveText(parentTitle))
                            .completed(parent.isStudied())
                            .tooltip(ResearchInfoTextResolver.resolveText("ui.game_progression_research_table.research_info.condition.required_research") + ": " + ResearchInfoTextResolver.resolveText(parentTitle))
                            .icon(new ColorRectTexture(parent.getGroupColor()));
                });
            }
        });
    }

    /**
     * Adds the standard unlocks section.
     */
    public static void appendUnlockedRewards(ResearchInfoContent.Builder builder,
                                             List<ResearchNode> visibleChildren,
                                             String fallbackText
    ) {
        builder.section("ui.game_progression_research_table.research_info.section.unlocks", section -> {
            if (visibleChildren.isEmpty()) {
                section.rewardText(fallbackText);
                return;
            }

            for (ResearchNode child : visibleChildren) {
                String childTitle = child.getTitle() != null ? child.getTitle() : ("Node " + child.getId());
                section.rewardResearch(childTitle, child.getResearchKey());
            }
        });
    }

    /**
     * Removes or hides jump buttons that should not be rendered in the current context.
     * <p>
     * The predicate receives the target research key and whether that row requested the
     * "visible target only" behavior.
     * </p>
     */
    public static ResearchInfoContent sanitizeJumps(ResearchInfoContent content,
                                                    BiPredicate<String, Boolean> jumpVisibility
    ) {
        ResearchInfoContent.Builder sanitizedBuilder = ResearchInfoContent.builder()
                .title(content.title())
                .titleAlign(content.titleAlign())
                .titleLarge(content.titleLarge())
                .groupText(content.groupText())
                .modeText(content.modeText())
                .stateText(content.stateText())
                .description(content.description())
                .panelColor(content.panelColor());

        if (content.showTimedProgress()) {
            sanitizedBuilder.timedProgress(
                    content.timedProgressText(),
                    content.timedProgress01(),
                    content.timedProgressFillColor()
            ).timedProgressStyle(content.timedProgressVisualStyle());
        } else {
            sanitizedBuilder.hideTimedProgress();
        }

        if (content.showResearchButton()) {
            sanitizedBuilder.researchButton(content.researchButtonText(), true, content.researchButtonEnabled());
        } else {
            sanitizedBuilder.hideResearchButton();
        }

        for (ResearchInfoSection section : content.sections()) {
            ResearchInfoSection.Builder sectionBuilder = ResearchInfoSection.builder().title(section.title());
            for (ResearchInfoEntry entry : section.entries()) {
                sectionBuilder.addEntry(sanitizeEntryJump(entry, jumpVisibility));
            }
            sanitizedBuilder.addSection(sectionBuilder.build());
        }

        return sanitizedBuilder.build();
    }

    private static void appendConditionModeSummary(ResearchInfoSection.Builder section,
                                                   ResearchNode.VisibilityMode visibilityMode,
                                                   List<ResearchNode> parents
    ) {
        if (parents.size() <= 1) {
            return;
        }

        switch (visibilityMode) {
            case REQUIRE_ANY_PARENT_STUDIED -> section.condition(entry -> entry
                    .text("ui.game_progression_research_table.research_info.condition.study_any_parent")
                    .completed(hasAnyStudiedParent(parents))
                    .tooltip("ui.game_progression_research_table.research_info.condition.study_any_parent.tooltip"));
            case REQUIRE_ALL_PARENTS_STUDIED -> section.condition(entry -> entry
                    .text("ui.game_progression_research_table.research_info.condition.study_all_parents")
                    .completed(hasAllStudiedParents(parents))
                    .tooltip("ui.game_progression_research_table.research_info.condition.study_all_parents.tooltip"));
            case ALWAYS_VISIBLE -> section.condition(entry -> entry
                    .text("ui.game_progression_research_table.research_info.condition.parents_navigation_only")
                    .completed(true)
                    .tooltip("ui.game_progression_research_table.research_info.condition.parents_navigation_only.tooltip"));
        }
    }

    private static ResearchInfoEntry sanitizeEntryJump(ResearchInfoEntry entry,
                                                       BiPredicate<String, Boolean> jumpVisibility
    ) {
        String researchKey = entry.jumpToResearchKey();
        if (researchKey == null || researchKey.isBlank()) {
            return entry;
        }

        if (jumpVisibility.test(researchKey, entry.visibleJumpTargetOnly())) {
            return entry;
        }

        return entry.toBuilder().hideJumpButton().build();
    }

    private static boolean hasAnyStudiedParent(List<ResearchNode> parents) {
        for (ResearchNode parent : parents) {
            if (parent.isStudied()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAllStudiedParents(List<ResearchNode> parents) {
        for (ResearchNode parent : parents) {
            if (!parent.isStudied()) {
                return false;
            }
        }
        return true;
    }
}
