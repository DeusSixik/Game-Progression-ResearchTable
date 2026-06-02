package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeBuild;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Debug-screen with a live example of the optional auto-layout system.
 * <p>
 * Navigation through the class:
 * 1. {@link #createView()} - entry point that returns the root container for the screen.
 * 2. {@link Main} - outer container that holds the graph itself and an overlay panel with controls.
 * 3. {@link #ResearchTreeScreenDebug()} - creates the example graph, enables batching and turns on auto-layout.
 * 4. {@link #seedExampleTree()} - fills the graph with demo nodes/links to show the default dependency layout.
 * 5. {@link #createNode(float, float)} / {@link #createNodeWithLink(int, float, float)} - tiny helpers for graph construction.
 * 6. {@link #createNodeWidget(ResearchNode)} - shows how a graph node is converted into a visible UI widget.
 * <p>
 * What this demo shows in practice:
 * - auto-layout stays optional and can be turned on only for screens that need it;
 * - by default the dependency graph is laid out from left to right;
 * - click on an available node marks it as studied and reveals deeper research;
 * - begin/end batch prevents repeated rebuilds while many nodes and links are inserted;
 * - node widgets still use absolute world coordinates, but those coordinates are now written by the layout utility.
 */
public class ResearchTreeScreenDebug extends ResearchTreeScreen {
    private static final String ROOT_KEY = "primitive_tools";
    private static final String ROOT_GROUP_ID = "root";
    private static final String METALLURGY_GROUP_ID = "metallurgy";
    private static final String FARMING_GROUP_ID = "farming";
    private static final String LOGISTICS_GROUP_ID = "logistics";
    private final Int2ObjectOpenHashMap<Button> nodeButtonsById = new Int2ObjectOpenHashMap<>();
    private int rootNodeId = -1;

    public static UIElement createView() {
        return new Main();
    }

    private static class Main extends UIElement {
        public Main() {
            ResearchTreeScreenDebug graph = new ResearchTreeScreenDebug();

            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));

            addChildren(graph, createOverlay(graph));
        }

        private static UIElement createOverlay(ResearchTreeScreenDebug graph) {
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
                    new Button().setText("Fit").setOnClick(event -> graph.fitToChildren(80f, 0.35f)),
                    new Button().setText("Center Root").setOnClick(event -> graph.centerRoot()),
                    new Button().setText("Reset Demo").setOnClick(event -> graph.resetDemoProgress())
            );

            groupButtons.addChildren(
                    new Button().setText("Root").setOnClick(event -> graph.focusGroup(ROOT_GROUP_ID)),
                    new Button().setText("Metallurgy").setOnClick(event -> graph.focusGroup(METALLURGY_GROUP_ID)),
                    new Button().setText("Farming").setOnClick(event -> graph.focusGroup(FARMING_GROUP_ID)),
                    new Button().setText("Logistics").setOnClick(event -> graph.focusGroup(LOGISTICS_GROUP_ID)),
                    new Button().setText("Clear Mark").setOnClick(event -> graph.clearGroupFocus())
            );

            panel.addChildren(
                    new Label().setText(Component.literal("ResearchTree progression demo")),
                    new Label().setText(Component.literal("Default preset: dependency tree flows from left to right.")),
                    new Label().setText(Component.literal("Click an available node to mark it studied and reveal children.")),
                    new Label().setText(Component.literal("Choose a branch once to center it, click it again to zoom into it.")),
                    new Label().setText(Component.literal("Node fill keeps branch color; cross-branch links become gradients.")),
                    cameraButtons,
                    groupButtons
            );

            return panel;
        }
    }

    public ResearchTreeScreenDebug() {
        autoLayoutConfig()
                .origin(0f, 0f)
                .horizontalGap(130f)
                .verticalGap(42f);

        // Keep the current zoom when a node is studied and the tree rebuilds.
        // Initial framing is still handled by the base GraphView one-time fit pass.
        setAutoLayoutAutoFit(false);
        beginAutoLayoutBatch();
        try {
            seedExampleTree();
            resetDemoProgress();
        } finally {
            endAutoLayoutBatch();
        }
        setAutoLayoutEnabled(true);
    }

    private void seedExampleTree() {
        ResearchTreeBuild build = ResearchTreeBuild.create();

        ResearchGroup rootGroup = build.group(ROOT_GROUP_ID, "Root", 0xFFD0D5DD);
        ResearchGroup metallurgyGroup = build.group(METALLURGY_GROUP_ID, "Metallurgy", 0xFFE29A47, 0xFFF0C17C);
        ResearchGroup farmingGroup = build.group(FARMING_GROUP_ID, "Farming", 0xFF54B36B, 0xFF87D99C);
        ResearchGroup logisticsGroup = build.group(LOGISTICS_GROUP_ID, "Logistics", 0xFF4C90E8, 0xFF81B7FF);

        build.node(ROOT_KEY)
                .title("Primitive Tools")
                .group(rootGroup)
                .visibility(ResearchNode.VisibilityMode.ALWAYS_VISIBLE);

        build.node("metallurgy")
                .title("Metallurgy")
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("farming")
                .title("Farming")
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("logistics")
                .title("Logistics")
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("alloying").title("Alloying").group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("metallurgy");
        build.node("steel").title("Steel").group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("metallurgy");
        build.node("irrigation").title("Irrigation").group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("farming");
        build.node("breeding").title("Breeding").group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("farming");
        build.node("carts").title("Carts").group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("logistics");
        build.node("storage").title("Storage").group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("logistics");

        build.node("steam").title("Steam").group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("alloying");
        build.node("chemistry").title("Chemistry").group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("alloying");
        build.node("machines").title("Machines").group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("steel", "chemistry", "steam");
        build.node("greenhouses").title("Greenhouses").group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("irrigation");
        build.node("food_processing").title("Food Processing").group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("breeding", "greenhouses");
        build.node("rail").title("Rail").group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("carts", "machines");
        build.node("warehouse").title("Warehouse").group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("storage", "machines", "food_processing");

        ResearchTreeBuild.BuildResult buildResult = build.applyTo(this);
        rootNodeId = buildResult.nodeId(ROOT_KEY);
    }

    private void resetDemoProgress() {
        for (ResearchNode node : nodes) {
            node.setStudied(false);
        }

        ResearchNode root = getNodeById(rootNodeId);
        if (root != null) {
            root.setStudied(false);
        }

        refreshResearchProgression();
    }

    @Override
    protected @Nullable UIElement createNodeWidget(ResearchNode node) {
        Button nodeButton = new Button();
        nodeButtonsById.put(node.getId(), nodeButton);
        applyNodeButtonState(nodeButton, node);
        nodeButton.setOnClick(event -> {
            if (!node.isStudied() && isNodeUnlockedForStudy(node.getId())) {
                setNodeStudied(node.getId(), true);
            }
            centerCameraOn(node.getId());
        });
        nodeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(node.getX())
                .top(node.getY())
                .width(node.getWidth())
                .height(node.getHeight())
        );
        return nodeButton;
    }

    @Override
    protected void onResearchProgressionUpdated() {
        for (ResearchNode node : nodes) {
            Button nodeButton = nodeButtonsById.get(node.getId());
            if (nodeButton != null) {
                applyNodeButtonState(nodeButton, node);
            }
        }
    }

    private void centerRoot() {
        if (rootNodeId >= 0) {
            centerCameraOn(rootNodeId);
        }
    }

    private void focusGroup(String groupId) {
        boolean zoomToGroup = groupId.equals(getHighlightedGroupId());
        focusGroup(groupId, zoomToGroup);
    }

    private void clearGroupFocus() {
        clearHighlightedGroup();
    }

    private void applyNodeButtonState(Button nodeButton, ResearchNode node) {
        boolean studied = node.isStudied();
        boolean unlocked = isNodeUnlockedForStudy(node.getId());

        String stateText = studied ? "DONE" : (unlocked ? "OPEN" : "LOCK");
        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        nodeButton.setText(stateText + " | " + title);

        ResearchLinkRenderState renderState = studied
                ? ResearchLinkRenderState.STUDIED
                : (unlocked ? ResearchLinkRenderState.AVAILABLE : ResearchLinkRenderState.LOCKED);
        int backgroundColor = applyLinkRenderStateColor(node.getGroupColor(), renderState);
        nodeButton.style(style -> style.backgroundTexture(new ColorRectTexture(backgroundColor)));
    }
}
