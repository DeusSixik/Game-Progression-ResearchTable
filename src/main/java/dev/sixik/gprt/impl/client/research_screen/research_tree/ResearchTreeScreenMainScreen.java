package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContent;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContentFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoContentPresets;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanel;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPanelWidget;
import dev.sixik.gprt.impl.client.research_screen.research_tree.info.ResearchInfoPresentationRules;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.DefaultResearchNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.DefaultResearchNodeThemeResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeGroupThemeResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchRevealAnimationStyle;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeRenderContext;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeTheme;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeThemeResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeVisualResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWidgetFactory;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWrapper;
import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWrapperResolver;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.presentation.ResearchUnlockPresentationController;
import dev.sixik.gprt.impl.client.research_screen.research_tree.presentation.ResearchTreeSeenStateCache;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ClientResearchProgress;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchProgressController;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchState;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.SimpleClientResearchProgressManager;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable "real screen" layer on top of {@link ResearchTreeScreen}.
 * <p>
 * {@link ResearchTreeScreen} stays focused on graph logic, layout, unlock animation and link rendering,
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
 *
 * <p><b>Navigation:</b></p>
 * <ul>
 *     <li>{@link #initializeMainScreen()} -
 *     one-time setup that builds the tree and resets client-side progress state.</li>
 *     <li>{@link #buildResearchTree()} -
 *     subclass hook where the actual node/link data is created.</li>
 *     <li>{@link #createView()} -
 *     root UI entry point that assembles the graph, overlays and shared panels.</li>
 *     <li>{@link #buildInfoContent(ResearchNode, ResearchState)} -
 *     default details-panel content builder.</li>
 *     <li>{@link #createStandardInfoContentBuilder(ResearchNode, ResearchState)} -
 *     recommended extension point when a subclass wants the shared panel behavior plus custom sections.</li>
 *     <li>{@link #progressController()} -
 *     access point for client-side research progress state in subclasses.</li>
 * </ul>
 *
 * <p><b>Related helper layers:</b></p>
 * <ul>
 *     <li>{@link ResearchInfoContentFactory} -
 *     high-level builder for assembling a full info panel.</li>
 *     <li>{@link ResearchInfoContentPresets} -
 *     reusable standard sections and jump sanitizing.</li>
 *     <li>{@link ResearchInfoPresentationRules} -
 *     shared formatting and state-to-UI rules for panel text, progress and buttons.</li>
 * </ul>
 *
 * <p><b>Typical subclass workflow:</b></p>
 * <ol>
 *     <li>Configure layout in the constructor and call {@link #initializeMainScreen()}.</li>
 *     <li>Implement {@link #buildResearchTree()} to define the research graph.</li>
 *     <li>Optionally override {@link #createOverlayPanel()} for screen-specific controls.</li>
 *     <li>Optionally override {@link #buildInfoContent(ResearchNode, ResearchState)} and start from
 *     {@link #createStandardInfoContentBuilder(ResearchNode, ResearchState)} when custom panel sections are needed.</li>
 * </ol>
 */
public abstract class ResearchTreeScreenMainScreen extends ResearchTreeScreen {
    private static final long DETAILS_PANEL_ANIMATION_DURATION_MS = 240L;

    protected final Int2ObjectOpenHashMap<UIElement> nodeWidgetsById = new Int2ObjectOpenHashMap<>();
    protected final SimpleClientResearchProgressManager researchProgressManager = new SimpleClientResearchProgressManager();
    protected final ResearchProgressController researchProgressController = new ResearchProgressController(researchProgressManager);

    protected @Nullable UIElement overlayPanel;
    protected @Nullable ResearchInfoPanelWidget detailsPanel;
    protected @Nullable ResearchNodeWidgetFactory nodeWidgetFactory;
    protected @Nullable ResearchNodeThemeResolver nodeThemeResolver;
    protected @Nullable ResearchNodeWrapperResolver nodeWrapperResolver;
    protected @Nullable ResearchUnlockPresentationController unlockPresentationController;

    protected @Nullable ResearchTablePlaceholderOverlay tablePlaceholderOverlay;
    protected int selectedNodeId = -1;
    protected float detailsPanelProgress;
    protected float detailsPanelTargetProgress;
    protected float detailsPanelAnimationStartProgress;
    protected long detailsPanelAnimationStartedAtMs;
    private DetailsPanelAnimationPhase detailsPanelAnimationPhase = DetailsPanelAnimationPhase.CLOSED;
    protected boolean tablePlaceholderVisible;
    protected boolean unlockAnimationOnOpenPrepared;
    protected int rootNodeId = -1;

    /**
     * Creates the root UI container that hosts the graph, overlay controls and shared panels.
     */
    public final UIElement createView() {
        prepareUnlockAnimationsOnOpen();
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
     * Creates the info-panel widget used by this screen.
     * <p>
     * Override this when you want a fully custom details panel widget. The returned widget only
     * needs to consume {@link ResearchInfoContent} and react to slide progress; the screen keeps
     * ownership of selection, progression and content generation logic.
     * </p>
     */
    protected ResearchInfoPanelWidget createInfoPanelWidget(ResearchInfoPanelContext context) {
        return new ResearchInfoPanel(context);
    }

    /**
     * Creates the factory responsible for research-node widgets.
     * <p>
     * Screens can override this to replace the default node look without touching selection,
     * progression or animation/interaction-lock logic in the main screen itself.
     * </p>
     */
    protected ResearchNodeWidgetFactory createNodeWidgetFactory() {
        return new DefaultResearchNodeWidgetFactory();
    }

    /**
     * Creates the resolver that maps a research node to a reusable visual theme preset.
     * <p>
     * Override this when you want to centralize branch presets, icon packs or project-wide
     * node theme rules separately from the final state-based rendering pass.
     * </p>
     */
    protected ResearchNodeThemeResolver createNodeThemeResolver() {
        ResearchNodeGroupThemeResolver.Builder builder = ResearchNodeGroupThemeResolver.builder()
                .fallback(new DefaultResearchNodeThemeResolver());
        configureNodeThemePresets(builder);
        return builder.build();
    }

    /**
     * Creates the resolver responsible for effective runtime node bounds.
     * <p>
     * Default behavior keeps runtime bounds equal to logical node bounds. Override this when a
     * screen wants style-specific card geometry without pushing that data into {@link ResearchNode}.
     * </p>
     */
    protected ResearchNodeWrapperResolver createNodeWrapperResolver() {
        return ResearchNodeWrapperResolver.identity();
    }

    /**
     * Optional convenience hook for declarative group presets.
     * <p>
     * Most concrete screens should prefer this method over overriding
     * {@link #createNodeThemeResolver()}: register one preset per branch/group here and keep the
     * default fallback resolver for everything else.
     * </p>
     */
    protected void configureNodeThemePresets(ResearchNodeGroupThemeResolver.Builder builder) {
    }

    /**
     * Hook for subclasses that want extra behavior when the user selects a node.
     */
    protected void onNodeSelected(ResearchNode node) {
    }

    /**
     * Builds the runtime render context for a node widget.
     * <p>
     * This context carries volatile state such as selection, temporary interaction lock and timed progress,
     * while the visual definition returned by {@link #buildNodeVisualDefinition(ResearchNode, ResearchNodeRenderContext)}
     * remains the reusable style description.
     * </p>
     */
    protected ResearchNodeRenderContext buildNodeRenderContext(ResearchNode node) {
        return buildNodeRenderContext(node, System.currentTimeMillis());
    }

    /**
     * Builds the reusable semantic theme for a research node before runtime state is applied.
     * <p>
     * Override this when you want branch-specific presets such as icons, badges, title alignment
     * or shared per-group styling without duplicating the state-color logic.
     * </p>
     */
    protected ResearchNodeTheme buildNodeTheme(ResearchNode node, ResearchNodeRenderContext context) {
        return getNodeThemeResolver().resolveTheme(node, context);
    }

    /**
     * Builds the visual description for one research node.
     * <p>
     * Override this only when the default theme + runtime-state pipeline is not enough.
     * For most cases it is cleaner to override {@link #buildNodeTheme(ResearchNode, ResearchNodeRenderContext)}
     * and let {@link ResearchNodeVisualResolver} derive the final stateful visuals.
     * </p>
     */
    protected ResearchNodeVisualDefinition buildNodeVisualDefinition(ResearchNode node, ResearchNodeRenderContext context) {
        return ResearchNodeVisualResolver.resolve(
                buildNodeTheme(node, context),
                node,
                context,
                resolveNodeSizePreset(context.getNodeWrapper())
        );
    }

    protected final ResearchProgressController progressController() {
        return researchProgressController;
    }

    @Override
    protected ResearchRevealAnimationStyle resolveUnlockRevealAnimationStyle(ResearchNode node) {
        if (node != null && node.getRevealAnimationStyle() != null) {
            return node.getRevealAnimationStyle();
        }
        ResearchNodeRenderContext previewContext = ResearchNodeRenderContext.builder()
                .nodeWrapper(ResearchNodeWrapper.fromNode(node))
                .state(resolveNodeState(node))
                .visible(isNodeVisible(node.getId()))
                .highlighted(getHighlightedGroupId() != null && getHighlightedGroupId().equals(node.getGroup().getId()))
                .interactionLocked(isUnlockAnimationActive())
                .selected(selectedNodeId == node.getId() && detailsPanelTargetProgress > 0f)
                .hasNewUnlockMarker(false)
                .nowMs(System.currentTimeMillis())
                .progress(researchProgressController.getProgress(node))
                .unlockRevealAnimationStyle(ResearchRevealAnimationStyle.DROP_BOUNCE)
                .build();
        ResearchNodeTheme theme = getNodeThemeResolver().resolveTheme(node, previewContext);
        return theme != null ? theme.getRevealAnimationStyle() : ResearchRevealAnimationStyle.DROP_BOUNCE;
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
        refreshAllNodeWidgets(System.currentTimeMillis());
    }

    protected final void clearFocusedGroup() {
        clearHighlightedGroup();
        refreshAllNodeWidgets(System.currentTimeMillis());
    }

    /**
     * Forces an immediate refresh of all currently existing node widgets.
     * <p>
     * Useful for debug tools or runtime theme switches where the underlying node data does not
     * change, but the current visual presentation should be reapplied right away.
     * </p>
     */
    protected final void refreshNodeWidgetsNow() {
        refreshAllNodeWidgets(System.currentTimeMillis());
    }

    /**
     * Enables or disables wrapper-aware spacing in auto-layout.
     * <p>
     * When enabled, the dependency layout uses resolved runtime wrapper size instead of raw node
     * size while computing inter-layer and vertical spacing.
     * </p>
     */
    protected final void setAutoLayoutUseNodeWrappersForSpacing(boolean enabled) {
        autoLayoutConfig().useNodeWrappersForSpacing(enabled);
    }

    @Override
    protected ResearchNodeWrapper getLayoutWrapperForNode(ResearchNode node) {
        ResearchNodeWrapper wrapper = ResearchNodeWrapper.fromNode(node);
        applyNodeWrapperResolver(node, wrapper, System.currentTimeMillis());
        return wrapper;
    }

    /**
     * Controls whether this screen should replay unseen unlock animations on the next open.
     * <p>
     * Default is enabled. Concrete screens can disable it while the feature is still being wired
     * to real server-driven progression state.
     * </p>
     */
    protected boolean isUnlockAnimationOnOpenEnabled() {
        return true;
    }

    /**
     * Controls whether already studied but still unseen researches may replay their unlock
     * animation the first time the player notices them in the tree.
     * <p>
     * This hook affects only delayed open presentation on tree show/reopen. It does not change
     * real progression, unlock conditions, study completion, or node visibility.
     * </p>
     *
     * <p>
     * Default keeps the original conservative behavior: once a research is already studied it is
     * treated as already seen and will not get a delayed open animation. Override this in screens
     * that want the tree to visually introduce already completed content the first time the player
     * notices it.
     * </p>
     */
    protected ResearchUnlockPresentationController.StudiedResearchAnimationMode getStudiedUnlockAnimationMode() {
        return ResearchUnlockPresentationController.StudiedResearchAnimationMode.NEVER_FOR_STUDIED;
    }

    /**
     * Logical runtime key used by the delayed unlock-presentation cache.
     * <p>
     * Override this when several screens share the same research tree and should therefore also
     * share the same "already seen" presentation state.
     * </p>
     */
    protected String getUnlockAnimationPresentationKey() {
        return getClass().getName();
    }

    /**
     * Returns the delayed unlock-presentation controller used by this screen instance/tree key.
     * <p>
     * This is mainly exposed for debug or advanced presentation tooling. Most subclasses should
     * prefer the higher-level helper methods below instead of mutating the cache directly.
     * </p>
     */
    protected final ResearchUnlockPresentationController unlockPresentationController() {
        return getUnlockPresentationController();
    }

    /**
     * Clears the runtime-only "already shown to the player" state for delayed unlock presentation.
     * <p>
     * This does not change real research progression. It only resets the client cache that drives
     * "animate newly visible researches on next open" behavior.
     * </p>
     */
    protected final void resetUnlockPresentationState() {
        getUnlockPresentationController().resetSeenState();
    }

    /**
     * Simulates the normal "tree just opened" delayed unlock scan against currently visible nodes.
     * <p>
     * This uses the same flow as {@link #createView()}: visible researches are compared against the
     * seen cache, new pending unlock animations are recorded, and matching visible nodes are queued
     * into the regular unlock-animation pipeline.
     * </p>
     *
     * @return number of visible node animations queued right now
     */
    protected final int simulateUnlockAnimationPresentationOpenNow() {
        ObjectArrayList<ResearchNode> visibleNodes = collectVisibleNodesForUnlockPresentation();
        ObjectArrayList<String> pendingResearchKeys = getUnlockPresentationController()
                .collectPendingUnlockAnimationResearchIds(visibleNodes, getStudiedUnlockAnimationMode());
        return queueUnlockAnimationsForResearchKeys(pendingResearchKeys);
    }

    /**
     * Queues all currently visible, not-yet-seen, not-yet-pending researches for unlock animation.
     * <p>
     * Unlike {@link #simulateUnlockAnimationPresentationOpenNow()}, this helper bypasses the
     * first-open suppression logic and is meant specifically for debug tooling or forced replays.
     * </p>
     *
     * @return number of visible node animations queued right now
     */
    protected final int queueVisibleUnseenUnlockAnimationsNow() {
        ObjectArrayList<ResearchNode> visibleNodes = collectVisibleNodesForUnlockPresentation();
        ResearchTreeSeenStateCache cache = getUnlockPresentationController().cache();
        ObjectArrayList<String> pendingResearchKeys = new ObjectArrayList<>();

        for (int i = 0, size = visibleNodes.size(); i < size; i++) {
            ResearchNode node = visibleNodes.get(i);
            String researchKey = node.getResearchKey();
            if (researchKey == null || researchKey.isBlank() || node.isStudied()) {
                continue;
            }
            if (cache.seenVisibleResearchIds().contains(researchKey)
                    || cache.pendingUnlockAnimationResearchIds().contains(researchKey)) {
                continue;
            }

            cache.pendingUnlockAnimationResearchIds().add(researchKey);
            pendingResearchKeys.add(researchKey);
        }

        return queueUnlockAnimationsForResearchKeys(pendingResearchKeys);
    }

    /**
     * Replays every currently cached pending unlock animation that is still visible in the tree.
     *
     * @return number of visible node animations queued right now
     */
    protected final int replayPendingUnlockAnimationsNow() {
        return queueUnlockAnimationsForResearchKeys(getUnlockPresentationController().cache().pendingUnlockAnimationResearchIds());
    }

    /**
     * Marks all currently visible researches as already shown to the player.
     *
     * @return number of visible researches processed
     */
    protected final int markVisibleUnlockPresentationNodesSeen() {
        ObjectArrayList<ResearchNode> visibleNodes = collectVisibleNodesForUnlockPresentation();
        ObjectArrayList<String> visibleResearchKeys = new ObjectArrayList<>(visibleNodes.size());
        for (int i = 0, size = visibleNodes.size(); i < size; i++) {
            String researchKey = visibleNodes.get(i).getResearchKey();
            if (researchKey != null && !researchKey.isBlank()) {
                visibleResearchKeys.add(researchKey);
            }
        }
        getUnlockPresentationController().markVisibleAsSeen(visibleResearchKeys);
        return visibleResearchKeys.size();
    }

    /**
     * Returns how many researches are visible right now and therefore participate in the
     * delayed unlock-presentation scan.
     */
    protected final int getVisibleUnlockPresentationNodeCount() {
        return collectVisibleNodesForUnlockPresentation().size();
    }

    /**
     * Returns how many research keys are remembered as already shown to the player.
     */
    protected final int getSeenUnlockPresentationNodeCount() {
        return getUnlockPresentationController().cache().seenVisibleResearchIds().size();
    }

    /**
     * Returns how many research keys are still waiting to replay their delayed unlock animation.
     */
    protected final int getPendingUnlockPresentationNodeCount() {
        return getUnlockPresentationController().cache().pendingUnlockAnimationResearchIds().size();
    }

    /**
     * Returns the last timestamp when the delayed unlock-presentation "tree open" scan ran.
     */
    protected final long getUnlockPresentationLastTreeOpenTime() {
        return getUnlockPresentationController().cache().lastTreeOpenTime();
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
        if (node == null || !canFocusResearchFromInfoPanel(node)) {
            return false;
        }
        openDetailsPanel(node.getId());
        centerCameraOn(node.getId());
        onNodeSelected(node);
        return true;
    }

    /**
     * Controls whether an info-panel jump is allowed to focus the target research.
     * <p>
     * By default hidden researches are protected from info-panel navigation, so prerequisite
     * rows cannot expose branches that are still intentionally invisible to the player.
     * </p>
     */
    protected boolean canFocusResearchFromInfoPanel(ResearchNode node) {
        return node != null && isNodeVisible(node.getId());
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
        ResearchNodeRenderContext context = buildNodeRenderContext(node);
        ResearchNodeVisualDefinition visualDefinition = buildNodeVisualDefinition(node, context);
        UIElement nodeWidget = getNodeWidgetFactory().createNodeWidget(
                node,
                context,
                visualDefinition,
                () -> {
                    if (isUnlockAnimationActive()) {
                        return;
                    }
                    openDetailsPanel(node.getId());
                    centerCameraOn(node.getId());
                    onNodeSelected(node);
                }
        );
        nodeWidgetsById.put(node.getId(), nodeWidget);
        applyNodeWidgetLayout(nodeWidget, context.getNodeWrapper());
        return nodeWidget;
    }

    @Override
    protected void onResearchProgressionUpdated() {
        syncProgressManagerUnlockedState();
        refreshAllNodeWidgets(System.currentTimeMillis());
        refreshDetailsPanel();
        refreshTablePlaceholderOverlay();
    }

    @Override
    protected void afterUnlockAnimationCompleted(ResearchNode node) {
        String researchKey = node.getResearchKey();
        if (researchKey != null && !researchKey.isBlank()) {
            getUnlockPresentationController().markUnlockAnimationFinished(researchKey);
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        updateDetailsPanelAnimation(System.currentTimeMillis());
        super.drawBackgroundAdditional(guiContext);
    }

    @Override
    public void screenTick() {
        super.screenTick();

        long nowMs = System.currentTimeMillis();
        if (researchProgressController.update(nowMs)) {
            applyCompletedResearchFromManager();
        }

        refreshRealtimeResearchUi();
        updateDetailsPanelAnimation(nowMs);
    }

    protected final void tryStartResearch(ResearchNode node) {
        String researchKey = node.getResearchKey();
        if (researchKey == null || isUnlockAnimationActive()) {
            return;
        }

        ResearchState currentState = researchProgressController.getState(node);
        if (currentState == ResearchState.IN_PROGRESS && node.getStudyType() == ResearchStudyType.TABLE) {
            openTablePlaceholderOverlay(node);
            refreshDetailsPanel();
            return;
        }

        if (!canStartResearch(node)) {
            refreshDetailsPanel();
            refreshTablePlaceholderOverlay();
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
            refreshNodeWidget(node, System.currentTimeMillis());
            refreshDetailsPanel();
            refreshTablePlaceholderOverlay();
        }
    }

    /**
     * Hook for screen-specific validation before a research may start.
     * <p>
     * The default implementation only checks the generic progress state and allows subclasses to
     * add extra gates such as item conditions, energy costs, table occupancy or server-side
     * placeholders. Returning {@code false} cancels the start attempt without changing progress.
     * </p>
     */
    protected boolean canStartResearch(ResearchNode node) {
        return true;
    }

    /**
     * Returns whether the current info-panel action button should be interactive.
     * <p>
     * In-progress entries stay enabled so table researches can be resumed, while newly available
     * entries delegate to {@link #canStartResearch(ResearchNode)} for extra screen-specific gates.
     * </p>
     */
    protected boolean isResearchActionEnabled(ResearchNode node, ResearchState state) {
        if (state == ResearchState.IN_PROGRESS) {
            return true;
        }
        if (state != ResearchState.AVAILABLE) {
            return false;
        }
        return canStartResearch(node);
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

        refreshRealtimeNodeWidgets(System.currentTimeMillis());

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
        int previousSelectedNodeId = selectedNodeId;
        selectedNodeId = nodeId;
        refreshNodeWidgetById(previousSelectedNodeId, System.currentTimeMillis());
        refreshNodeWidgetById(selectedNodeId, System.currentTimeMillis());
        if (detailsPanel != null) {
            detailsPanel.setDisplay(true);
        }
        if (overlayPanel != null) {
            overlayPanel.setDisplay(false);
        }
        startDetailsPanelAnimation(1f);
        refreshDetailsPanel();
    }

    private void closeDetailsPanel() {
        refreshNodeWidgetById(selectedNodeId, System.currentTimeMillis());
        if (overlayPanel != null) {
            // Show the helper overlay immediately under the sliding panel so it is already
            // present by the time the close animation finishes.
            overlayPanel.setDisplay(true);
        }
        startDetailsPanelAnimation(0f);
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
            refreshNodeWidget(node, System.currentTimeMillis());
            closeTablePlaceholderOverlay();
            refreshDetailsPanel();
        }
    }

    private void bindDetailsPanel(ResearchInfoPanelWidget panel) {
        this.detailsPanel = panel;
        refreshDetailsPanel();
        updateDetailsPanelAnimation(System.currentTimeMillis());
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
        detailsPanel.applyContent(ResearchInfoContentPresets.sanitizeJumps(
                buildInfoContent(node, state),
                this::shouldRenderInfoJump
        ));
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

    private void updateDetailsPanelAnimation(long nowMs) {
        if (detailsPanel == null) {
            return;
        }

        if (Math.abs(detailsPanelTargetProgress - detailsPanelProgress) > 0.0001f) {
            long elapsedMs = Math.max(0L, nowMs - detailsPanelAnimationStartedAtMs);
            float animationProgress = Math.min(1f, elapsedMs / (float) DETAILS_PANEL_ANIMATION_DURATION_MS);
            float easedProgress = easeInOutCubic(animationProgress);
            detailsPanelProgress = lerp(detailsPanelAnimationStartProgress, detailsPanelTargetProgress, easedProgress);
            if (animationProgress >= 1f) {
                detailsPanelProgress = detailsPanelTargetProgress;
            }
        }

        detailsPanel.setSlideProgress(detailsPanelProgress);

        if (detailsPanelTargetProgress >= 1f && detailsPanelProgress >= 1f) {
            if (detailsPanelAnimationPhase != DetailsPanelAnimationPhase.OPEN) {
                detailsPanelAnimationPhase = DetailsPanelAnimationPhase.OPEN;
                detailsPanel.onOpenAnimationEnd();
            }
            return;
        }

        if (detailsPanelTargetProgress <= 0f && detailsPanelProgress <= 0f) {
            detailsPanel.setDisplay(false);
            if (overlayPanel != null) {
                overlayPanel.setDisplay(true);
            }
            if (detailsPanelAnimationPhase != DetailsPanelAnimationPhase.CLOSED) {
                detailsPanelAnimationPhase = DetailsPanelAnimationPhase.CLOSED;
                detailsPanel.onCloseAnimationEnd();
            }
        }
    }

    private void startDetailsPanelAnimation(float targetProgress) {
        float clampedTarget = clamp01(targetProgress);
        long nowMs = System.currentTimeMillis();
        boolean opening = clampedTarget > detailsPanelProgress;
        boolean closing = clampedTarget < detailsPanelProgress;

        detailsPanelAnimationStartProgress = detailsPanelProgress;
        detailsPanelTargetProgress = clampedTarget;
        detailsPanelAnimationStartedAtMs = nowMs;

        if (detailsPanel != null && clampedTarget > 0f) {
            detailsPanel.setDisplay(true);
            // A tiny non-zero progress keeps the first opening frame visible instead of
            // instantly hiding the widget again when the previous state was fully closed.
            detailsPanel.setSlideProgress(Math.max(detailsPanelProgress, 0.001f));
            if (opening && detailsPanelAnimationPhase != DetailsPanelAnimationPhase.OPENING) {
                detailsPanelAnimationPhase = DetailsPanelAnimationPhase.OPENING;
                detailsPanel.onOpenAnimationStart();
            }
        }
        if (overlayPanel != null && clampedTarget > 0f) {
            overlayPanel.setDisplay(false);
        }
        if (detailsPanel != null && closing && detailsPanelAnimationPhase != DetailsPanelAnimationPhase.CLOSING) {
            detailsPanelAnimationPhase = DetailsPanelAnimationPhase.CLOSING;
            detailsPanel.onCloseAnimationStart();
        }

        if (Math.abs(detailsPanelAnimationStartProgress - detailsPanelTargetProgress) < 0.0001f) {
            detailsPanelProgress = detailsPanelTargetProgress;
            if (detailsPanel != null) {
                detailsPanel.setSlideProgress(detailsPanelProgress);
                if (detailsPanelProgress <= 0f) {
                    detailsPanel.setDisplay(false);
                }
            }
            if (overlayPanel != null && detailsPanelProgress <= 0f) {
                overlayPanel.setDisplay(true);
            }
            if (detailsPanel != null) {
                if (detailsPanelProgress >= 1f && detailsPanelAnimationPhase != DetailsPanelAnimationPhase.OPEN) {
                    detailsPanelAnimationPhase = DetailsPanelAnimationPhase.OPEN;
                    detailsPanel.onOpenAnimationEnd();
                } else if (detailsPanelProgress <= 0f && detailsPanelAnimationPhase != DetailsPanelAnimationPhase.CLOSED) {
                    detailsPanelAnimationPhase = DetailsPanelAnimationPhase.CLOSED;
                    detailsPanel.onCloseAnimationEnd();
                }
            }
        }
    }

    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4f * t * t * t;
        }
        float inverse = -2f * t + 2f;
        return 1f - (inverse * inverse * inverse) / 2f;
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private ResearchUnlockPresentationController getUnlockPresentationController() {
        if (unlockPresentationController == null) {
            unlockPresentationController = ResearchUnlockPresentationController.forTree(getUnlockAnimationPresentationKey());
        }
        return unlockPresentationController;
    }

    private void prepareUnlockAnimationsOnOpen() {
        if (unlockAnimationOnOpenPrepared || !isUnlockAnimationOnOpenEnabled()) {
            return;
        }
        unlockAnimationOnOpenPrepared = true;

        ObjectArrayList<ResearchNode> visibleNodes = collectVisibleNodesForUnlockPresentation();
        if (visibleNodes.isEmpty()) {
            return;
        }

        ObjectArrayList<String> pendingResearchKeys = getUnlockPresentationController()
                .collectPendingUnlockAnimationResearchIds(visibleNodes, getStudiedUnlockAnimationMode());
        if (pendingResearchKeys.isEmpty()) {
            return;
        }

        IntArrayList pendingNodeIds = new IntArrayList(pendingResearchKeys.size());
        for (int i = 0, size = pendingResearchKeys.size(); i < size; i++) {
            ResearchNode node = getNodeByResearchKey(pendingResearchKeys.get(i));
            if (node != null) {
                pendingNodeIds.add(node.getId());
            }
        }
        queueUnlockAnimationNodeIds(pendingNodeIds);
    }

    private ObjectArrayList<ResearchNode> collectVisibleNodesForUnlockPresentation() {
        ObjectArrayList<ResearchNode> visibleNodes = new ObjectArrayList<>();
        for (ResearchNode node : nodes) {
            if (isNodeVisible(node.getId())
                    && node.getResearchKey() != null
                    && !node.getResearchKey().isBlank()) {
                visibleNodes.add(node);
            }
        }
        visibleNodes.sort((left, right) -> {
            int byX = Float.compare(left.getX(), right.getX());
            if (byX != 0) {
                return byX;
            }
            int byY = Float.compare(left.getY(), right.getY());
            if (byY != 0) {
                return byY;
            }
            return Integer.compare(left.getId(), right.getId());
        });
        return visibleNodes;
    }

    private int queueUnlockAnimationsForResearchKeys(Iterable<String> researchKeys) {
        IntArrayList pendingNodeIds = new IntArrayList();
        for (String researchKey : researchKeys) {
            if (researchKey == null || researchKey.isBlank()) {
                continue;
            }

            ResearchNode node = getNodeByResearchKey(researchKey);
            if (node != null) {
                pendingNodeIds.add(node.getId());
            }
        }
        queueUnlockAnimationNodeIds(pendingNodeIds);
        return pendingNodeIds.size();
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
     *
     * <p>
     * For most overrides the recommended pattern is:
     * start from {@link #createStandardInfoContentBuilder(ResearchNode, ResearchState)},
     * append custom sections and then call {@code build()}.
     * </p>
     */
    protected ResearchInfoContent buildInfoContent(ResearchNode node, ResearchState state) {
        ResearchInfoContentFactory.Builder builder = createStandardInfoContentBuilder(node, state);
        return builder.build();
    }

    protected ResearchInfoContent buildEmptyInfoContent() {
        return ResearchInfoContentFactory.emptySelection();
    }

    /**
     * Builds the standard info-panel builder for a research node before subclasses add their own sections.
     * <p>
     * This is the recommended extension point when a screen wants the shared metadata / unlock /
     * progress / button behavior but still needs to append extra custom blocks.
     * </p>
     *
     * <p>
     * Typical usage in a subclass:
     * create the builder here, append project-specific sections, then call {@code build()}
     * to produce the final {@link ResearchInfoContent}.
     * </p>
     */
    protected final ResearchInfoContentFactory.Builder createStandardInfoContentBuilder(ResearchNode node, ResearchState state) {
        ResearchInfoContentFactory.Builder builder = ResearchInfoContentFactory.forNode(node)
                .modeText("Mode: " + ResearchInfoPresentationRules.formatStudyType(node))
                .stateText("Status: " + ResearchInfoPresentationRules.formatStateText(state, node))
                .visibilityText(ResearchInfoPresentationRules.formatVisibilityMode(node))
                .panelColor(0xD0000000 | (applyLinkRenderStateColor(node.getGroupColor(), toRenderState(state)) & 0x00FFFFFF));

        if (isAutoConditionsSectionEnabled()) {
            List<ResearchNode> parents = collectParentNodes(node);
            if(!parents.isEmpty())
                builder.conditions(parents);
        } else {
            builder.noConditions();
        }
        if (isAutoUnlocksSectionEnabled()) {
            List<ResearchNode> unlocks = collectVisibleUnlockedChildren(node);
            if(!unlocks.isEmpty())
                builder.unlocks(unlocks, buildUnlocksFallbackText(node));
        } else {
            builder.noUnlocks();
        }

        long nowMs = System.currentTimeMillis();
        ClientResearchProgress progress = researchProgressController.getProgress(node);
        ResearchInfoPresentationRules.applyTimedProgress(builder, node, state, progress, nowMs);
        ResearchInfoPresentationRules.applyResearchButton(
                builder,
                node,
                state,
                progress,
                nowMs,
                isResearchActionEnabled(node, state)
        );
        return builder;
    }

    protected List<ResearchNode> collectParentNodes(ResearchNode node) {
        ResearchLink[] parentLinks = getLinksToNode(node.getId());
        List<ResearchNode> parents = new ObjectArrayList<>(parentLinks.length);
        for (ResearchLink parentLink : parentLinks) {
            ResearchNode parent = getNodeById(parentLink.getNodeFrom());
            if (parent != null) {
                parents.add(parent);
            }
        }
        return parents;
    }

    protected List<ResearchNode> collectVisibleUnlockedChildren(ResearchNode node) {
        ResearchLink[] childLinks = getLinksFromNode(node.getId());
        List<ResearchNode> children = new ObjectArrayList<>(childLinks.length);
        for (ResearchLink childLink : childLinks) {
            ResearchNode child = getNodeById(childLink.getNodeTo());
            if (child == null) {
                continue;
            }
            if (!includeHiddenUnlocksInInfoPanel() && !isNodeVisible(child.getId())) {
                continue;
            }
            children.add(child);
        }
        return children;
    }

    protected String buildUnlocksFallbackText(ResearchNode node) {
        ResearchLink[] childLinks = getLinksFromNode(node.getId());
        if (childLinks.length == 0) {
            return "No direct follow-up research";
        }
        if (!includeHiddenUnlocksInInfoPanel()) {
            return "Follow-up research is still hidden";
        }
        return "No visible follow-up research";
    }

    protected boolean shouldRenderInfoJump(String researchKey, boolean visibleTargetOnly) {
        ResearchNode targetNode = getNodeByResearchKey(researchKey);
        if (targetNode == null) {
            return false;
        }
        return !visibleTargetOnly || canFocusResearchFromInfoPanel(targetNode);
    }

    protected ResearchLinkRenderState toRenderState(ResearchState state) {
        return switch (state) {
            case STUDIED -> ResearchLinkRenderState.STUDIED;
            case AVAILABLE, IN_PROGRESS -> ResearchLinkRenderState.AVAILABLE;
            case LOCKED -> ResearchLinkRenderState.LOCKED;
        };
    }

    protected UIElement createDetailsPanel() {
        ResearchInfoPanelContext context = new ResearchInfoPanelContext(
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
        ResearchInfoPanelWidget panel = createInfoPanelWidget(context);
        bindDetailsPanel(panel);
        return panel;
    }

    protected UIElement createTablePlaceholderOverlay() {
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

    private enum DetailsPanelAnimationPhase {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    private ResearchNodeWidgetFactory getNodeWidgetFactory() {
        if (nodeWidgetFactory == null) {
            nodeWidgetFactory = createNodeWidgetFactory();
        }
        return nodeWidgetFactory;
    }

    protected final ResearchNodeThemeResolver getNodeThemeResolver() {
        if (nodeThemeResolver == null) {
            nodeThemeResolver = createNodeThemeResolver();
        }
        return nodeThemeResolver;
    }

    protected final ResearchNodeWrapperResolver getNodeWrapperResolver() {
        if (nodeWrapperResolver == null) {
            nodeWrapperResolver = createNodeWrapperResolver();
        }
        return nodeWrapperResolver;
    }

    private ResearchNodeRenderContext buildNodeRenderContext(ResearchNode node, long nowMs) {
        ResearchNodeWrapper nodeWrapper = buildNodeWrapper(node, nowMs);
        boolean unlockAnimating = isUnlockAnimationNode(node);
        float unlockNodeProgress01 = unlockAnimating ? getUnlockNodeAnimationProgress01(node, nowMs) : 0f;
        float unlockLinkProgress01 = unlockAnimating ? getUnlockLinkAnimationProgress01(node, nowMs) : 0f;
        UnlockNodeTransform unlockTransform = unlockAnimating
                ? getUnlockNodeAnimationTransform(node, unlockNodeProgress01)
                : new UnlockNodeTransform(1f, 0f, 0f);
        ResearchRevealAnimationStyle revealAnimationStyle = resolveUnlockRevealAnimationStyle(node);
        return ResearchNodeRenderContext.builder()
                .nodeWrapper(nodeWrapper)
                .state(resolveNodeState(node))
                .visible(isNodeVisible(node.getId()))
                .highlighted(getHighlightedGroupId() != null && getHighlightedGroupId().equals(node.getGroup().getId()))
                .interactionLocked(isUnlockAnimationActive())
                .selected(selectedNodeId == node.getId() && detailsPanelTargetProgress > 0f)
                .hasNewUnlockMarker(false)
                .nowMs(nowMs)
                .progress(researchProgressController.getProgress(node))
                .unlockAnimating(unlockAnimating)
                .unlockNodeProgress01(unlockNodeProgress01)
                .unlockLinkProgress01(unlockLinkProgress01)
                .unlockCurrentScale(unlockTransform.scale())
                .unlockCurrentTranslateX(unlockTransform.translateX())
                .unlockCurrentTranslateY(unlockTransform.translateY())
                .unlockRevealAnimationStyle(revealAnimationStyle)
                .build();
    }

    private void refreshAllNodeWidgets(long nowMs) {
        for (ResearchNode node : nodes) {
            refreshNodeWidget(node, nowMs);
        }
    }

    private void refreshRealtimeNodeWidgets(long nowMs) {
        for (ResearchNode node : nodes) {
            if (node.getId() == selectedNodeId
                    || researchProgressController.isInProgress(node)
                    || (node.getStudyType() == ResearchStudyType.TIMED
                    && !researchProgressController.isStudied(node))) {
                refreshNodeWidget(node, nowMs);
            }
        }
    }

    private void refreshNodeWidgetById(int nodeId, long nowMs) {
        if (nodeId < 0) {
            return;
        }
        ResearchNode node = getNodeById(nodeId);
        if (node != null) {
            refreshNodeWidget(node, nowMs);
        }
    }

    private void refreshNodeWidget(ResearchNode node, long nowMs) {
        UIElement widget = nodeWidgetsById.get(node.getId());
        if (widget == null) {
            return;
        }

        ResearchNodeRenderContext context = buildNodeRenderContext(node, nowMs);
        ResearchNodeVisualDefinition visualDefinition = buildNodeVisualDefinition(node, context);
        getNodeWidgetFactory().updateNodeWidget(widget, node, context, visualDefinition);
        applyNodeWidgetLayout(widget, context.getNodeWrapper());
    }

    /**
     * Builds the runtime wrapper used by widget styling, camera focus and reveal visuals.
     * <p>
     * Default behavior mirrors the logical node bounds 1:1. Subclasses may override this to
     * define style-specific card geometry without mutating the graph model itself.
     * </p>
     */
    protected ResearchNodeWrapper buildNodeWrapper(ResearchNode node, long nowMs) {
        ResearchNodeWrapper wrapper = ResearchNodeWrapper.fromNode(node);
        applyNodeWrapperResolver(node, wrapper, nowMs);
        setNodeWrapper(wrapper);
        return wrapper;
    }

    /**
     * Re-applies the current wrapper resolver to an already allocated runtime wrapper.
     * <p>
     * This is used both by normal widget refresh and by wrapper-aware auto-layout preview so both
     * systems see the same effective bounds.
     * </p>
     */
    protected final void applyNodeWrapperResolver(ResearchNode node, ResearchNodeWrapper wrapper, long nowMs) {
        ResearchNodeRenderContext preparationContext = ResearchNodeRenderContext.builder()
                .nodeWrapper(wrapper)
                .state(resolveNodeState(node))
                .visible(isNodeVisible(node.getId()))
                .highlighted(getHighlightedGroupId() != null && getHighlightedGroupId().equals(node.getGroup().getId()))
                .interactionLocked(isUnlockAnimationActive())
                .selected(selectedNodeId == node.getId() && detailsPanelTargetProgress > 0f)
                .hasNewUnlockMarker(false)
                .nowMs(nowMs)
                .progress(researchProgressController.getProgress(node))
                .unlockRevealAnimationStyle(resolveUnlockRevealAnimationStyle(node))
                .build();
        getNodeWrapperResolver().resolveWrapper(node, preparationContext);
    }

    private void applyNodeWidgetLayout(UIElement nodeWidget, ResearchNodeWrapper wrapper) {
        nodeWidget.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(wrapper.getX())
                .top(wrapper.getY())
                .width(wrapper.getWidth())
                .height(wrapper.getHeight())
        );
    }

    private ResearchNodeVisualDefinition.SizePreset resolveNodeSizePreset(ResearchNodeWrapper wrapper) {
        if (wrapper.getHeight() >= 50f || wrapper.getWidth() >= 170f) {
            return ResearchNodeVisualDefinition.SizePreset.LARGE;
        }
        if (wrapper.getHeight() <= 30f || wrapper.getWidth() <= 116f) {
            return ResearchNodeVisualDefinition.SizePreset.SMALL;
        }
        return ResearchNodeVisualDefinition.SizePreset.MEDIUM;
    }

}
