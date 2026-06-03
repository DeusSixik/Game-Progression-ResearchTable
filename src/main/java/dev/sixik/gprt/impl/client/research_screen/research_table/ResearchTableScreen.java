package dev.sixik.gprt.impl.client.research_screen.research_table;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.sixik.gprt.api.ResearchCondition;
import dev.sixik.gprt.api.ResearchDefinition;
import dev.sixik.gprt.api.ResearchGroupDefinition;
import dev.sixik.gprt.api.ResearchReward;
import dev.sixik.gprt.impl.client.research_screen.demo.DebugResearchNodeWidgetShowcase;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeBuild;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreenMainScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContent;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelWidget;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.sixik.gprt.impl.client.research_screen.research_table.info.TableInfoPanelWidget;
import dev.sixik.gprt.test.GprtTests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResearchTableScreen extends ResearchTreeScreenMainScreen {

    private static final String DEFAULT_ROOT_KEY = "primitive_tools";
    private final Map<String, dev.sixik.gprt.api.ResearchDefinition> researchDefinitionsByKey = new LinkedHashMap<>();

    public ResearchTableScreen() {
        autoLayoutConfig()
                .origin(0f, 0f)
                .horizontalGap(130f)
                .verticalGap(42f);
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
        return DebugResearchNodeWidgetShowcase.createWidgetFactory(() -> DebugResearchNodeWidgetShowcase.StyleMode.TECH_CARDS);
    }

    /*@Override
    protected void configureNodeThemePresets(ResearchNodeGroupThemeResolver.Builder builder) {
        DebugResearchNodeWidgetShowcase.configureThemePresets(
                builder,
                ROOT_GROUP,
                METALLURGY_GROUP,
                FARMING_GROUP,
                LOGISTICS_GROUP
        );
    }*/

    @Override
    protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state) {
        ResearchInfoContent.Builder builder = createStandardInfoContentBuilder(node, state).build().toBuilder();

        ResearchDefinition data = researchDefinitionsByKey.get(node.getResearchKey());
        if(data != null) {


            List<ResearchCondition> conditions = data.getConditions();
            if(conditions != null && !conditions.isEmpty()) {

                //TODO: Conditions complete detected
                builder.section("Conditions", section -> {
                    for (ResearchCondition condition : conditions) {
                        boolean isCompleted = false;

                        switch (condition.getKind()) {
                            case ITEM -> section.conditionItem(condition.getStack().getDisplayName().getString(), condition.getStack(), isCompleted);
                            case INGREDIENT -> section.condition(entry -> entry
                                    .ingredient(condition.getIngredient())
                                    .completed(isCompleted)
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

    private boolean isNodeStudiedByKey(String researchKey) {
        for (ResearchNode node : nodes) {
            if (researchKey.equals(node.getResearchKey())) {
                return node.isStudied();
            }
        }
        return false;
    }

    private boolean areResearchConditionsMet(ResearchNode node) {
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

    private boolean isConditionMet(LocalPlayer player, ResearchCondition condition) {
        if (condition == null) {
            return true;
        }

        return switch (condition.getKind()) {
            case ITEM -> inventoryContainsExactStack(player, condition.getStack());
            case INGREDIENT -> inventoryContainsIngredient(player, condition.getIngredient());
            case CUSTOM -> true;
        };
    }

    private boolean inventoryContainsExactStack(LocalPlayer player, ItemStack requiredStack) {
        if (requiredStack.isEmpty()) {
            return true;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(stack, requiredStack) && stack.getCount() >= requiredStack.getCount()) {
                return true;
            }
        }
        return false;
    }

    private boolean inventoryContainsIngredient(LocalPlayer player, Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return true;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsResearch(GprtTests.BuildData data, String researchKey) {
        for (dev.sixik.gprt.api.ResearchDefinition definition : data.definitions()) {
            if (researchKey.equals(definition.getKey())) {
                return true;
            }
        }
        return false;
    }
}
