package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
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
    private static final float DETAILS_PANEL_WIDTH = 300f;
    private static final float DETAILS_PANEL_HIDDEN_X = -(DETAILS_PANEL_WIDTH + 18f);
    private static final float DETAILS_PANEL_OPEN_X = 0f;
    private static final float DETAILS_PANEL_LERP_SPEED = 0.22f;
    private final Int2ObjectOpenHashMap<Button> nodeButtonsById = new Int2ObjectOpenHashMap<>();
    private @Nullable UIElement detailsPanel;
    private @Nullable Label detailsTitleLabel;
    private @Nullable Label detailsGroupLabel;
    private @Nullable Label detailsStateLabel;
    private @Nullable Label detailsDescriptionLabel;
    private @Nullable Button researchButton;
    private int selectedNodeId = -1;
    private float detailsPanelProgress;
    private float detailsPanelTargetProgress;
    private int rootNodeId = -1;

    private UIElement helpPanel;

    public static UIElement createView() {
        return new Main();
    }

    private static class Main extends UIElement {
        public Main() {
            ResearchTreeScreenDebug graph = new ResearchTreeScreenDebug();

            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));

            addChildren(graph, createOverlay(graph), createDetailsPanel(graph));
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

            graph.helpPanel = panel;

            return panel;
        }

        private static UIElement createDetailsPanel(ResearchTreeScreenDebug graph) {
            UIElement panel = new UIElement()
                    .layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(8)
                            .top(118)
                            .width(DETAILS_PANEL_WIDTH)
                            .paddingAll(8)
                            .gapAll(5)
                    )
                    .style(style -> style
                            .backgroundTexture(new ColorRectTexture(0xE6192432))
                            .transform2D(new Transform2D().translate(DETAILS_PANEL_HIDDEN_X, 0f)));

            Label titleLabel = new Label();
            titleLabel.setText(Component.literal("Research"));
            Label groupLabel = new Label();
            groupLabel.setText(Component.literal("Group: -"));
            Label stateLabel = new Label();
            stateLabel.setText(Component.literal("Status: -"));
            Label descriptionLabel = new Label();
            descriptionLabel.setText(Component.literal("Click a research node to open its info panel."));
            descriptionLabel.layout(layout -> layout.widthPercent(100));
            descriptionLabel.textStyle(style -> style
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true));
            Button closeButton = new Button().setText("X").setOnClick(event -> graph.closeDetailsPanel());
            closeButton.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(264)
                    .top(6)
                    .width(24)
                    .height(18)
            );
            Button research = new Button().setText("Research")
                    .setOnClick(event -> {
                        if(graph.selectedNodeId != -1) {
                            graph.setNodeStudied(graph.selectedNodeId, true);

                            if(graph.researchButton != null)
                                graph.researchButton.setDisplay(false);

                        }
                    });

            panel.addChildren(titleLabel, groupLabel, stateLabel, descriptionLabel, research, closeButton);
            graph.bindDetailsPanel(panel, titleLabel, groupLabel, stateLabel, descriptionLabel, research);
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
                .description("Basic survival know-how. Opens the first major directions of technological progress.")
                .group(rootGroup)
                .visibility(ResearchNode.VisibilityMode.ALWAYS_VISIBLE);

        build.node("metallurgy")
                .title("Metallurgy")
                .description("The first step into furnaces, ore treatment and controlled metalworking.")
                .group(metallurgyGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("farming")
                .title("Farming")
                .description("Organized food production with better crop planning and field management.")
                .group(farmingGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("logistics")
                .title("Logistics")
                .description("The branch focused on moving, storing and routing materials efficiently.")
                .group(logisticsGroup)
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn(ROOT_KEY);

        build.node("alloying").title("Alloying").group(metallurgyGroup)
                .description("Combining metals to unlock stronger and more specialized materials.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("metallurgy");
        build.node("steel").title("Steel").group(metallurgyGroup)
                .description("A major leap in structural materials and a base for advanced machinery.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("metallurgy");
        build.node("irrigation").title("Irrigation").group(farmingGroup)
                .description("Water control that stabilizes crop growth and supports larger harvests.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("farming");
        build.node("breeding").title("Breeding").group(farmingGroup)
                .description("Improved livestock management for better quality and larger output.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("farming");
        build.node("carts").title("Carts").group(logisticsGroup)
                .description("Simple vehicle transport for moving resources between work areas.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("logistics");
        build.node("storage").title("Storage").group(logisticsGroup)
                .description("Bigger and better-organized stockpiles for a growing production chain.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("logistics");

        build.node("steam").title("Steam").group(metallurgyGroup)
                .description("Pressure, heat and primitive power systems for the first automation steps.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("alloying");
        build.node("chemistry").title("Chemistry").group(metallurgyGroup)
                .description("Controlled reactions and refined processing methods for complex production.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("alloying");
        build.node("machines").title("Machines").group(metallurgyGroup)
                .description("Industrial assembly that combines metallurgy, power and precision work.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("steel", "chemistry", "steam");
        build.node("greenhouses").title("Greenhouses").group(farmingGroup)
                .description("Protected growing spaces that improve consistency and yield.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ANY_PARENT_STUDIED)
                .dependsOn("irrigation");
        build.node("food_processing").title("Food Processing").group(farmingGroup)
                .description("Turning raw farm output into preserved, efficient and higher-value food.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("breeding", "greenhouses");
        build.node("rail").title("Rail").group(logisticsGroup)
                .description("Heavy transport infrastructure for reliable movement over distance.")
                .visibility(ResearchNode.VisibilityMode.REQUIRE_ALL_PARENTS_STUDIED)
                .dependsOn("carts", "machines");
        build.node("warehouse").title("Warehouse").group(logisticsGroup)
                .description("A centralized logistics hub for large-scale item flow and storage.")
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
            if (isRevealSequenceActive()) {
                return;
            }
            if(node.isStudied() && researchButton != null)
                researchButton.setDisplay(false);
            else if(!node.isStudied() && researchButton != null)
                researchButton.setDisplay(true);
            openDetailsPanel(node.getId());

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
        refreshDetailsPanel();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateDetailsPanelAnimation();
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

    private void bindDetailsPanel(UIElement panel,
                                  Label titleLabel,
                                  Label groupLabel,
                                  Label stateLabel,
                                  Label descriptionLabel,
                                  Button researchButton
    ) {
        this.detailsPanel = panel;
        this.detailsTitleLabel = titleLabel;
        this.detailsGroupLabel = groupLabel;
        this.detailsStateLabel = stateLabel;
        this.detailsDescriptionLabel = descriptionLabel;
        this.researchButton = researchButton;
        refreshDetailsPanel();
        updateDetailsPanelAnimation();
    }

    private void openDetailsPanel(int nodeId) {
        selectedNodeId = nodeId;

        if (detailsPanel != null) {
            detailsPanel.setDisplay(true);
        }
        helpPanel.setDisplay(false);
        detailsPanelTargetProgress = 1f;
        refreshDetailsPanel();
    }

    private void closeDetailsPanel() {
        detailsPanelTargetProgress = 0f;
    }

    private void refreshDetailsPanel() {
        if (detailsTitleLabel == null || detailsGroupLabel == null || detailsStateLabel == null || detailsDescriptionLabel == null) {
            return;
        }

        ResearchNode node = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (node == null) {
            detailsTitleLabel.setText("Research");
            detailsGroupLabel.setText("Group: -");
            detailsStateLabel.setText("Status: -");
            detailsDescriptionLabel.setText("Click a research node to open its info panel.");
            return;
        }

        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        String group = node.getGroup() != null ? node.getGroup().getTitle() : "Unknown";
        String status = node.isStudied() ? "Studied" : (isNodeUnlockedForStudy(node.getId()) ? "Available" : "Locked");
        String description = node.getDescription();
        if (description == null || description.isBlank()) {
            description = "No description has been assigned to this research yet.";
        }

        detailsTitleLabel.setText(title);
        detailsGroupLabel.setText("Group: " + group);
        detailsStateLabel.setText("Status: " + status);
        detailsDescriptionLabel.setText(description);

        if (detailsPanel != null) {
            int panelColor = applyLinkRenderStateColor(node.getGroupColor(), node.isStudied()
                    ? ResearchLinkRenderState.STUDIED
                    : (isNodeUnlockedForStudy(node.getId()) ? ResearchLinkRenderState.AVAILABLE : ResearchLinkRenderState.LOCKED));
            detailsPanel.style(style -> style.backgroundTexture(new ColorRectTexture(0xD0000000 | (panelColor & 0x00FFFFFF))));
        }
    }

    private void updateDetailsPanelAnimation() {
        if (detailsPanel == null) {
            return;
        }

        detailsPanelProgress += (detailsPanelTargetProgress - detailsPanelProgress) * DETAILS_PANEL_LERP_SPEED;
        if (Math.abs(detailsPanelTargetProgress - detailsPanelProgress) < 0.002f) {
            detailsPanelProgress = detailsPanelTargetProgress;
        }

        float translateX = interpolate(detailsPanelProgress, DETAILS_PANEL_HIDDEN_X, DETAILS_PANEL_OPEN_X);
        detailsPanel.style(style -> style.transform2D(new Transform2D().translate(translateX, 0f)));

        if (detailsPanelTargetProgress <= 0f && detailsPanelProgress <= 0f) {
            detailsPanel.setDisplay(false);
            helpPanel.setDisplay(true);
        }
    }

    private float interpolate(float progress, float start, float end) {
        return start + (end - start) * progress;
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
