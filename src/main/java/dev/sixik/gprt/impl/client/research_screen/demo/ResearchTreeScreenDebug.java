package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeBuild;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreenMainScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContent;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchDisplayValue;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

/**
 * Demo implementation of {@link ResearchTreeScreenMainScreen}.
 * <p>
 * After the refactor this class is intentionally small: it only defines demo data and
 * the top-left helper overlay, while shared details panel / timed progress / table
 * placeholder behavior now lives in the reusable main-screen base class.
 * </p>
 */
public final class ResearchTreeScreenDebug extends ResearchTreeScreenMainScreen {
    private static final String ROOT_KEY = "primitive_tools";
    private static final String ROOT_GROUP_ID = "root";
    private static final String METALLURGY_GROUP_ID = "metallurgy";
    private static final String FARMING_GROUP_ID = "farming";
    private static final String LOGISTICS_GROUP_ID = "logistics";

    public ResearchTreeScreenDebug() {
        autoLayoutConfig()
                .origin(0f, 0f)
                .horizontalGap(130f)
                .verticalGap(42f);
        initializeMainScreen();
    }

    @Override
    protected UIElement createOverlayPanel() {
        UIElement panel = new UIElement()
                .layout(layout -> layout
                        .positionType(TaffyPosition.ABSOLUTE)
                        .left(8)
                        .top(8)
                        .width(310)
                        .paddingAll(6)
                        .gapAll(4)
                )
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xCC1A2330)));

        UIElement cameraButtons = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(4));
        UIElement groupButtons = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(4));

        cameraButtons.addChildren(
                new Button().setText("Fit").setOnClick(event -> fitToChildren(80f, 0.35f)),
                new Button().setText("Center Root").setOnClick(event -> centerRootNode()),
                new Button().setText("Reset Demo").setOnClick(event -> resetProgressState())
        );

        groupButtons.addChildren(
                new Button().setText("Root").setOnClick(event -> toggleGroupFocus(ROOT_GROUP_ID)),
                new Button().setText("Metallurgy").setOnClick(event -> toggleGroupFocus(METALLURGY_GROUP_ID)),
                new Button().setText("Farming").setOnClick(event -> toggleGroupFocus(FARMING_GROUP_ID)),
                new Button().setText("Logistics").setOnClick(event -> toggleGroupFocus(LOGISTICS_GROUP_ID)),
                new Button().setText("Clear Mark").setOnClick(event -> clearFocusedGroup())
        );

        panel.addChildren(
                new Label().setText("ResearchTree progression demo"),
                new Label().setText("Shared UI now lives in ResearchTreeScreenMainScreen."),
                new Label().setText("Timed research uses the built-in progress bar and details panel."),
                new Label().setText("Table research reuses the built-in placeholder overlay."),
                new Label().setText("This class now mostly defines data and screen-specific controls."),
                cameraButtons,
                groupButtons
        );

        return panel;
    }

    @Override
    protected void buildResearchTree() {
        ResearchTreeBuild build = ResearchTreeBuild.create();

        ResearchGroup rootGroup = build.group(ROOT_GROUP_ID, "Root", 0xFFD0D5DD);
        ResearchGroup metallurgyGroup = build.group(METALLURGY_GROUP_ID, "Metallurgy", 0xFFE29A47, 0xFFF0C17C);
        ResearchGroup farmingGroup = build.group(FARMING_GROUP_ID, "Farming", 0xFF54B36B, 0xFF87D99C);
        ResearchGroup logisticsGroup = build.group(LOGISTICS_GROUP_ID, "Logistics", 0xFF4C90E8, 0xFF81B7FF);

        build.node(ROOT_KEY)
                .definition(instantDefinition(ROOT_KEY, "Primitive Tools",
                        "Basic survival know-how. Opens the first major directions of technological progress."))
                .group(rootGroup)
                .visibility(ResearchNode.VisibilityMode.ALWAYS_VISIBLE);

        build.node("metallurgy")
                .definition(instantDefinition("metallurgy", "Metallurgy",
                        "The first step into furnaces, ore treatment and controlled metalworking."))
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("farming")
                .definition(instantDefinition("farming", "Farming",
                        "Organized food production with better crop planning and field management."))
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("logistics")
                .definition(instantDefinition("logistics", "Logistics",
                        "The branch focused on moving, storing and routing materials efficiently."))
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("alloying")
                .definition(instantDefinition("alloying", "Alloying",
                        "Combining metals to unlock stronger and more specialized materials."))
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("metallurgy");
        build.node("steel")
                .definition(instantDefinition("steel", "Steel",
                        "A major leap in structural materials and a base for advanced machinery."))
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("metallurgy");
        build.node("irrigation")
                .definition(instantDefinition("irrigation", "Irrigation",
                        "Water control that stabilizes crop growth and supports larger harvests."))
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("farming");
        build.node("breeding")
                .definition(instantDefinition("breeding", "Breeding",
                        "Improved livestock management for better quality and larger output."))
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("farming");
        build.node("carts")
                .definition(instantDefinition("carts", "Carts",
                        "Simple vehicle transport for moving resources between work areas."))
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("logistics");
        build.node("storage")
                .definition(instantDefinition("storage", "Storage",
                        "Bigger and better-organized stockpiles for a growing production chain."))
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("logistics");

        build.node("steam")
                .definition(timedDefinition("steam", "Steam",
                        "Pressure, heat and primitive power systems for the first automation steps.", 9_000L))
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("alloying");
        build.node("chemistry")
                .definition(instantDefinition("chemistry", "Chemistry",
                        "Controlled reactions and refined processing methods for complex production."))
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("alloying");
        build.node("machines")
                .definition(instantDefinition("machines", "Machines",
                        "Industrial assembly that combines metallurgy, power and precision work."))
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("steel", "chemistry", "steam");
        build.node("greenhouses")
                .definition(instantDefinition("greenhouses", "Greenhouses",
                        "Protected growing spaces that improve consistency and yield."))
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("irrigation");
        build.node("food_processing")
                .definition(instantDefinition("food_processing", "Food Processing",
                        "Turning raw farm output into preserved, efficient and higher-value food."))
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("breeding", "greenhouses");
        build.node("rail")
                .definition(instantDefinition("rail", "Rail",
                        "Heavy transport infrastructure for reliable movement over distance."))
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("carts", "machines");
        build.node("warehouse")
                .definition(tableDefinition("warehouse", "Warehouse",
                        "A centralized logistics hub for large-scale item flow and storage."))
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("storage", "machines", "food_processing");

        ResearchTreeBuild.BuildResult buildResult = build.applyTo(this);
        setRootNodeId(buildResult.nodeId(ROOT_KEY));
    }

    @Override
    protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state) {
        ResearchInfoContent.Builder builder = super.buildInfoContent(node, state).toBuilder();

        builder.section("Debug Notes", section -> section
                .infoLine("Branch", node.getGroup() != null ? node.getGroup().getId() : "unknown")
        );

        if ("steam".equals(node.getResearchKey())) {
            builder.section("Example Rewards", section -> {
                section.rewardItem(new ItemStack(Items.COPPER_INGOT, 3), "Prototype bronze fittings");
                section.rewardItem(Blocks.BLAST_FURNACE, "Heavy furnace branch support");
                section.rewardIngredient("Any fuel source", Ingredient.of(Items.COAL, Items.CHARCOAL));
                section.rewardItemId("minecraft:diamond");
            });
        }

        if ("warehouse".equals(node.getResearchKey())) {
            builder.section("Example Table Flow", section -> {
                section.conditionText("Example table step UI will appear after pressing research", true);
                section.condition(entry -> entry
                        .item(new ItemStack(Items.CHEST))
                        .text("Requires Storage")
                        .completed(isNodeStudiedByKey("storage"))
                        .tooltip("This row uses icon + text + tooltip + research jump.")
                        .jumpToResearch("storage")
                        .jumpButtonText("Open"));
            });
        }

        if (state == ResearchState.IN_PROGRESS && node.getStudyType() == ResearchStudyType.TIMED) {
            builder.section("Live State", section -> section
                    .entry(entry -> entry
                            .text("Timed progress is mirrored in this panel")
                            .tooltip("This note is only added while timed research is active."))
            );
        }

        return builder.build();
    }

    private boolean isNodeStudiedByKey(String researchKey) {
        for (ResearchNode node : nodes) {
            if (researchKey.equals(node.getResearchKey())) {
                return node.isStudied();
            }
        }
        return false;
    }

    private static ResearchDefinition instantDefinition(String key, String title, String description) {
        return ResearchDefinition.builder(key)
                .title(title)
                .description(description)
                .studyType(ResearchStudyType.INSTANT)
                .build();
    }

    private static ResearchDefinition timedDefinition(String key, String title, String description, long durationMs) {
        return ResearchDefinition.builder(key)
                .title(title)
                .description(description)
                .timed(durationMs)
                .build();
    }

    private static ResearchDefinition tableDefinition(String key, String title, String description) {
        return ResearchDefinition.builder(key)
                .title(title)
                .description(description)
                .studyType(ResearchStudyType.TABLE)
                .build();
    }
}
