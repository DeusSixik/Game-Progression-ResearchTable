package dev.sixik.gprt.impl.client.research_screen.research_table;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.api.ResearchCondition;
import dev.sixik.gprt.api.ResearchDefinition;
import dev.sixik.gprt.api.ResearchGroupDefinition;
import dev.sixik.gprt.api.ResearchReward;
import dev.sixik.gprt.impl.client.research_screen.demo.DebugResearchNodeWidgetShowcase;
import dev.sixik.gprt.impl.client.research_screen.research_table.widget_factory.ResearchTableNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeBuild;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreenMainScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContent;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelWidget;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.*;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_table.info.TableInfoPanelWidget;
import dev.sixik.gprt.impl.utils.ResearchUtils;
import dev.sixik.gprt.test.GprtTests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResearchTableScreen extends ResearchTreeScreenMainScreen {

    private static final String DEFAULT_ROOT_KEY = "primitive_tools";
    private static final float BASE_NODE_WIDTH = 132f;
    private static final float BASE_NODE_HEIGHT = 42f;
    private final Map<String, dev.sixik.gprt.api.ResearchDefinition> researchDefinitionsByKey = new LinkedHashMap<>();

    public ResearchTableScreen() {
        autoLayoutConfig()
                .origin(0f, 0f)
                .horizontalGap(130f)
                .verticalGap(42f);
        setAutoLayoutUseNodeWrappersForSpacing(true);
        initializeMainScreen();
    }

    @Override
    protected void buildResearchTree() {
        ResearchTreeBuild build = ResearchTreeBuild.create();
        researchDefinitionsByKey.clear();

        GprtTests.BuildData data = GprtTests.createDebugRecipes();

        for (ResearchGroupDefinition group : data.groups()) {
            build.group(group);
        }

        String fallbackRootKey = null;
        for (dev.sixik.gprt.api.ResearchDefinition research : data.definitions()) {
            if (fallbackRootKey == null) {
                fallbackRootKey = research.getKey();
            }
            researchDefinitionsByKey.put(research.getKey(), research);
            build.node(research);
        }

        ResearchTreeBuild.BuildResult buildResult = build.applyTo(this);
        String rootKey = containsResearch(data, DEFAULT_ROOT_KEY) ? DEFAULT_ROOT_KEY : fallbackRootKey;
        if (rootKey != null) {
            setRootNodeId(buildResult.nodeId(rootKey));
        }
    }

    @Override
    protected ResearchNodeWidgetFactory createNodeWidgetFactory() {
        return new ResearchTableNodeWidgetFactory();
    }

//    @Override
//    protected ResearchNodeWrapperResolver createNodeWrapperResolver() {
//        return (node, context) -> {
//            ResearchNodeWrapper wrapper = context.getNodeWrapper();
//            dev.sixik.gprt.api.ResearchDefinition definition = researchDefinitionsByKey.get(node.getResearchKey());
//
//            float titleUnits = estimateTitleUnits(node.getTitle());
//            float descriptionUnits = definition == null ? 0f : estimateDescriptionUnits(definition.getDescription());
//            float rewardsUnits = definition == null ? 0f : definition.getRewards().size() * 14f;
//            float conditionsUnits = definition == null ? 0f : definition.getConditions().size() * 12f;
//
//            // Example dynamic width:
//            // wider titles and more content reserve more horizontal room up front.
//            float targetWidth = BASE_NODE_WIDTH
//                    + Math.min(160f, Math.max(0f, titleUnits - 18f) * 4.4f)
//                    + Math.min(70f, descriptionUnits * 0.45f);
//
//            // Example dynamic height:
//            // table-style nodes become taller when they contain more data.
//            float targetHeight = BASE_NODE_HEIGHT
//                    + Math.min(52f, descriptionUnits * 0.22f)
//                    + Math.min(34f, rewardsUnits)
//                    + Math.min(28f, conditionsUnits);
//
//            // Center expansion around the original logical node center so the card grows evenly.
//            float centerX = node.centerX();
//            float centerY = node.centerY();
//            wrapper.setBounds(
//                    centerX - targetWidth * 0.5f,
//                    centerY - targetHeight * 0.5f,
//                    targetWidth,
//                    targetHeight
//            );
//        };
//    }

    /*@Override
    protected void configureNodeThemePresets(ResearchNodeGroupThemeResolver.Builder builder) {
        builder.group("metallurgy", theme -> theme
                .primaryColor(0xFFE29A47)
                .secondaryColor(0xFFF0C17C)
                .accentColor(0xFFFFC766)
                .badge("FORGE", 0xFFF0C17C)
                .revealAnimationStyle(ResearchRevealAnimationStyle.DROP_BOUNCE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group("farming", theme -> theme
                .primaryColor(0xFF54B36B)
                .secondaryColor(0xFF87D99C)
                .accentColor(0xFF9BE27F)
                .badge("GROW", 0xFFA7E3A4)
                .revealAnimationStyle(ResearchRevealAnimationStyle.SOFT_POP)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group("logistics", theme -> theme
                .primaryColor(0xFF4C90E8)
                .secondaryColor(0xFF81B7FF)
                .accentColor(0xFF8BC0FF)
                .badge("FLOW", 0xFFA9CBFF)
                .revealAnimationStyle(ResearchRevealAnimationStyle.SLIDE_FROM_LEFT)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group("energy", theme -> theme
                .primaryColor(0xFFF0C94A)
                .secondaryColor(0xFFFFE08A)
                .accentColor(0xFFFFE07A)
                .badge("SPARK", 0xFFFFEDAE)
                .revealAnimationStyle(ResearchRevealAnimationStyle.FADE_SCALE)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
        builder.group("alchemy", theme -> theme
                .primaryColor(0xFF9C6BE8)
                .secondaryColor(0xFFC7A8FF)
                .accentColor(0xFFD8B4FF)
                .badge("MIST", 0xFFE7D4FF)
                .revealAnimationStyle(ResearchRevealAnimationStyle.ARC_DROP)
                .badgeTextShadow(false)
                .titleAlignment(ResearchNodeVisualDefinition.TitleAlignment.LEFT));
    }*/

    @Override
    protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state) {
        ResearchInfoContent.Builder builder = createStandardInfoContentBuilder(node, state).build().toBuilder();

        ResearchDefinition data = researchDefinitionsByKey.get(node.getResearchKey());
        if(data != null) {


            List<ResearchCondition> conditions = data.getConditions();
            if(conditions != null && !conditions.isEmpty()) {

                builder.section("Conditions", section -> {
                    for (ResearchCondition condition : conditions) {
                        switch (condition.getKind()) {
                            case ITEM -> section.conditionItem(condition.getStack().getDisplayName().getString(), condition.getStack(),
                                    ResearchUtils.hasPlayerItem(Minecraft.getInstance().player, condition.getStack()));
                            case INGREDIENT -> section.condition(entry -> entry
                                    .ingredient(condition.getIngredient())
                                    .completed(ResearchUtils.hasPlayerIngredient(Minecraft.getInstance().player, condition.getIngredient()))
                            );
                            case STAGE -> section.condition(entry -> entry
                                    .text("Stage: '" + condition.getValue() + "'")
                                    .completed(ResearchUtils.hasStage(condition.getValue()))
                            );
                            case CUSTOM -> section.condition(entry -> entry
                                    .text(condition.getKey())
                            );
                        }
                    }
                });
            }

            List<ResearchReward> rewards = data.getRewards();
            if(rewards != null && !rewards.isEmpty()) {
                builder.section("Rewards", section -> {
                    for (ResearchReward reward : rewards) {
                        switch (reward.getKind()) {
                            case ITEM -> section.rewardItem(reward.getStack());
                            case INGREDIENT -> section.rewardIngredient(reward.getKey(), reward.getIngredient());
                            case RESEARCH -> section.rewardText("Stage: '" + reward.getKey() + "'");
                            case CUSTOM -> section.rewardText(reward.getKey());
                        }
                    }
                });
            }
        }

        return builder.build();
    }

    @Override
    protected ResearchInfoPanelWidget createInfoPanelWidget(ResearchInfoPanelContext context) {
        return new TableInfoPanelWidget(context);
    }

    @Override
    protected boolean canStartResearch(ResearchNode node) {
        return areResearchConditionsMet(node);
    }

    protected final boolean areResearchConditionsMet(ResearchNode node) {
        if (!areParentResearchRequirementsMet(node)) {
            return false;
        }

        dev.sixik.gprt.api.ResearchDefinition definition = researchDefinitionsByKey.get(node.getResearchKey());
        if (definition == null || definition.getConditions().isEmpty()) {
            return true;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        for (ResearchCondition condition : definition.getConditions()) {
            if (!isConditionMet(player, condition)) {
                return false;
            }
        }
        return true;
    }

    protected final boolean areParentResearchRequirementsMet(ResearchNode node) {
        var parents = collectParentNodes(node);
        if (parents.isEmpty()) {
            return true;
        }

        return switch (node.getVisibilityMode()) {
            case ALWAYS_VISIBLE, REQUIRE_ALL_PARENTS_STUDIED -> areAllParentsStudied(parents);
            case REQUIRE_ANY_PARENT_STUDIED -> hasAnyStudiedParent(parents);
        };
    }

    protected final boolean hasAnyStudiedParent(Iterable<ResearchNode> parents) {
        for (ResearchNode parent : parents) {
            if (parent != null && parent.isStudied()) {
                return true;
            }
        }
        return false;
    }

    protected final boolean areAllParentsStudied(Iterable<ResearchNode> parents) {
        for (ResearchNode parent : parents) {
            if (parent == null || !parent.isStudied()) {
                return false;
            }
        }
        return true;
    }

    protected final boolean isConditionMet(LocalPlayer player, ResearchCondition condition) {
        if (condition == null) {
            return true;
        }

        return switch (condition.getKind()) {
            case ITEM -> ResearchUtils.hasPlayerItem(player, condition.getStack());
            case INGREDIENT -> ResearchUtils.hasPlayerIngredient(player, condition.getIngredient());
            case STAGE -> ResearchUtils.hasStage(condition.getValue());
            case CUSTOM -> true;
        };
    }

    protected static boolean containsResearch(GprtTests.BuildData data, String researchKey) {
        for (dev.sixik.gprt.api.ResearchDefinition definition : data.definitions()) {
            if (researchKey.equals(definition.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static float estimateTitleUnits(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return 0f;
        }
        return text.length();
    }

    private static float estimateDescriptionUnits(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return 0f;
        }
        return text.length();
    }
}
