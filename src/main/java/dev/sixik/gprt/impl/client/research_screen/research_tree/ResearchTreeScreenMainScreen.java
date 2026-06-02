package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContent;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchDisplayValue;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoEntry;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanel;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoSection;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchProgressController;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.SimpleClientResearchProgressManager;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
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
    private static final float DETAILS_PANEL_LERP_SPEED = 0.22f;

    private final Int2ObjectOpenHashMap<Button> nodeButtonsById = new Int2ObjectOpenHashMap<>();
    private final SimpleClientResearchProgressManager researchProgressManager = new SimpleClientResearchProgressManager();
    private final ResearchProgressController researchProgressController = new ResearchProgressController(researchProgressManager);

    private @Nullable UIElement overlayPanel;
    private @Nullable ResearchInfoPanel detailsPanel;

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

    /**
     * Auto-generated prerequisites section toggle.
     * <p>
     * Override in concrete screens when you want to fully own how conditions are presented.
     * </p>
     */
    protected boolean isAutoConditionsSectionEnabled() {
        return true;
    }

    /**
     * Auto-generated unlocks section toggle.
     * <p>
     * Override when the screen should not list direct child researches in the info panel.
     * </p>
     */
    protected boolean isAutoUnlocksSectionEnabled() {
        return true;
    }

    /**
     * Controls whether hidden child researches should appear in the auto-generated unlock list.
     * <p>
     * Default is {@code false}, so branches that are still hidden by progression stay hidden in the panel too.
     * </p>
     */
    protected boolean includeHiddenUnlocksInInfoPanel() {
        return false;
    }

    protected final boolean focusResearchByKey(String researchKey) {
        ResearchNode node = getNodeByResearchKey(researchKey);
        if (node == null) {
            return false;
        }
        openDetailsPanel(node.getId());
        centerCameraOn(node.getId());
        onNodeSelected(node);
        return true;
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

    private void bindDetailsPanel(ResearchInfoPanel panel) {
        this.detailsPanel = panel;
        refreshDetailsPanel();
        updateDetailsPanelAnimation();
    }

    private void bindTablePlaceholderOverlay(ResearchTablePlaceholderOverlay overlay) {
        this.tablePlaceholderOverlay = overlay;
        refreshTablePlaceholderOverlay();
    }

    private void refreshDetailsPanel() {
        if (detailsPanel == null) {
            return;
        }

        ResearchNode node = selectedNodeId >= 0 ? getNodeById(selectedNodeId) : null;
        if (node == null) {
            detailsPanel.applyContent(buildEmptyInfoContent());
            return;
        }

        ResearchState state = researchProgressController.getState(node);
        detailsPanel.applyContent(buildInfoContent(node, state));
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

    private void updateDetailsPanelAnimation() {
        if (detailsPanel == null) {
            return;
        }

        detailsPanelProgress += (detailsPanelTargetProgress - detailsPanelProgress) * DETAILS_PANEL_LERP_SPEED;
        if (Math.abs(detailsPanelTargetProgress - detailsPanelProgress) < 0.002f) {
            detailsPanelProgress = detailsPanelTargetProgress;
        }

        detailsPanel.setSlideProgress(detailsPanelProgress);

        if (detailsPanelTargetProgress <= 0f && detailsPanelProgress <= 0f) {
            detailsPanel.setDisplay(false);
            if (overlayPanel != null) {
                overlayPanel.setDisplay(true);
            }
        }
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

    private ResearchState resolveNodeState(ResearchNode node) {
        return researchProgressController.getState(node);
    }

    /**
     * Default panel content builder.
     * <p>
     * Subclasses can override this method to inject rewards, conditions or project-specific
     * text generation later without rewriting the panel widget itself.
     * </p>
     */
    protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state) {
        String title = node.getTitle() != null ? node.getTitle() : ("Node " + node.getId());
        String group = node.getGroup() != null ? node.getGroup().getTitle() : "Unknown";
        String description = node.getDescription();
        if (description == null || description.isBlank()) {
            description = "No description has been assigned to this research yet.";
        }

        ResearchInfoContent.Builder builder = ResearchInfoContent.builder()
                .title(title)
                .titleCentered()
                .titleLarge(true)
                .groupText("Group: " + group)
                .modeText("Mode: " + formatStudyType(node))
                .stateText("Status: " + formatStateText(state, node))
                .description(description)
                .panelColor(0xD0000000 | (applyLinkRenderStateColor(node.getGroupColor(), toRenderState(state)) & 0x00FFFFFF));
        appendMetadataSection(builder, node);
        if (isAutoConditionsSectionEnabled()) {
            appendParentConditions(builder, node);
        }
        if (isAutoUnlocksSectionEnabled()) {
            appendUnlockedRewards(builder, node);
        }

        if (node.getStudyType() == ResearchStudyType.TIMED && state != ResearchState.STUDIED && state != ResearchState.LOCKED) {
            ClientResearchProgress progress = researchProgressController.getProgress(node);
            if (progress == null || state != ResearchState.IN_PROGRESS) {
                builder.timedProgress("Duration: " + formatDuration(node.getStudyDurationMs()), 0f, 0x664C90E8);
            } else {
                long nowMs = System.currentTimeMillis();
                long remainingMs = progress.getRemainingMs(nowMs);
                float progress01 = progress.getProgress01(nowMs);
                builder.timedProgress(
                        "Progress: " + Math.round(progress01 * 100f) + "% | left " + formatDuration(remainingMs),
                        progress01,
                        0xFF67B7FF
                );
            }
        } else {
            builder.hideTimedProgress();
        }

        if (state == ResearchState.STUDIED || state == ResearchState.LOCKED) {
            builder.hideResearchButton();
        } else if (state == ResearchState.IN_PROGRESS) {
            builder.researchButton(getInProgressButtonText(node), true);
        } else {
            builder.researchButton(getAvailableButtonText(node), true);
        }

        return builder.build();
    }

    protected ResearchInfoContent buildEmptyInfoContent() {
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

    private String formatVisibilityMode(ResearchNode node) {
        return switch (node.getVisibilityMode()) {
            case ALWAYS_VISIBLE -> "Always visible";
            case REQUIRE_ANY_PARENT_STUDIED -> "Require any parent studied";
            case REQUIRE_ALL_PARENTS_STUDIED -> "Require all parents studied";
        };
    }

    private String getInProgressButtonText(ResearchNode node) {
        return switch (node.getStudyType()) {
            case TIMED -> {
                ClientResearchProgress progress = researchProgressController.getProgress(node);
                long remainingMs = progress == null ? 0L : progress.getRemainingMs(System.currentTimeMillis());
                yield "Timed: " + formatDuration(remainingMs);
            }
            case TABLE -> "Resume Table Research";
            case INSTANT -> "In Progress";
        };
    }

    private String getAvailableButtonText(ResearchNode node) {
        return switch (node.getStudyType()) {
            case INSTANT -> "Research";
            case TIMED -> "Start Timed Research";
            case TABLE -> "Open Table Research";
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

    private void appendParentConditions(ResearchInfoContent.Builder builder, ResearchNode node) {
        ResearchLink[] parentLinks = getLinksToNode(node.getId());
        builder.section("Conditions", section -> {
            if (parentLinks.length == 0) {
                section.conditionText("No prerequisites", true);
                return;
            }

            appendConditionModeSummary(section, node, parentLinks);

            for (ResearchLink parentLink : parentLinks) {
                ResearchNode parent = getNodeById(parentLink.getNodeFrom());
                if (parent == null) {
                    continue;
                }
                String parentTitle = parent.getTitle() != null ? parent.getTitle() : ("Node " + parent.getId());
                section.condition(entry -> {
                    entry.text("Study " + parentTitle)
                            .completed(parent.isStudied())
                            .tooltip(Component.literal("Required research: " + parentTitle))
                            .jumpToResearch(parent.getResearchKey())
                            .jumpButtonText("Go to");
                    if (parent.getTitle() != null) {
                        entry.icon(new ColorRectTexture(parent.getGroupColor()));
                    }
                });
            }
        });
    }

    private void appendConditionModeSummary(ResearchInfoSection.Builder section, ResearchNode node, ResearchLink[] parentLinks) {
        if (parentLinks.length <= 1) {
            return;
        }

        switch (node.getVisibilityMode()) {
            case REQUIRE_ANY_PARENT_STUDIED -> section.condition(entry -> entry
                    .text("Study any one of the researches below")
                    .completed(hasAnyStudiedParent(parentLinks))
                    .tooltip(Component.literal("This research unlocks when at least one parent research is studied.")));
            case REQUIRE_ALL_PARENTS_STUDIED -> section.condition(entry -> entry
                    .text("Study all researches below")
                    .completed(hasAllStudiedParents(parentLinks))
                    .tooltip(Component.literal("This research unlocks only after every parent research below is studied.")));
            case ALWAYS_VISIBLE -> section.condition(entry -> entry
                    .text("Parents are linked for navigation only")
                    .completed(true)
                    .tooltip(Component.literal("This node stays visible even if none of the linked parent researches are studied.")));
        }
    }

    private boolean hasAnyStudiedParent(ResearchLink[] parentLinks) {
        for (ResearchLink parentLink : parentLinks) {
            ResearchNode parent = getNodeById(parentLink.getNodeFrom());
            if (parent != null && parent.isStudied()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAllStudiedParents(ResearchLink[] parentLinks) {
        for (ResearchLink parentLink : parentLinks) {
            ResearchNode parent = getNodeById(parentLink.getNodeFrom());
            if (parent == null || !parent.isStudied()) {
                return false;
            }
        }
        return true;
    }

    private void appendUnlockedRewards(ResearchInfoContent.Builder builder, ResearchNode node) {
        ResearchLink[] childLinks = getLinksFromNode(node.getId());
        builder.section("Unlocks", section -> {
            boolean addedAny = false;

            for (ResearchLink childLink : childLinks) {
                ResearchNode child = getNodeById(childLink.getNodeTo());
                if (child == null) {
                    continue;
                }
                if (!includeHiddenUnlocksInInfoPanel() && !isNodeVisible(child.getId())) {
                    continue;
                }
                String childTitle = child.getTitle() != null ? child.getTitle() : ("Node " + child.getId());
                section.rewardResearch(childTitle, child.getResearchKey());
                addedAny = true;
            }

            if (!addedAny) {
                if (childLinks.length == 0) {
                    section.rewardText("No direct follow-up research");
                } else if (!includeHiddenUnlocksInInfoPanel()) {
                    section.rewardText("Follow-up research is still hidden");
                } else {
                    section.rewardText("No visible follow-up research");
                }
            }
        });
    }

    private void appendMetadataSection(ResearchInfoContent.Builder builder, ResearchNode node) {
        builder.section("Info", section -> {
            section.infoLine("Key", node.getResearchKey() != null ? node.getResearchKey() : ("node_" + node.getId()));
            section.infoLine("Visibility", formatVisibilityMode(node));
        });
    }

    private ResearchLinkRenderState toRenderState(ResearchState state) {
        return switch (state) {
            case STUDIED -> ResearchLinkRenderState.STUDIED;
            case AVAILABLE, IN_PROGRESS -> ResearchLinkRenderState.AVAILABLE;
            case LOCKED -> ResearchLinkRenderState.LOCKED;
        };
    }

    private UIElement createDetailsPanel() {
        ResearchInfoPanel panel = new ResearchInfoPanel(
                this::closeDetailsPanel,
                () -> {
                    if (selectedNodeId != -1) {
                        ResearchNode node = getNodeById(selectedNodeId);
                        if (node != null) {
                            tryStartResearch(node);
                        }
                    }
                },
                this::focusResearchByKey
        );
        bindDetailsPanel(panel);
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
