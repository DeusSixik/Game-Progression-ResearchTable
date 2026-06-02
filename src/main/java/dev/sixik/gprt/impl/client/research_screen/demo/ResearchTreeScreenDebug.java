package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchGroup;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeBuild;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchProgressController;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.SimpleClientResearchProgressManager;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * Debug screen for the research tree.
 * <p>
 * This screen now demonstrates three important layers at once:
 * <ul>
 *     <li>declarative tree construction through {@link ResearchTreeBuild};</li>
 *     <li>client-only progression state through {@link SimpleClientResearchProgressManager};</li>
 *     <li>details UI that reacts differently to instant, timed and table research entries.</li>
 * </ul>
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #createView()} - creates the root UI object for the whole demo;</li>
 *     <li>{@link #seedExampleTree()} - defines the example research graph and its metadata;</li>
 *     <li>{@link #tryStartResearch(ResearchNode)} - unified entry point for all research modes;</li>
 *     <li>{@link #refreshDetailsPanel()} - updates title, state, buttons and timed progress text;</li>
 *     <li>{@link #refreshTimedProgressSection(ResearchNode, ResearchState)} - draws the live timed progress bar;</li>
 *     <li>{@link #openTablePlaceholderOverlay(ResearchNode)} - temporary table research placeholder UI.</li>
 * </ul>
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
    private static final float DETAILS_PROGRESS_BAR_WIDTH = DETAILS_PANEL_WIDTH - 16f;
    private static final float DETAILS_PROGRESS_BAR_HEIGHT = 8f;

    private final Int2ObjectOpenHashMap<Button> nodeButtonsById = new Int2ObjectOpenHashMap<>();
    private final SimpleClientResearchProgressManager researchProgressManager = new SimpleClientResearchProgressManager();
    private final ResearchProgressController researchProgressController = new ResearchProgressController(researchProgressManager);

    private @Nullable UIElement detailsPanel;
    private @Nullable Label detailsTitleLabel;
    private @Nullable Label detailsGroupLabel;
    private @Nullable Label detailsModeLabel;
    private @Nullable Label detailsStateLabel;
    private @Nullable Label detailsDescriptionLabel;
    private @Nullable Label detailsTimedProgressLabel;
    private @Nullable UIElement detailsTimedProgressBar;
    private @Nullable UIElement detailsTimedProgressFill;
    private @Nullable Button researchButton;

    private @Nullable ResearchTablePlaceholderOverlay tablePlaceholderOverlay;

    private @Nullable UIElement helpPanel;
    private int selectedNodeId = -1;
    private float detailsPanelProgress;
    private float detailsPanelTargetProgress;
    private int rootNodeId = -1;
    private boolean tablePlaceholderVisible;

    public static UIElement createView() {
        return new Main();
    }

    private static final class Main extends UIElement {
        private Main() {
            ResearchTreeScreenDebug graph = new ResearchTreeScreenDebug();

            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));

            addChildren(
                    graph,
                    createOverlay(graph),
                    createDetailsPanel(graph),
                    createTablePlaceholderOverlay(graph)
            );
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
                    new Label().setText("ResearchTree progression demo"),
                    new Label().setText("Timed research now has a live progress bar in the details panel."),
                    new Label().setText("Table research opens a placeholder overlay until the real table screen exists."),
                    new Label().setText("Choose a branch once to center it, click it again to zoom into it."),
                    new Label().setText("Node fill keeps branch color; cross-branch links become gradients."),
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
            titleLabel.setText("Research");
            Label groupLabel = new Label();
            groupLabel.setText("Group: -");
            Label modeLabel = new Label();
            modeLabel.setText("Mode: -");
            Label stateLabel = new Label();
            stateLabel.setText("Status: -");

            Label descriptionLabel = new Label();
            descriptionLabel.setText("Click a research node to open its info panel.");
            descriptionLabel.layout(layout -> layout.widthPercent(100));
            descriptionLabel.textStyle(style -> style
                    .textWrap(TextWrap.WRAP)
                    .adaptiveHeight(true));

            Label timedProgressLabel = new Label();
            timedProgressLabel.setText("Progress: -");
            UIElement timedProgressBar = new UIElement()
                    .layout(layout -> layout.width(DETAILS_PROGRESS_BAR_WIDTH).height(DETAILS_PROGRESS_BAR_HEIGHT))
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0x55232D39)));
            UIElement timedProgressFill = new UIElement()
                    .layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(0)
                            .top(0)
                            .width(0)
                            .height(DETAILS_PROGRESS_BAR_HEIGHT)
                    )
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF67B7FF)));
            timedProgressBar.addChildren(timedProgressFill);

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
                        if (graph.selectedNodeId != -1) {
                            ResearchNode node = graph.getNodeById(graph.selectedNodeId);
                            if (node != null) {
                                graph.tryStartResearch(node);
                            }
                        }
                    });

            panel.addChildren(
                    titleLabel,
                    groupLabel,
                    modeLabel,
                    stateLabel,
                    descriptionLabel,
                    timedProgressLabel,
                    timedProgressBar,
                    research,
                    closeButton
            );

            graph.bindDetailsPanel(
                    panel,
                    titleLabel,
                    groupLabel,
                    modeLabel,
                    stateLabel,
                    descriptionLabel,
                    timedProgressLabel,
                    timedProgressBar,
                    timedProgressFill,
                    research
            );
            return panel;
        }

        private static UIElement createTablePlaceholderOverlay(ResearchTreeScreenDebug graph) {
            ResearchTablePlaceholderOverlay overlay = new ResearchTablePlaceholderOverlay(
                    graph::completeTablePlaceholderResearch,
                    graph::cancelTablePlaceholderResearch,
                    graph::closeTablePlaceholderOverlay
            );
            graph.bindTablePlaceholderOverlay(overlay);
            return overlay;
        }
    }

    public ResearchTreeScreenDebug() {
        autoLayoutConfig()
                .origin(0f, 0f)
                .horizontalGap(130f)
                .verticalGap(42f);

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
        rootNodeId = buildResult.nodeId(ROOT_KEY);
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

    private void resetDemoProgress() {
        closeTablePlaceholderOverlay();
        researchProgressController.reset();
        for (ResearchNode node : nodes) {
            node.setStudied(false);
        }
        refreshResearchProgression();
    }

    @Override
    public ResearchTreeScreen setNodeStudied(int nodeId, boolean studied) {
        ResearchNode node = getNodeById(nodeId);
        if (node != null) {
            if (studied) {
                researchProgressController.tryCompleteResearch(node);
            } else {
                researchProgressController.tryCancelResearch(node);
            }
        }
        return super.setNodeStudied(nodeId, studied);
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
        syncProgressManagerUnlockedState();
        for (ResearchNode node : nodes) {
            Button nodeButton = nodeButtonsById.get(node.getId());
            if (nodeButton != null) {
                applyNodeButtonState(nodeButton, node);
            }
        }
        refreshDetailsPanel();
        refreshTablePlaceholderOverlay();
    }

    @Override
    public void screenTick() {
        super.screenTick();

        long nowMs = System.currentTimeMillis();
        if (researchProgressManager.update(nowMs)) {
            applyCompletedResearchFromManager();
        }

        refreshRealtimeResearchUi(nowMs);
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
                                  Label modeLabel,
                                  Label stateLabel,
                                  Label descriptionLabel,
                                  Label timedProgressLabel,
                                  UIElement timedProgressBar,
                                  UIElement timedProgressFill,
                                  Button researchButton
    ) {
        this.detailsPanel = panel;
        this.detailsTitleLabel = titleLabel;
        this.detailsGroupLabel = groupLabel;
        this.detailsModeLabel = modeLabel;
        this.detailsStateLabel = stateLabel;
        this.detailsDescriptionLabel = descriptionLabel;
        this.detailsTimedProgressLabel = timedProgressLabel;
        this.detailsTimedProgressBar = timedProgressBar;
        this.detailsTimedProgressFill = timedProgressFill;
        this.researchButton = researchButton;
        refreshDetailsPanel();
        updateDetailsPanelAnimation();
    }

    private void bindTablePlaceholderOverlay(ResearchTablePlaceholderOverlay overlay) {
        this.tablePlaceholderOverlay = overlay;
        refreshTablePlaceholderOverlay();
    }

    private void tryStartResearch(ResearchNode node) {
        String researchKey = node.getResearchKey();
        if (researchKey == null || isRevealSequenceActive()) {
            return;
        }

        ResearchState currentState = researchProgressController.getState(node);
        if (currentState == ResearchState.IN_PROGRESS && node.getStudyType() == ResearchStudyType.TABLE) {
            openTablePlaceholderOverlay(node);
            refreshDetailsPanel();
            return;
        }

        boolean started = researchProgressController.tryStartResearch(node, System.currentTimeMillis());
        if (!started) {
            refreshDetailsPanel();
            return;
        }

        if (node.getStudyType() == ResearchStudyType.TABLE) {
            openTablePlaceholderOverlay(node);
        }

        if (researchProgressController.isStudied(node)) {
            setNodeStudied(node.getId(), true);
        } else {
            Button nodeButton = nodeButtonsById.get(node.getId());
            if (nodeButton != null) {
                applyNodeButtonState(nodeButton, node);
            }
            refreshDetailsPanel();
            refreshTablePlaceholderOverlay();
        }
    }

    private void applyCompletedResearchFromManager() {
        var completedIds = researchProgressController.drainCompletedResearchIds();
        for (int i = 0, size = completedIds.size(); i < size; i++) {
            ResearchNode node = getNodeByResearchKey(completedIds.get(i));
            if (node != null && !node.isStudied()) {
                super.setNodeStudied(node.getId(), true);
            }
        }
    }

    private void syncProgressManagerUnlockedState() {
        for (ResearchNode node : nodes) {
            String researchKey = node.getResearchKey();
            if (researchKey != null) {
                researchProgressController.setUnlocked(node, isNodeUnlockedForStudy(node.getId()));
            }
        }
    }

    private void refreshRealtimeResearchUi(long nowMs) {
        ResearchNode selectedNode = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (selectedNode != null) {
            if (researchProgressController.isInProgress(selectedNode)) {
                refreshDetailsPanel();
            }
        }

        ResearchNode tableNode = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (tablePlaceholderOverlay != null
                && tablePlaceholderVisible
                && tableNode != null
                && tableNode.getStudyType() == ResearchStudyType.TABLE) {
            refreshTablePlaceholderOverlay();
        }
    }

    private @Nullable ResearchNode getNodeByResearchKey(String researchKey) {
        for (ResearchNode node : nodes) {
            if (researchKey.equals(node.getResearchKey())) {
                return node;
            }
        }
        return null;
    }

    private void openDetailsPanel(int nodeId) {
        selectedNodeId = nodeId;
        if (detailsPanel != null) {
            detailsPanel.setDisplay(true);
        }
        if (helpPanel != null) {
            helpPanel.setDisplay(false);
        }
        detailsPanelTargetProgress = 1f;
        refreshDetailsPanel();
    }

    private void closeDetailsPanel() {
        detailsPanelTargetProgress = 0f;
    }

    private void openTablePlaceholderOverlay(ResearchNode node) {
        selectedNodeId = node.getId();
        tablePlaceholderVisible = true;
        if (tablePlaceholderOverlay != null) {
            tablePlaceholderOverlay.openFor(node, resolveNodeState(node));
        }
    }

    private void closeTablePlaceholderOverlay() {
        tablePlaceholderVisible = false;
        if (tablePlaceholderOverlay != null) {
            tablePlaceholderOverlay.hideOverlay();
        }
    }

    private void completeTablePlaceholderResearch() {
        ResearchNode node = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (node == null) {
            return;
        }

        String researchKey = node.getResearchKey();
        if (researchKey == null) {
            return;
        }

        if (researchProgressController.tryCompleteResearch(node)) {
            closeTablePlaceholderOverlay();
            applyCompletedResearchFromManager();
            refreshDetailsPanel();
        }
    }

    private void cancelTablePlaceholderResearch() {
        ResearchNode node = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (node == null) {
            return;
        }

        String researchKey = node.getResearchKey();
        if (researchKey == null) {
            return;
        }

        if (researchProgressController.tryCancelResearch(node)) {
            Button nodeButton = nodeButtonsById.get(node.getId());
            if (nodeButton != null) {
                applyNodeButtonState(nodeButton, node);
            }
            closeTablePlaceholderOverlay();
            refreshDetailsPanel();
        }
    }

    private void refreshDetailsPanel() {
        if (detailsTitleLabel == null
                || detailsGroupLabel == null
                || detailsModeLabel == null
                || detailsStateLabel == null
                || detailsDescriptionLabel == null) {
            return;
        }

        ResearchNode node = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (node == null) {
            detailsTitleLabel.setText("Research");
            detailsGroupLabel.setText("Group: -");
            detailsModeLabel.setText("Mode: -");
            detailsStateLabel.setText("Status: -");
            detailsDescriptionLabel.setText("Click a research node to open its info panel.");
            if (researchButton != null) {
                researchButton.setDisplay(false);
            }
            hideTimedProgressSection();
            return;
        }

        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        String group = node.getGroup() != null ? node.getGroup().getTitle() : "Unknown";
        ResearchState state = researchProgressController.getState(node);
        String description = node.getDescription();
        if (description == null || description.isBlank()) {
            description = "No description has been assigned to this research yet.";
        }

        detailsTitleLabel.setText(title);
        detailsGroupLabel.setText("Group: " + group);
        detailsModeLabel.setText("Mode: " + formatStudyType(node));
        detailsStateLabel.setText("Status: " + formatStateText(state, node));
        detailsDescriptionLabel.setText(description);

        refreshTimedProgressSection(node, state);
        updateResearchButton(node, state);

        if (detailsPanel != null) {
            int panelColor = applyLinkRenderStateColor(node.getGroupColor(), toRenderState(state));
            detailsPanel.style(style -> style.backgroundTexture(new ColorRectTexture(0xD0000000 | (panelColor & 0x00FFFFFF))));
        }
    }

    private void refreshTablePlaceholderOverlay() {
        if (tablePlaceholderOverlay == null) {
            return;
        }

        if (!tablePlaceholderVisible) {
            return;
        }

        ResearchNode node = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (node == null || node.getStudyType() != ResearchStudyType.TABLE) {
            tablePlaceholderOverlay.showEmpty();
            return;
        }

        tablePlaceholderOverlay.updateFor(node, researchProgressController.getState(node));
    }

    private void refreshTimedProgressSection(ResearchNode node, ResearchState state) {
        if (detailsTimedProgressLabel == null || detailsTimedProgressBar == null || detailsTimedProgressFill == null) {
            return;
        }

        if (node.getStudyType() != ResearchStudyType.TIMED) {
            hideTimedProgressSection();
            return;
        }

        detailsTimedProgressLabel.setDisplay(true);
        detailsTimedProgressBar.setDisplay(true);
        ClientResearchProgress progress = researchProgressController.getProgress(node);

        if (state == ResearchState.STUDIED || state == ResearchState.LOCKED) {
            hideTimedProgressSection();
            return;
        }

        if (progress == null || state != ResearchState.IN_PROGRESS) {
            detailsTimedProgressLabel.setText("Duration: " + formatDuration(node.getStudyDurationMs()));
            updateTimedProgressFill(0f, 0x664C90E8);
            return;
        }

        long nowMs = System.currentTimeMillis();
        long remainingMs = progress.getRemainingMs(nowMs);
        float progress01 = progress.getProgress01(nowMs);

        detailsTimedProgressLabel.setText(
                "Progress: " + Math.round(progress01 * 100f) + "% | left " + formatDuration(remainingMs)
        );
        updateTimedProgressFill(progress01, 0xFF67B7FF);
    }

    private void hideTimedProgressSection() {
        if (detailsTimedProgressLabel != null) {
            detailsTimedProgressLabel.setDisplay(false);
        }
        if (detailsTimedProgressBar != null) {
            detailsTimedProgressBar.setDisplay(false);
        }
        updateTimedProgressFill(0f, 0x664C90E8);
    }

    private void updateTimedProgressFill(float progress01, int fillColor) {
        if (detailsTimedProgressFill == null) {
            return;
        }

        float clamped = Math.max(0f, Math.min(1f, progress01));
        detailsTimedProgressFill.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .width(DETAILS_PROGRESS_BAR_WIDTH * clamped)
                .height(DETAILS_PROGRESS_BAR_HEIGHT)
        );
        detailsTimedProgressFill.style(style -> style.backgroundTexture(new ColorRectTexture(fillColor)));
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
            if (helpPanel != null) {
                helpPanel.setDisplay(true);
            }
        }
    }

    private float interpolate(float progress, float start, float end) {
        return start + (end - start) * progress;
    }

    private void applyNodeButtonState(Button nodeButton, ResearchNode node) {
        ResearchState state = resolveNodeState(node);
        String stateText = switch (state) {
            case STUDIED -> "DONE";
            case AVAILABLE -> "OPEN";
            case IN_PROGRESS -> node.getStudyType() == ResearchStudyType.TIMED ? "TIME" : "WORK";
            case LOCKED -> "LOCK";
        };

        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        nodeButton.setText(stateText + " | " + title);

        ResearchLinkRenderState renderState = toRenderState(state);
        int backgroundColor = applyLinkRenderStateColor(node.getGroupColor(), renderState);
        nodeButton.style(style -> style.backgroundTexture(new ColorRectTexture(backgroundColor)));
    }

    private void updateResearchButton(ResearchNode node, ResearchState state) {
        if (researchButton == null) {
            return;
        }

        if (state == ResearchState.STUDIED || state == ResearchState.LOCKED) {
            researchButton.setDisplay(false);
            return;
        }

        researchButton.setDisplay(true);
        if (state == ResearchState.IN_PROGRESS) {
            switch (node.getStudyType()) {
                case TIMED -> {
                    ClientResearchProgress progress = researchProgressController.getProgress(node);
                    long remainingMs = progress == null ? 0L : progress.getRemainingMs(System.currentTimeMillis());
                    researchButton.setText("Timed: " + formatDuration(remainingMs));
                }
                case TABLE -> researchButton.setText("Resume Table Research");
                case INSTANT -> researchButton.setText("In Progress");
            }
            return;
        }

        researchButton.setText(switch (node.getStudyType()) {
            case INSTANT -> "Research";
            case TIMED -> "Start Timed Research";
            case TABLE -> "Open Table Research";
        });
    }

    private ResearchState resolveNodeState(ResearchNode node) {
        return researchProgressController.getState(node);
    }

    private String formatStateText(ResearchState state, ResearchNode node) {
        return switch (state) {
            case STUDIED -> "Studied";
            case AVAILABLE -> "Available";
            case LOCKED -> "Locked";
            case IN_PROGRESS -> switch (node.getStudyType()) {
                case TIMED -> "Timed research in progress";
                case TABLE -> "Table research in progress";
                case INSTANT -> "In progress";
            };
        };
    }

    private String formatStudyType(ResearchNode node) {
        return switch (node.getStudyType()) {
            case INSTANT -> "Instant";
            case TIMED -> "Timed (" + formatDuration(node.getStudyDurationMs()) + ")";
            case TABLE -> "Table";
        };
    }

    private String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, (durationMs + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private ResearchLinkRenderState toRenderState(ResearchState state) {
        return switch (state) {
            case STUDIED -> ResearchLinkRenderState.STUDIED;
            case AVAILABLE, IN_PROGRESS -> ResearchLinkRenderState.AVAILABLE;
            case LOCKED -> ResearchLinkRenderState.LOCKED;
        };
    }
}
