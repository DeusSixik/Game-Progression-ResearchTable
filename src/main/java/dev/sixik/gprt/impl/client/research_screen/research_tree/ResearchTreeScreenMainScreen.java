package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
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
 * Reusable "real screen" layer on top of {@link ResearchTreeScreen}.
 * <p>
 * {@link ResearchTreeScreen} stays focused on graph logic, layout, reveal animation and link rendering,
 * while this class owns the higher-level HUD pieces that most actual research-tree screens will need:
 * details panel, timed progress bar, research button flow, table placeholder overlay and client-side
 * progress controller integration.
 * </p>
 *
 * <p><b>Architecture intent:</b></p>
 * <ul>
 *     <li>{@link ResearchTreeScreen} = tree engine / graph behavior.</li>
 *     <li>{@link ResearchTreeScreenMainScreen} = reusable UI shell for an actual research screen.</li>
 *     <li>Concrete subclasses = provide tree data and any project-specific overlay controls.</li>
 * </ul>
 */
public abstract class ResearchTreeScreenMainScreen extends ResearchTreeScreen {
    private static final float DETAILS_PANEL_WIDTH = 300f;
    private static final float DETAILS_PANEL_HIDDEN_X = -(DETAILS_PANEL_WIDTH + 18f);
    private static final float DETAILS_PANEL_OPEN_X = 0f;
    private static final float DETAILS_PANEL_LERP_SPEED = 0.22f;
    private static final float DETAILS_PROGRESS_BAR_WIDTH = DETAILS_PANEL_WIDTH - 16f;
    private static final float DETAILS_PROGRESS_BAR_HEIGHT = 8f;

    private final Int2ObjectOpenHashMap<Button> nodeButtonsById = new Int2ObjectOpenHashMap<>();
    private final SimpleClientResearchProgressManager researchProgressManager = new SimpleClientResearchProgressManager();
    private final ResearchProgressController researchProgressController = new ResearchProgressController(researchProgressManager);

    private @Nullable UIElement overlayPanel;
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
    private int selectedNodeId = -1;
    private float detailsPanelProgress;
    private float detailsPanelTargetProgress;
    private boolean tablePlaceholderVisible;
    private int rootNodeId = -1;

    /**
     * Creates the root UI container that hosts the graph, overlay controls and shared panels.
     */
    public final UIElement createView() {
        return new RootView(this);
    }

    /**
     * One-time screen setup for subclasses after they configured auto-layout and filled the tree.
     */
    protected final void initializeMainScreen() {
        setAutoLayoutAutoFit(false);
        beginAutoLayoutBatch();
        try {
            buildResearchTree();
            resetProgressState();
        } finally {
            endAutoLayoutBatch();
        }
        setAutoLayoutEnabled(true);
    }

    /**
     * Subclasses define their actual research nodes/links here.
     */
    protected abstract void buildResearchTree();

    /**
     * Optional overlay controls shown over the graph. Subclasses can return {@code null} when they
     * do not need top-left helper controls.
     */
    protected @Nullable UIElement createOverlayPanel() {
        return null;
    }

    /**
     * Hook for subclasses that want extra behavior when the user selects a node.
     */
    protected void onNodeSelected(ResearchNode node) {
    }

    protected final ResearchProgressController progressController() {
        return researchProgressController;
    }

    protected final void setRootNodeId(int rootNodeId) {
        this.rootNodeId = rootNodeId;
    }

    protected final void centerRootNode() {
        if (rootNodeId >= 0) {
            centerCameraOn(rootNodeId);
        }
    }

    protected final void toggleGroupFocus(String groupId) {
        boolean zoomToGroup = groupId.equals(getHighlightedGroupId());
        focusGroup(groupId, zoomToGroup);
    }

    protected final void clearFocusedGroup() {
        clearHighlightedGroup();
    }

    protected final void resetProgressState() {
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
            onNodeSelected(node);
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
        if (researchProgressController.update(nowMs)) {
            applyCompletedResearchFromManager();
        }

        refreshRealtimeResearchUi();
        updateDetailsPanelAnimation();
    }

    protected final void tryStartResearch(ResearchNode node) {
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
            if (node.getResearchKey() != null) {
                researchProgressController.setUnlocked(node, isNodeUnlockedForStudy(node.getId()));
            }
        }
    }

    private void refreshRealtimeResearchUi() {
        ResearchNode selectedNode = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (selectedNode != null && researchProgressController.isInProgress(selectedNode)) {
            refreshDetailsPanel();
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
        if (overlayPanel != null) {
            overlayPanel.setDisplay(false);
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

        if (researchProgressController.tryCancelResearch(node)) {
            Button nodeButton = nodeButtonsById.get(node.getId());
            if (nodeButton != null) {
                applyNodeButtonState(nodeButton, node);
            }
            closeTablePlaceholderOverlay();
            refreshDetailsPanel();
        }
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

        ResearchState state = researchProgressController.getState(node);
        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        String group = node.getGroup() != null ? node.getGroup().getTitle() : "Unknown";
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
        if (tablePlaceholderOverlay == null || !tablePlaceholderVisible) {
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
            if (overlayPanel != null) {
                overlayPanel.setDisplay(true);
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

    private UIElement createDetailsPanel() {
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

        Button closeButton = new Button().setText("X").setOnClick(event -> closeDetailsPanel());
        closeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(264)
                .top(6)
                .width(24)
                .height(18)
        );

        Button research = new Button().setText("Research")
                .setOnClick(event -> {
                    if (selectedNodeId != -1) {
                        ResearchNode node = getNodeById(selectedNodeId);
                        if (node != null) {
                            tryStartResearch(node);
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

        bindDetailsPanel(
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

    private UIElement createTablePlaceholderOverlay() {
        ResearchTablePlaceholderOverlay overlay = new ResearchTablePlaceholderOverlay(
                this::completeTablePlaceholderResearch,
                this::cancelTablePlaceholderResearch,
                this::closeTablePlaceholderOverlay
        );
        bindTablePlaceholderOverlay(overlay);
        return overlay;
    }

    private static final class RootView extends UIElement {
        private RootView(ResearchTreeScreenMainScreen screen) {
            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));

            addChildren(screen);

            UIElement overlay = screen.createOverlayPanel();
            if (overlay != null) {
                screen.overlayPanel = overlay;
                addChildren(overlay);
            }

            addChildren(screen.createDetailsPanel(), screen.createTablePlaceholderOverlay());
        }
    }
}
