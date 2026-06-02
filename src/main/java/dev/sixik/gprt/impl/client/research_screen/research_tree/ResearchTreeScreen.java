package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.EnhancedPoseStack;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import dev.sixik.gprt.registry.GPTRSounds;
import dev.sixik.gprt.impl.client.research_screen.research_tree.layout.DependencyTreeAutoLayout;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNodeManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.AdvancedGraphView;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeManager;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Vector2f;

/**
 * Specialized research-tree screen built on top of {@link AdvancedGraphView}.
 * <p>
 * This class is the high-level "gameplay layer" of the graph:
 * it knows what a researched node is, when a node is visible, how unlock progression works,
 * how links should change color depending on state, how optional auto-layout is applied,
 * how group focus/highlight behaves, and how reveal animations play when new research becomes available.
 * </p>
 *
 * <p><b>Quick navigation through the class:</b></p>
 * <ul>
 *     <li><b>Setup / configuration:</b>
 *     {@link #ResearchTreeScreen()},
 *     {@link #ResearchTreeScreen(NodeManager, NodeLinkManager)},
 *     {@link #setAutoLayoutEnabled(boolean)},
 *     {@link #setAutoLayoutAutoFit(boolean)},
 *     {@link #autoLayoutConfig()}</li>
 *     <li><b>Batch layout control:</b>
 *     {@link #beginAutoLayoutBatch()},
 *     {@link #endAutoLayoutBatch()},
 *     {@link #isAutoLayoutSuspended()},
 *     {@link #applyAutoLayout()}</li>
 *     <li><b>Research progression API:</b>
 *     {@link #refreshResearchProgression()},
 *     {@link #setNodeStudied(int, boolean)},
 *     {@link #isNodeStudied(int)},
 *     {@link #setNodeVisibilityMode(int, ResearchNode.VisibilityMode)},
 *     {@link #isNodeUnlockedForStudy(int)},
 *     {@link #isNodeVisible(int)}</li>
 *     <li><b>Camera / group focus:</b>
 *     {@link #centerCameraOnGroup(String)},
 *     {@link #focusGroup(String, boolean)},
 *     {@link #zoomToGroup(String)},
 *     {@link #setHighlightedGroup(String)},
 *     {@link #clearHighlightedGroup()},
 *     {@link #getVisibleGroupMarkers()}</li>
 *     <li><b>Custom link rendering:</b>
 *     {@link #getLinkStartColor(ResearchLink)},
 *     {@link #getLinkEndColor(ResearchLink)},
 *     {@link #getLinkRenderPriority(ResearchLink, ResearchNode, ResearchNode)},
 *     {@link #buildLinkRenderData(ResearchLink, ResearchNode, ResearchNode)},
 *     {@link #buildLinkRoute(ResearchLink, ResearchNode, ResearchNode)}</li>
 *     <li><b>Reveal animation flow:</b>
 *     {@link #prepareRevealAnimationState(IntOpenHashSet, boolean)},
 *     {@link #startNextRevealAnimation()},
 *     {@link #updateRevealAnimationState()},
 *     {@link #finishActiveRevealAnimation()},
 *     {@link #drawActiveRevealLinks(GUIContext)},
 *     {@link #drawRevealLinkProgress(GUIContext, ResearchLink, ResearchNode, ResearchNode, float)}</li>
 *     <li><b>Visibility / unlock logic:</b>
 *     {@link #syncResearchNodeVisibility()},
 *     {@link #collectVisibleNodeIds()},
 *     {@link #isNodeVisible(ResearchNode)},
 *     {@link #isNodeUnlockedForStudy(ResearchNode)},
 *     {@link #hasAnyStudiedParent(ResearchLink[])},
 *     {@link #areAllParentsStudied(ResearchLink[])}</li>
 *     <li><b>Group ordering / bounds helpers:</b>
 *     {@link #compactAutoLayoutGroups()},
 *     {@link #collectStableGroupOrder()},
 *     {@link #collectVisibleGroupBounds()},
 *     {@link #getVisibleGroupBounds(String)}</li>
 * </ul>
 *
 * <p><b>Main responsibilities:</b></p>
 * <ul>
 *     <li>Keep node widgets attached only when they should really be visible in progression.</li>
 *     <li>Apply stable layout rules, so the tree does not constantly jump around after unlocks.</li>
 *     <li>Render links with state-aware colors and priorities:
 *     studied links are drawn above available ones, and available ones above locked ones.</li>
 *     <li>Separate incoming links from different groups into parallel lanes, so cross-group routes are
 *     easier to read and do not sit directly on top of each other.</li>
 *     <li>Handle cinematic reveal sequences for newly unlocked nodes:
 *     camera move, node drop/scale, delayed link connection and temporary input lock.</li>
 * </ul>
 *
 * <p><b>Useful maintenance notes for future you:</b></p>
 * <ul>
 *     <li>If unlock logic starts behaving strangely, inspect the visibility methods first; they are the
 *     source of truth for both node attachment and reveal queue generation.</li>
 *     <li>If lines look wrong, check {@link #buildLinkRoute(ResearchLink, ResearchNode, ResearchNode)}
 *     before touching render code elsewhere, because both cached rendering and reveal rendering rely on it.</li>
 *     <li>If group order drifts or branches start mixing visually, inspect
 *     {@link #compactAutoLayoutGroups()} and {@link #collectStableGroupOrder()}.</li>
 *     <li>If the camera behaves unexpectedly during unlocks, the reveal lifecycle methods are the correct
 *     place to debug rather than the generic camera helpers in the parent class.</li>
 * </ul>
 */
public class ResearchTreeScreen extends AdvancedGraphView<
        ResearchNode,
        ObjectArrayList<ResearchNode>,
        ResearchLink,
        ObjectArrayList<ResearchLink>
> {

    private static final int DEFAULT_GROUP_COLOR = 0xFF87D4FF;
    private static final int AVAILABLE_STATE_COLOR = 0xFFE05555;
    private static final float DEFAULT_GROUP_BOUNDS_PADDING_X = 14f;
    private static final float DEFAULT_GROUP_BOUNDS_PADDING_Y = 20f;
    private static final float DEFAULT_GROUP_FOCUS_MIN_SCALE = 0.35f;
    private static final float GROUP_NODE_HIGHLIGHT_PADDING = 4f;
    private static final float GROUP_NODE_HIGHLIGHT_WIDTH = 2f;
    private static final float GROUP_NODE_HIGHLIGHT_GLOW_WIDTH = 1f;
    private static final long GROUP_HIGHLIGHT_BLINK_DURATION_MS = 4_000L;
    private static final long REVEAL_CAMERA_DURATION_MS = 480L;
    private static final long REVEAL_NODE_DELAY_MS = 180L;
    private static final long REVEAL_NODE_DURATION_MS = 720L;
    private static final long REVEAL_LINK_DELAY_MS = 430L;
    private static final long REVEAL_LINK_DURATION_MS = 460L;
    private static final long REVEAL_STEP_DURATION_MS = 1_080L;
    private static final float REVEAL_NODE_START_SCALE = 3.0f;
    private static final float REVEAL_NODE_START_Y = -52f;

    protected enum ResearchLinkRenderState {
        LOCKED(0),
        AVAILABLE(1),
        STUDIED(2);

        private final int renderPriority;

        ResearchLinkRenderState(int renderPriority) {
            this.renderPriority = renderPriority;
        }

        public int renderPriority() {
            return renderPriority;
        }
    }

    private final DependencyTreeAutoLayout.Config autoLayoutConfig = new DependencyTreeAutoLayout.Config();
    private boolean autoLayoutEnabled;
    private boolean autoLayoutAutoFit = true;
    private @Nullable String highlightedGroupId;
    private long highlightedGroupBlinkStartedAtMs;
    private int autoLayoutSuspendDepth;
    private final IntArrayList queuedRevealNodeIds = new IntArrayList();
    private final IntOpenHashSet queuedRevealNodeIdSet = new IntOpenHashSet();
    private @Nullable RevealAnimation activeRevealAnimation;

    public ResearchTreeScreen() {
        this(new ResearchNodeManager(), new ResearchNodeLinkManager());
    }

    public ResearchTreeScreen(NodeManager<ResearchNode, ObjectArrayList<ResearchNode>> nodeManager,
                              NodeLinkManager<ResearchLink, ObjectArrayList<ResearchLink>> linkManager
    ) {
        super(nodeManager, linkManager);
    }

    /**
     * Enables or disables automatic dependency-based layout for this screen.
     * <p>
     * When enabled, node positions are rebuilt from the dependency graph and then progression visuals
     * are refreshed. When disabled, existing node coordinates stay as-is and only progression state is updated.
     * </p>
     */
    public ResearchTreeScreen setAutoLayoutEnabled(boolean autoLayoutEnabled) {
        this.autoLayoutEnabled = autoLayoutEnabled;
        if (autoLayoutEnabled && !isAutoLayoutSuspended()) {
            applyAutoLayout();
        } else if (!autoLayoutEnabled && !isAutoLayoutSuspended()) {
            refreshResearchProgression();
        }
        return this;
    }

    public boolean isAutoLayoutEnabled() {
        return autoLayoutEnabled;
    }

    /**
     * Controls whether rebuilds also try to fit the camera to visible content.
     */
    public ResearchTreeScreen setAutoLayoutAutoFit(boolean autoLayoutAutoFit) {
        this.autoLayoutAutoFit = autoLayoutAutoFit;
        return this;
    }

    public DependencyTreeAutoLayout.Config autoLayoutConfig() {
        return autoLayoutConfig;
    }

    /**
     * Begins a batch where auto-layout/progression refresh is deferred until the matching end call.
     */
    public void beginAutoLayoutBatch() {
        autoLayoutSuspendDepth++;
    }

    /**
     * Ends one level of batch suppression and runs the deferred rebuild when the outermost batch ends.
     */
    public void endAutoLayoutBatch() {
        if (autoLayoutSuspendDepth > 0) {
            autoLayoutSuspendDepth--;
        }

        if (autoLayoutSuspendDepth == 0) {
            if (autoLayoutEnabled) {
                applyAutoLayout();
            } else {
                refreshResearchProgression();
            }
        }
    }

    public boolean isAutoLayoutSuspended() {
        return autoLayoutSuspendDepth > 0;
    }

    /**
     * Recomputes node positions from dependencies using the current auto-layout config.
     */
    public void applyAutoLayout() {
        applyAutoLayout(null, false);
    }

    private void applyAutoLayout(@Nullable IntOpenHashSet visibleBefore, boolean animateNewNodes) {
        DependencyTreeAutoLayout.apply(nodes, links, autoLayoutConfig);
        compactAutoLayoutGroups();
        syncAllNodeWidgetBounds();
        prepareRevealAnimationState(visibleBefore, animateNewNodes);
        syncResearchNodeVisibility();
        invalidateLinkGeometry();
        onResearchProgressionUpdated();

        if (autoLayoutAutoFit && !isRevealSequenceActive() && getContentWidth() > 0 && getContentHeight() > 0) {
            fitToChildren(80f, 0.35f);
        }
    }

    /**
     * Re-evaluates visibility, widget attachment and cached link geometry without changing studied state.
     */
    public ResearchTreeScreen refreshResearchProgression() {
        return refreshResearchProgression(null, false);
    }

    private ResearchTreeScreen refreshResearchProgression(@Nullable IntOpenHashSet visibleBefore, boolean animateNewNodes) {
        syncAllNodeWidgetBounds();
        prepareRevealAnimationState(visibleBefore, animateNewNodes);
        syncResearchNodeVisibility();
        invalidateLinkGeometry();
        onResearchProgressionUpdated();

        if (autoLayoutAutoFit && !isRevealSequenceActive() && getContentWidth() > 0 && getContentHeight() > 0) {
            fitToChildren(80f, 0.35f);
        }
        return this;
    }

    /**
     * Changes whether a node is studied and refreshes progression.
     * <p>
     * When a node becomes studied, newly visible nodes can be queued for reveal animation.
     * </p>
     */
    public ResearchTreeScreen setNodeStudied(int nodeId, boolean studied) {
        ResearchNode node = getNodeById(nodeId);
        if (node == null || node.isStudied() == studied) {
            return this;
        }

        IntOpenHashSet visibleBefore = studied ? collectVisibleNodeIds() : null;
        node.setStudied(studied);
        requestProgressionRefresh(visibleBefore, studied);
        return this;
    }

    public boolean isNodeStudied(int nodeId) {
        ResearchNode node = getNodeById(nodeId);
        return node != null && node.isStudied();
    }

    /**
     * Changes the visibility rule of a node and immediately refreshes progression visuals.
     */
    public ResearchTreeScreen setNodeVisibilityMode(int nodeId, ResearchNode.VisibilityMode visibilityMode) {
        ResearchNode node = getNodeById(nodeId);
        if (node == null) {
            return this;
        }

        node.setVisibilityMode(visibilityMode);
        requestProgressionRefresh();
        return this;
    }

    public boolean isNodeUnlockedForStudy(int nodeId) {
        ResearchNode node = getNodeById(nodeId);
        return node != null && isNodeUnlockedForStudy(node);
    }

    public boolean isNodeVisible(int nodeId) {
        ResearchNode node = getNodeById(nodeId);
        return node != null && isNodeVisible(node);
    }

    /**
     * Centers the camera on the visible bounds of the given research group.
     */
    public boolean centerCameraOnGroup(String groupId) {
        GroupBounds bounds = getVisibleGroupBounds(groupId);
        if (bounds == null) {
            return false;
        }

        centerCameraOn(bounds.centerX(), bounds.centerY());
        return true;
    }

    /**
     * Highlights a group and either centers or zooms the camera onto it.
     */
    public boolean focusGroup(String groupId, boolean zoomToGroup) {
        setHighlightedGroup(groupId);
        if (zoomToGroup) {
            return zoomToGroup(groupId, DEFAULT_GROUP_FOCUS_MIN_SCALE);
        }
        return centerCameraOnGroup(groupId);
    }

    public boolean zoomToGroup(String groupId) {
        return zoomToGroup(groupId, DEFAULT_GROUP_FOCUS_MIN_SCALE);
    }

    /**
     * Fits the camera around one visible group while respecting a minimum zoom bound.
     */
    public boolean zoomToGroup(String groupId, float minScaleBound) {
        GroupBounds bounds = getVisibleGroupBounds(groupId);
        if (bounds == null) {
            return false;
        }

        fit(bounds.minX(), bounds.minY(), bounds.maxX(), bounds.maxY(), minScaleBound);
        return true;
    }

    public boolean isGroupVisible(String groupId) {
        return getVisibleGroupBounds(groupId) != null;
    }

    /**
     * Returns {@code true} while reveal animations are active or queued.
     * <p>
     * This is the main flag used to lock input during cinematic unlocks.
     * </p>
     */
    public boolean isRevealSequenceActive() {
        return activeRevealAnimation != null || !queuedRevealNodeIds.isEmpty();
    }

    /**
     * Starts the temporary blinking highlight for one group.
     */
    public ResearchTreeScreen setHighlightedGroup(@Nullable String groupId) {
        highlightedGroupId = groupId;
        highlightedGroupBlinkStartedAtMs = groupId == null ? 0L : System.currentTimeMillis();
        return this;
    }

    public @Nullable String getHighlightedGroupId() {
        return highlightedGroupId;
    }

    public ResearchTreeScreen clearHighlightedGroup() {
        highlightedGroupId = null;
        highlightedGroupBlinkStartedAtMs = 0L;
        return this;
    }

    /**
     * Returns visible group bounds in a UI-friendly form for overlays and navigation lists.
     */
    public Collection<GroupMarkerLayout> getVisibleGroupMarkers() {
        ObjectArrayList<GroupBounds> bounds = collectVisibleGroupBounds();
        ObjectArrayList<GroupMarkerLayout> markers = new ObjectArrayList<>(bounds.size());
        for (int i = 0, size = bounds.size(); i < size; i++) {
            GroupBounds groupBounds = bounds.get(i);
            markers.add(new GroupMarkerLayout(
                    groupBounds.group(),
                    groupBounds.minX(),
                    groupBounds.minY(),
                    groupBounds.maxX(),
                    groupBounds.maxY()
            ));
        }
        markers.sort(Comparator.comparing(marker -> marker.group().getTitle()));
        return markers;
    }

    @Override
    public void addNode(ResearchNode node) {
        super.addNode(node);
        requestAutoLayout();
        if (!autoLayoutEnabled && !isAutoLayoutSuspended()) {
            refreshResearchProgression();
        }
    }

    @Override
    public void addLink(ResearchLink link) {
        super.addLink(link);
        requestAutoLayout();
        if (!autoLayoutEnabled && !isAutoLayoutSuspended()) {
            refreshResearchProgression();
        }
    }

    @Override
    public boolean removeNode(ResearchNode node) {
        boolean removed = super.removeNode(node);
        if (removed) {
            requestAutoLayout();
            if (!autoLayoutEnabled && !isAutoLayoutSuspended()) {
                refreshResearchProgression();
            }
        }
        return removed;
    }

    @Override
    public boolean removeLink(ResearchLink link) {
        boolean removed = super.removeLink(link);
        if (removed) {
            requestAutoLayout();
            if (!autoLayoutEnabled && !isAutoLayoutSuspended()) {
                refreshResearchProgression();
            }
        }
        return removed;
    }

    @Override
    protected int getLinkStartColor(ResearchLink link) {
        ResearchNode from = getNodeById(link.getNodeFrom());
        ResearchNode to = getNodeById(link.getNodeTo());
        if (from == null || to == null) {
            return DEFAULT_GROUP_COLOR;
        }

        ResearchLinkRenderState renderState = resolveLinkRenderState(link, from, to);
        int groupColor = getLinkStartGroupColor(link, from, to);
        return getLinkStartRenderColor(link, renderState, from, to, groupColor);
    }

    @Override
    protected int getLinkEndColor(ResearchLink link) {
        ResearchNode from = getNodeById(link.getNodeFrom());
        ResearchNode to = getNodeById(link.getNodeTo());
        if (from == null || to == null) {
            return DEFAULT_GROUP_COLOR;
        }

        ResearchLinkRenderState renderState = resolveLinkRenderState(link, from, to);
        int groupColor = getLinkEndGroupColor(link, from, to);
        return getLinkEndRenderColor(link, renderState, from, to, groupColor);
    }

    @Override
    protected int getLinkRenderPriority(ResearchLink link, ResearchNode from, ResearchNode to) {
        return resolveLinkRenderState(link, from, to).renderPriority();
    }

    /**
     * Builds cached render geometry for one research link.
     * <p>
     * Hidden links and links currently owned by the reveal animation are skipped, while all others
     * are routed through {@link #buildLinkRoute(ResearchLink, ResearchNode, ResearchNode)}.
     * </p>
     */
    @Override
    protected @Nullable LinkRenderData buildLinkRenderData(ResearchLink link, ResearchNode from, ResearchNode to) {
        if (!isNodeVisible(from) || !isNodeVisible(to) || isLinkHandledByRevealAnimation(link, to)) {
            return null;
        }

        int renderPriority = getLinkRenderPriority(link, from, to);
        LinkRenderData renderData = createLinkRenderData(link, renderPriority);
        LinkRoute route = buildLinkRoute(link, from, to);
        for (int i = 0, size = route.segments().size(); i < size; i++) {
            LinkRouteSegment segment = route.segments().get(i);
            addLinkPolyline(
                    renderData,
                    segment.startColor(),
                    segment.endColor(),
                    segment.width(),
                    segment.x1(),
                    segment.y1(),
                    segment.x2(),
                    segment.y2()
            );
        }
        return renderData;
    }

    /**
     * Updates reveal state and then draws the normal graph plus reveal/highlight overlays.
     */
    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        updateRevealAnimationState();
        super.drawBackgroundAdditional(guiContext);
        drawActiveRevealLinks(guiContext);
        drawHighlightedGroupBounds(guiContext);
    }

    /**
     * Advances reveal animation state once per tick.
     */
    @Override
    public void screenTick() {
        updateRevealAnimationState();
        super.screenTick();
    }

    @Override
    protected void onMouseDown(UIEvent event) {
        if (activeRevealAnimation != null) {
            return;
        }
        super.onMouseDown(event);
    }

    @Override
    protected void onDragSourceUpdate(UIEvent event) {
        if (activeRevealAnimation != null) {
            return;
        }
        super.onDragSourceUpdate(event);
    }

    @Override
    protected void onMouseWheel(UIEvent event) {
        if (activeRevealAnimation != null) {
            return;
        }
        super.onMouseWheel(event);
    }

    private void requestAutoLayout() {
        if (autoLayoutEnabled && !isAutoLayoutSuspended()) {
            applyAutoLayout();
        }
    }

    private void requestProgressionRefresh() {
        requestProgressionRefresh(null, false);
    }

    private void requestProgressionRefresh(@Nullable IntOpenHashSet visibleBefore, boolean animateNewNodes) {
        if (isAutoLayoutSuspended()) {
            return;
        }

        refreshResearchProgression(visibleBefore, animateNewNodes);
    }

    private void syncResearchNodeVisibility() {
        for (ResearchNode node : nodes) {
            setNodeWidgetAttached(node.getId(), isNodeVisible(node) && !isNodeWaitingForReveal(node.getId()));
        }
    }

    private void prepareRevealAnimationState(@Nullable IntOpenHashSet visibleBefore, boolean animateNewNodes) {
        if (!animateNewNodes) {
            clearRevealAnimations();
            return;
        }

        enqueueNewlyVisibleNodes(visibleBefore);
        if (activeRevealAnimation == null) {
            startNextRevealAnimation();
        }
    }

    private void enqueueNewlyVisibleNodes(@Nullable IntOpenHashSet visibleBefore) {
        if (visibleBefore == null) {
            return;
        }

        ObjectArrayList<ResearchNode> newlyVisibleNodes = new ObjectArrayList<>();
        for (ResearchNode node : nodes) {
            if (!node.isStudied() && isNodeVisible(node) && !visibleBefore.contains(node.getId())) {
                newlyVisibleNodes.add(node);
            }
        }

        newlyVisibleNodes.sort(Comparator
                .comparingDouble(ResearchNode::getX)
                .thenComparingDouble(ResearchNode::getY)
                .thenComparingInt(ResearchNode::getId));

        Set<Integer> alreadyQueued = new HashSet<>();
        for (int i = 0, size = queuedRevealNodeIds.size(); i < size; i++) {
            alreadyQueued.add(queuedRevealNodeIds.getInt(i));
        }
        if (activeRevealAnimation != null) {
            alreadyQueued.add(activeRevealAnimation.nodeId());
        }

        for (int i = 0, size = newlyVisibleNodes.size(); i < size; i++) {
            ResearchNode node = newlyVisibleNodes.get(i);
            if (alreadyQueued.add(node.getId())) {
                queuedRevealNodeIds.add(node.getId());
                queuedRevealNodeIdSet.add(node.getId());
            }
        }
    }

    private void clearRevealAnimations() {
        if (activeRevealAnimation != null) {
            resetNodeRevealTransform(activeRevealAnimation.nodeId());
        }
        activeRevealAnimation = null;
        queuedRevealNodeIds.clear();
        queuedRevealNodeIdSet.clear();
    }

    private void startNextRevealAnimation() {
        if (queuedRevealNodeIds.isEmpty()) {
            activeRevealAnimation = null;
            return;
        }

        int nodeId = queuedRevealNodeIds.removeInt(0);
        queuedRevealNodeIdSet.remove(nodeId);
        ResearchNode node = getNodeById(nodeId);
        if (node == null || !isNodeVisible(node)) {
            startNextRevealAnimation();
            return;
        }

        float targetOffsetX = computeCenteredOffsetX(node.centerX());
        float targetOffsetY = computeCenteredOffsetY(node.centerY());
        activeRevealAnimation = new RevealAnimation(
                nodeId,
                System.currentTimeMillis(),
                getOffsetX(),
                getOffsetY(),
                targetOffsetX,
                targetOffsetY,
                false,
                false,
                false
        );

        playRevealSound(GPTRSounds.SUCK_IN.get(), 1.0f, 0.85f);
        applyNodeRevealTransform(nodeId, 0f);
        syncResearchNodeVisibility();
        invalidateLinkGeometry();
        onResearchProgressionUpdated();
    }

    private void updateRevealAnimationState() {
        if (activeRevealAnimation == null) {
            return;
        }

        ResearchNode node = getNodeById(activeRevealAnimation.nodeId());
        if (node == null || !isNodeVisible(node)) {
            clearRevealAnimations();
            syncResearchNodeVisibility();
            invalidateLinkGeometry();
            onResearchProgressionUpdated();
            return;
        }

        long now = System.currentTimeMillis();
        float elapsed = now - activeRevealAnimation.startedAtMs();

        float cameraProgress = clamp01(elapsed / (float) REVEAL_CAMERA_DURATION_MS);
        float cameraEase = easeInOutCubic(cameraProgress);
        setOffsetX(lerp(activeRevealAnimation.cameraStartOffsetX(), activeRevealAnimation.cameraTargetOffsetX(), cameraEase));
        setOffsetY(lerp(activeRevealAnimation.cameraStartOffsetY(), activeRevealAnimation.cameraTargetOffsetY(), cameraEase));
        syncCameraTransform();

        float nodeProgress = clamp01((elapsed - REVEAL_NODE_DELAY_MS) / (float) REVEAL_NODE_DURATION_MS);
        applyNodeRevealTransform(node.getId(), nodeProgress);

        if (!activeRevealAnimation.nodeDropSoundPlayed() && elapsed >= REVEAL_NODE_DELAY_MS) {
            playRevealSound(GPTRSounds.SPIT_OUT.get(), 1.0f, 1.05f);
            activeRevealAnimation = activeRevealAnimation.withNodeDropSoundPlayed();
        }

        if (!activeRevealAnimation.linkConnectSoundPlayed() && elapsed >= REVEAL_LINK_DELAY_MS) {
            playRevealSound(GPTRSounds.SUCK_IN.get(), 1.0f, 1.18f);
            activeRevealAnimation = activeRevealAnimation.withLinkConnectSoundPlayed();
        }

        if (elapsed >= REVEAL_STEP_DURATION_MS) {
            finishActiveRevealAnimation();
        }
    }

    private void finishActiveRevealAnimation() {
        if (activeRevealAnimation == null) {
            return;
        }

        resetNodeRevealTransform(activeRevealAnimation.nodeId());
        activeRevealAnimation = null;
        syncResearchNodeVisibility();
        invalidateLinkGeometry();
        startNextRevealAnimation();
        onResearchProgressionUpdated();
    }

    private void applyNodeRevealTransform(int nodeId, float progress) {
        UIElement widget = getNodeWidget(nodeId);
        if (widget == null) {
            return;
        }

        float easedScale = easeOutBack(progress);
        float easedDrop = easeOutBounce(progress);
        float scale = lerp(REVEAL_NODE_START_SCALE, 1.0f, easedScale);
        float translateY = lerp(REVEAL_NODE_START_Y, 0f, easedDrop);

        widget.style(style -> style.transform2D(new Transform2D()
                .pivot(0.5f, 0.5f)
                .translate(0f, translateY)
                .scale(scale)));
    }

    private void resetNodeRevealTransform(int nodeId) {
        UIElement widget = getNodeWidget(nodeId);
        if (widget == null) {
            return;
        }

        widget.style(style -> style.transform2D(Transform2D.identity()));
    }

    private void drawActiveRevealLinks(GUIContext guiContext) {
        if (activeRevealAnimation == null) {
            return;
        }

        ResearchNode node = getNodeById(activeRevealAnimation.nodeId());
        if (node == null || !isNodeVisible(node)) {
            return;
        }

        long now = System.currentTimeMillis();
        float lineProgress = clamp01((now - activeRevealAnimation.startedAtMs() - REVEAL_LINK_DELAY_MS) / (float) REVEAL_LINK_DURATION_MS);
        if (lineProgress <= 0f) {
            return;
        }

        EnhancedPoseStack pose = guiContext.pose;
        pose.pushPose();
        pose.translate(getContentX(), getContentY(), 0f);
        pose.scale(getScale(), getScale(), 1f);
        pose.translate(-getOffsetX(), -getOffsetY(), 0f);

        ResearchLink[] parentLinks = getLinksToNode(node.getId());
        for (ResearchLink parentLink : parentLinks) {
            ResearchNode parent = getNodeById(parentLink.getNodeFrom());
            if (parent == null || !isNodeVisible(parent)) {
                continue;
            }
            drawRevealLinkProgress(guiContext, parentLink, parent, node, lineProgress);
        }

        pose.popPose();
    }

    private void drawRevealLinkProgress(GUIContext guiContext,
                                        ResearchLink link,
                                        ResearchNode from,
                                        ResearchNode to,
                                        float progress
    ) {
        LinkRoute route = buildLinkRoute(link, from, to);
        float remaining = route.totalLength() * progress;
        for (int i = 0, size = route.segments().size(); i < size && remaining > 0f; i++) {
            LinkRouteSegment segment = route.segments().get(i);
            float segmentLength = segment.length();
            float consumed = Math.min(segmentLength, remaining);
            float segmentProgress = segmentLength <= 0.0001f ? 1f : consumed / segmentLength;
            float currentX = lerp(segment.x1(), segment.x2(), segmentProgress);
            float currentY = lerp(segment.y1(), segment.y2(), segmentProgress);

            DrawerHelper.drawLines(
                    guiContext.graphics,
                    List.of(new Vector2f(segment.x1(), segment.y1()), new Vector2f(currentX, currentY)),
                    segment.startColor(),
                    interpolateColor(segment.startColor(), segment.endColor(), segmentProgress),
                    segment.width()
            );
            remaining -= consumed;
        }
    }

    private boolean isNodeWaitingForReveal(int nodeId) {
        return queuedRevealNodeIdSet.contains(nodeId);
    }

    private boolean isLinkHandledByRevealAnimation(ResearchLink link, ResearchNode to) {
        if (queuedRevealNodeIdSet.contains(to.getId())) {
            return true;
        }
        return activeRevealAnimation != null && activeRevealAnimation.nodeId() == to.getId();
    }

    private IntOpenHashSet collectVisibleNodeIds() {
        IntOpenHashSet visibleNodeIds = new IntOpenHashSet(nodes.size());
        for (ResearchNode node : nodes) {
            if (isNodeVisible(node)) {
                visibleNodeIds.add(node.getId());
            }
        }
        return visibleNodeIds;
    }

    private float computeCenteredOffsetX(float worldX) {
        float halfVisibleWidth = getContentWidth() / (2f * getScale());
        return worldX - halfVisibleWidth;
    }

    private float computeCenteredOffsetY(float worldY) {
        float halfVisibleHeight = getContentHeight() / (2f * getScale());
        return worldY - halfVisibleHeight;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4f * t * t * t
                : 1f - (float) Math.pow(-2f * t + 2f, 3f) * 0.5f;
    }

    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float p = t - 1f;
        return 1f + c3 * p * p * p + c1 * p * p;
    }

    private float easeOutBounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (t < 1f / d1) {
            return n1 * t * t;
        } else if (t < 2f / d1) {
            float p = t - 1.5f / d1;
            return n1 * p * p + 0.75f;
        } else if (t < 2.5f / d1) {
            float p = t - 2.25f / d1;
            return n1 * p * p + 0.9375f;
        } else {
            float p = t - 2.625f / d1;
            return n1 * p * p + 0.984375f;
        }
    }

    private void playRevealSound(net.minecraft.sounds.SoundEvent soundEvent, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getSoundManager() == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch, volume));
    }

    private void drawHighlightedGroupBounds(GUIContext guiContext) {
        if (highlightedGroupId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (highlightedGroupBlinkStartedAtMs <= 0L
                || now - highlightedGroupBlinkStartedAtMs >= GROUP_HIGHLIGHT_BLINK_DURATION_MS) {
            return;
        }

        GroupBounds bounds = getVisibleGroupBounds(highlightedGroupId);
        if (bounds == null) {
            return;
        }

        float pulse = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(now * 0.012d));
        int baseColor = mixColors(bounds.group().getPrimaryColor(), bounds.group().getSecondaryColor(), 0.45f);
        int highlightColor = withAlpha(baseColor, Math.max(90, Math.round(255f * pulse)));
        int glowColor = withAlpha(baseColor, Math.max(42, Math.round(120f * pulse)));

        EnhancedPoseStack pose = guiContext.pose;
        pose.pushPose();
        pose.translate(getContentX(), getContentY(), 0f);
        pose.scale(getScale(), getScale(), 1f);
        pose.translate(-getOffsetX(), -getOffsetY(), 0f);

        for (ResearchNode node : nodes) {
            if (!isNodeVisible(node) || !node.getGroup().getId().equals(highlightedGroupId)) {
                continue;
            }

            float outlineX = node.getX() - GROUP_NODE_HIGHLIGHT_PADDING;
            float outlineY = node.getY() - GROUP_NODE_HIGHLIGHT_PADDING;
            float outlineWidth = node.getWidth() + GROUP_NODE_HIGHLIGHT_PADDING * 2f;
            float outlineHeight = node.getHeight() + GROUP_NODE_HIGHLIGHT_PADDING * 2f;

            // Glow pass рисуется через прямоугольные полосы, поэтому нет щелей на стыках толстых углов.
            DrawerHelper.drawBorder(
                    guiContext.graphics,
                    outlineX - GROUP_NODE_HIGHLIGHT_GLOW_WIDTH,
                    outlineY - GROUP_NODE_HIGHLIGHT_GLOW_WIDTH,
                    outlineWidth + GROUP_NODE_HIGHLIGHT_GLOW_WIDTH * 2f,
                    outlineHeight + GROUP_NODE_HIGHLIGHT_GLOW_WIDTH * 2f,
                    glowColor,
                    Math.round(GROUP_NODE_HIGHLIGHT_WIDTH + GROUP_NODE_HIGHLIGHT_GLOW_WIDTH)
            );

            DrawerHelper.drawBorder(
                    guiContext.graphics,
                    outlineX,
                    outlineY,
                    outlineWidth,
                    outlineHeight,
                    highlightColor,
                    Math.round(GROUP_NODE_HIGHLIGHT_WIDTH)
            );
        }

        pose.popPose();
    }

    protected int getNodeGroupColor(ResearchNode node) {
        return getNodeGroupPrimaryColor(node);
    }

    protected int getNodeGroupPrimaryColor(ResearchNode node) {
        return node.getGroup().getPrimaryColor();
    }

    protected int getNodeGroupSecondaryColor(ResearchNode node) {
        return node.getGroup().getSecondaryColor();
    }

    protected int getLinkStartGroupColor(ResearchLink link, ResearchNode from, ResearchNode to) {
        return getNodeGroupColor(from);
    }

    protected int getLinkEndGroupColor(ResearchLink link, ResearchNode from, ResearchNode to) {
        return areNodesInSameGroup(from, to) ? getNodeGroupColor(from) : getNodeGroupColor(to);
    }

    protected int getLinkStartRenderColor(ResearchLink link,
                                          ResearchLinkRenderState renderState,
                                          ResearchNode from,
                                          ResearchNode to,
                                          int groupColor
    ) {
        return applyLinkRenderStateColor(groupColor, renderState);
    }

    protected int getLinkEndRenderColor(ResearchLink link,
                                        ResearchLinkRenderState renderState,
                                        ResearchNode from,
                                        ResearchNode to,
                                        int groupColor
    ) {
        return applyLinkRenderStateColor(groupColor, renderState);
    }

    protected ResearchLinkRenderState resolveLinkRenderState(ResearchLink link) {
        ResearchNode from = getNodeById(link.getNodeFrom());
        ResearchNode to = getNodeById(link.getNodeTo());
        if (from == null || to == null) {
            return ResearchLinkRenderState.LOCKED;
        }
        return resolveLinkRenderState(link, from, to);
    }

    protected ResearchLinkRenderState resolveLinkRenderState(ResearchLink link, ResearchNode from, ResearchNode to) {
        if (from.isStudied() && to.isStudied()) {
            return ResearchLinkRenderState.STUDIED;
        }

        if (from.isStudied() && !to.isStudied() && isNodeUnlockedForStudy(to)) {
            return ResearchLinkRenderState.AVAILABLE;
        }

        return ResearchLinkRenderState.LOCKED;
    }

    protected boolean areNodesInSameGroup(ResearchNode first, ResearchNode second) {
        return first.getGroup().getId().equals(second.getGroup().getId());
    }

    protected int applyLinkRenderStateColor(int groupColor, ResearchLinkRenderState renderState) {
        return switch (renderState) {
            case STUDIED -> groupColor;
            case AVAILABLE -> mixColors(groupColor, AVAILABLE_STATE_COLOR, 0.72f);
            case LOCKED -> multiplyColor(groupColor, 0.55f);
        };
    }

    protected int mixColors(int firstColor, int secondColor, float secondWeight) {
        float clampedWeight = Math.max(0f, Math.min(1f, secondWeight));
        float firstWeight = 1f - clampedWeight;

        int alpha = Math.round(((firstColor >>> 24) & 0xFF) * firstWeight + ((secondColor >>> 24) & 0xFF) * clampedWeight);
        int red = Math.round(((firstColor >>> 16) & 0xFF) * firstWeight + ((secondColor >>> 16) & 0xFF) * clampedWeight);
        int green = Math.round(((firstColor >>> 8) & 0xFF) * firstWeight + ((secondColor >>> 8) & 0xFF) * clampedWeight);
        int blue = Math.round((firstColor & 0xFF) * firstWeight + (secondColor & 0xFF) * clampedWeight);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    protected int multiplyColor(int color, float multiplier) {
        float clampedMultiplier = Math.max(0f, multiplier);
        int alpha = (color >>> 24) & 0xFF;
        int red = Math.min(255, Math.round(((color >>> 16) & 0xFF) * clampedMultiplier));
        int green = Math.min(255, Math.round(((color >>> 8) & 0xFF) * clampedMultiplier));
        int blue = Math.min(255, Math.round((color & 0xFF) * clampedMultiplier));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    protected int withAlpha(int color, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return (clampedAlpha << 24) | (color & 0x00FFFFFF);
    }

    protected int interpolateColor(int startColor, int endColor, float progress) {
        return mixColors(startColor, endColor, clamp01(progress));
    }

    /**
     * Builds the full multi-segment route of one dependency link.
     * <p>
     * Both cached rendering and reveal-animation rendering use this method, so future shape changes
     * should usually be implemented here first.
     * </p>
     */
    private LinkRoute buildLinkRoute(ResearchLink link, ResearchNode from, ResearchNode to) {
        float startX = from.getX() + from.getWidth();
        float startY = from.centerY();
        float endX = to.getX();
        float endY = resolveIncomingLinkAttachY(link, from, to);

        int startColor = getLinkStartColor(link);
        int endColor = getLinkEndColor(link);
        float width = getLinkWidth(link);
        float halfWidth = width * 0.5f;

        ObjectArrayList<LinkRouteSegment> segments = new ObjectArrayList<>(5);
        if (Math.abs(startY - endY) < 1.0f) {
            addRouteSegment(segments, startColor, endColor, width, startX, startY, endX, endY);
            return new LinkRoute(segments);
        }

        IncomingGroupLayout layout = getIncomingGroupLayout(to);
        if (layout.groupIds().size() <= 1) {
            float middleX = startX + (endX - startX) * 0.5f;
            addRouteSegment(segments, startColor, startColor, width, startX, startY, middleX + halfWidth, startY);
            addRouteSegment(segments, startColor, endColor, width, middleX, startY, middleX, endY);
            addRouteSegment(segments, endColor, endColor, width, middleX - halfWidth, endY, endX, endY);
            return new LinkRoute(segments);
        }

        float startLaneX = resolveIncomingLinkStartLaneX(link, from, to, startX, endX, layout);
        float endLaneX = resolveIncomingLinkEndLaneX(link, from, to, startX, endX, layout);
        float laneY = resolveIncomingLinkLaneY(link, from, to, layout);

        if (endLaneX - startLaneX <= Math.max(10f, width * 2f)) {
            float middleX = (startLaneX + endLaneX) * 0.5f;
            startLaneX = middleX;
            endLaneX = middleX;
        }

        addRouteSegment(segments, startColor, startColor, width, startX, startY, startLaneX + halfWidth, startY);
        addRouteSegment(segments, startColor, startColor, width, startLaneX, startY, startLaneX, laneY);
        addRouteSegment(segments, startColor, endColor, width, startLaneX, laneY, endLaneX, laneY);
        addRouteSegment(segments, endColor, endColor, width, endLaneX, laneY, endLaneX, endY);
        addRouteSegment(segments, endColor, endColor, width, endLaneX - halfWidth, endY, endX, endY);
        return new LinkRoute(segments);
    }

    private void addRouteSegment(ObjectArrayList<LinkRouteSegment> segments,
                                 int startColor,
                                 int endColor,
                                 float width,
                                 float x1,
                                 float y1,
                                 float x2,
                                 float y2
    ) {
        if (Math.abs(x1 - x2) < 0.0001f && Math.abs(y1 - y2) < 0.0001f) {
            return;
        }
        segments.add(new LinkRouteSegment(x1, y1, x2, y2, startColor, endColor, width));
    }

    private float resolveIncomingLinkAttachY(ResearchLink link, ResearchNode from, ResearchNode to) {
        IncomingGroupLayout layout = getIncomingGroupLayout(to);
        if (layout.groupIds().size() <= 1) {
            return to.centerY();
        }

        int groupIndex = layout.groupIds().indexOf(from.getGroup().getId());
        if (groupIndex < 0) {
            return to.centerY();
        }

        float topInset = Math.min(12f, Math.max(5f, to.getHeight() * 0.22f));
        float top = to.getY() + topInset;
        float bottom = to.getY() + to.getHeight() - topInset;
        if (layout.groupIds().size() == 2) {
            float offset = Math.min(10f, Math.max(5f, to.getHeight() * 0.18f));
            return groupIndex == 0 ? to.centerY() - offset : to.centerY() + offset;
        }

        float step = (bottom - top) / Math.max(1, layout.groupIds().size() - 1);
        return top + step * groupIndex;
    }

    private float resolveIncomingLinkLaneY(ResearchLink link, ResearchNode from, ResearchNode to, IncomingGroupLayout layout) {
        int groupIndex = layout.groupIds().indexOf(from.getGroup().getId());
        if (groupIndex < 0) {
            return to.centerY();
        }

        float laneSpacing = Math.max(18f, getLinkWidth(link) * 6f);
        float centerOffset = groupIndex - (layout.groupIds().size() - 1) * 0.5f;
        return to.centerY() + centerOffset * laneSpacing;
    }

    private float resolveIncomingLinkStartLaneX(ResearchLink link,
                                                ResearchNode from,
                                                ResearchNode to,
                                                float startX,
                                                float endX,
                                                IncomingGroupLayout layout
    ) {
        float defaultMiddleX = startX + (endX - startX) * 0.5f;
        int groupIndex = layout.groupIds().indexOf(from.getGroup().getId());
        if (groupIndex < 0) {
            return defaultMiddleX;
        }

        float laneSpacing = Math.max(16f, getLinkWidth(link) * 6f);
        float minLaneX = startX + 18f;
        float maxLaneX = endX - 28f - laneSpacing * Math.max(0, layout.groupIds().size() - 1);
        if (maxLaneX <= minLaneX) {
            return defaultMiddleX;
        }
        return Math.max(minLaneX, Math.min(maxLaneX, minLaneX + groupIndex * laneSpacing));
    }

    private float resolveIncomingLinkEndLaneX(ResearchLink link,
                                              ResearchNode from,
                                              ResearchNode to,
                                              float startX,
                                              float endX,
                                              IncomingGroupLayout layout
    ) {
        float defaultMiddleX = startX + (endX - startX) * 0.5f;
        int groupIndex = layout.groupIds().indexOf(from.getGroup().getId());
        if (groupIndex < 0) {
            return defaultMiddleX;
        }

        float laneSpacing = Math.max(16f, getLinkWidth(link) * 6f);
        float maxLaneX = endX - 24f;
        float minLaneX = startX + 28f + laneSpacing * Math.max(0, layout.groupIds().size() - 1);
        if (maxLaneX <= minLaneX) {
            return defaultMiddleX;
        }
        return Math.max(minLaneX, Math.min(maxLaneX, maxLaneX - groupIndex * laneSpacing));
    }

    /**
     * Collects visible incoming parent groups of a target node and orders them for lane assignment.
     */
    private IncomingGroupLayout getIncomingGroupLayout(ResearchNode target) {
        Map<String, GroupIncomingStats> statsByGroupId = new LinkedHashMap<>();
        ResearchLink[] parentLinks = getLinksToNode(target.getId());
        for (ResearchLink parentLink : parentLinks) {
            ResearchNode parent = getNodeById(parentLink.getNodeFrom());
            if (parent == null || !isNodeVisible(parent)) {
                continue;
            }

            GroupIncomingStats stats = statsByGroupId.computeIfAbsent(
                    parent.getGroup().getId(),
                    ignored -> new GroupIncomingStats(parent.getGroup())
            );
            stats.centerYSum += parent.centerY();
            stats.count++;
        }

        ObjectArrayList<GroupIncomingStats> stats = new ObjectArrayList<>(statsByGroupId.values());
        stats.sort(Comparator
                .comparingDouble(GroupIncomingStats::averageCenterY)
                .thenComparing(groupStats -> groupStats.group().getTitle()));

        ObjectArrayList<String> groupIds = new ObjectArrayList<>(stats.size());
        for (int i = 0, size = stats.size(); i < size; i++) {
            groupIds.add(stats.get(i).group().getId());
        }
        return new IncomingGroupLayout(groupIds);
    }

    /**
     * Clusters same-group nodes inside each layout layer after the generic dependency layout pass.
     */
    private void compactAutoLayoutGroups() {
        if (nodes.isEmpty()) {
            return;
        }

        Map<String, Integer> groupOrder = collectStableGroupOrder();
        Map<Float, ObjectArrayList<ResearchNode>> nodesByLayerX = new LinkedHashMap<>();
        for (ResearchNode node : nodes) {
            nodesByLayerX.computeIfAbsent(node.getX(), ignored -> new ObjectArrayList<>()).add(node);
        }

        for (ObjectArrayList<ResearchNode> layerNodes : nodesByLayerX.values()) {
            if (layerNodes.size() <= 1) {
                continue;
            }

            layerNodes.sort(Comparator.comparingDouble(ResearchNode::getY));

            float originalMinY = Float.MAX_VALUE;
            float originalMaxBottom = -Float.MAX_VALUE;
            for (int i = 0, size = layerNodes.size(); i < size; i++) {
                ResearchNode node = layerNodes.get(i);
                originalMinY = Math.min(originalMinY, node.getY());
                originalMaxBottom = Math.max(originalMaxBottom, node.getY() + node.getHeight());
            }
            float originalCenterY = (originalMinY + originalMaxBottom) * 0.5f;

            Map<String, GroupVerticalCluster> clustersByGroupId = new LinkedHashMap<>();
            for (int i = 0, size = layerNodes.size(); i < size; i++) {
                ResearchNode node = layerNodes.get(i);
                GroupVerticalCluster cluster = clustersByGroupId.computeIfAbsent(
                        node.getGroup().getId(),
                        ignored -> new GroupVerticalCluster(node.getGroup())
                );
                cluster.nodes.add(node);
            }

            ObjectArrayList<GroupVerticalCluster> clusters = new ObjectArrayList<>(clustersByGroupId.values());
            for (int i = 0, size = clusters.size(); i < size; i++) {
                clusters.get(i).sortAndCaptureAverageY();
            }
            clusters.sort(Comparator
                    .comparingInt((GroupVerticalCluster cluster) -> groupOrder.getOrDefault(cluster.group().getId(), Integer.MAX_VALUE))
                    .thenComparingDouble(GroupVerticalCluster::averageY)
                    .thenComparing(cluster -> cluster.group().getTitle()));

            float currentY = originalMinY;
            for (int clusterIndex = 0, clusterCount = clusters.size(); clusterIndex < clusterCount; clusterIndex++) {
                GroupVerticalCluster cluster = clusters.get(clusterIndex);
                for (int nodeIndex = 0, nodeCount = cluster.nodes.size(); nodeIndex < nodeCount; nodeIndex++) {
                    ResearchNode node = cluster.nodes.get(nodeIndex);
                    node.setPosition(node.getX(), currentY);
                    currentY += node.getHeight();
                    if (nodeIndex < nodeCount - 1) {
                        currentY += autoLayoutConfig.verticalGap();
                    }
                }

                if (clusterIndex < clusterCount - 1) {
                    currentY += autoLayoutConfig.verticalGap();
                }
            }

            float newMinY = Float.MAX_VALUE;
            float newMaxBottom = -Float.MAX_VALUE;
            for (int i = 0, size = layerNodes.size(); i < size; i++) {
                ResearchNode node = layerNodes.get(i);
                newMinY = Math.min(newMinY, node.getY());
                newMaxBottom = Math.max(newMaxBottom, node.getY() + node.getHeight());
            }
            float offsetY = originalCenterY - (newMinY + newMaxBottom) * 0.5f;
            if (Math.abs(offsetY) > 0.001f) {
                for (int i = 0, size = layerNodes.size(); i < size; i++) {
                    ResearchNode node = layerNodes.get(i);
                    node.setPosition(node.getX(), node.getY() + offsetY);
                }
            }
        }
    }

    /**
     * Captures a stable group order from first appearance so early branches do not swap unpredictably.
     */
    private Map<String, Integer> collectStableGroupOrder() {
        Map<String, Integer> groupOrder = new LinkedHashMap<>();
        int nextIndex = 0;
        for (int i = 0, size = nodes.size(); i < size; i++) {
            ResearchNode node = nodes.get(i);
            String groupId = node.getGroup().getId();
            if (!groupOrder.containsKey(groupId)) {
                groupOrder.put(groupId, nextIndex++);
            }
        }
        return groupOrder;
    }

    private ObjectArrayList<GroupBounds> collectVisibleGroupBounds() {
        ObjectArrayList<GroupBounds> markers = new ObjectArrayList<>();
        Object2ObjectOpenHashMap<String, GroupMarkerBounds> boundsByGroupId = new Object2ObjectOpenHashMap<>();
        for (ResearchNode node : nodes) {
            if (!isNodeVisible(node)) {
                continue;
            }

            ResearchGroup group = node.getGroup();
            GroupMarkerBounds bounds = boundsByGroupId.get(group.getId());
            if (bounds == null) {
                bounds = new GroupMarkerBounds(group, node.getX(), node.getY(), node.getX() + node.getWidth(), node.getY() + node.getHeight());
                boundsByGroupId.put(group.getId(), bounds);
            } else {
                bounds.include(node.getX(), node.getY(), node.getX() + node.getWidth(), node.getY() + node.getHeight());
            }
        }

        for (GroupMarkerBounds bounds : boundsByGroupId.values()) {
            markers.add(bounds.toBounds());
        }
        return markers;
    }

    private @Nullable GroupBounds getVisibleGroupBounds(String groupId) {
        ObjectArrayList<GroupBounds> markers = collectVisibleGroupBounds();
        for (int i = 0, size = markers.size(); i < size; i++) {
            GroupBounds bounds = markers.get(i);
            if (bounds.group().getId().equals(groupId)) {
                return bounds;
            }
        }
        return null;
    }

    private boolean isNodeVisible(ResearchNode node) {
        if (node.isStudied()) {
            return true;
        }

        return switch (node.getVisibilityMode()) {
            case ALWAYS_VISIBLE -> true;
            case REQUIRE_ANY_PARENT_STUDIED, REQUIRE_ALL_PARENTS_STUDIED -> isNodeUnlockedForStudy(node);
        };
    }

    private boolean isNodeUnlockedForStudy(ResearchNode node) {
        if (node.isStudied()) {
            return true;
        }

        ResearchLink[] parentLinks = getLinksToNode(node.getId());
        if (parentLinks.length == 0) {
            return true;
        }

        return switch (node.getVisibilityMode()) {
            case ALWAYS_VISIBLE -> true;
            case REQUIRE_ANY_PARENT_STUDIED -> hasAnyStudiedParent(parentLinks);
            case REQUIRE_ALL_PARENTS_STUDIED -> areAllParentsStudied(parentLinks);
        };
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

    private boolean areAllParentsStudied(ResearchLink[] parentLinks) {
        for (ResearchLink parentLink : parentLinks) {
            ResearchNode parent = getNodeById(parentLink.getNodeFrom());
            if (parent == null || !parent.isStudied()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hook for subclasses to refresh their own widgets or auxiliary UI after progression changes.
     */
    protected void onResearchProgressionUpdated() {
    }

    protected record GroupMarkerLayout(ResearchGroup group,
                                       float minX,
                                       float minY,
                                       float maxX,
                                       float maxY) {
        public float centerX() {
            return (minX + maxX) * 0.5f;
        }

        public float centerY() {
            return (minY + maxY) * 0.5f;
        }
    }

    protected record GroupBounds(ResearchGroup group,
                                 float minX,
                                 float minY,
                                 float maxX,
                                 float maxY) {
        public float centerX() {
            return (minX + maxX) * 0.5f;
        }

        public float centerY() {
            return (minY + maxY) * 0.5f;
        }
    }

    private static final class GroupMarkerBounds {
        private final ResearchGroup group;
        private float minX;
        private float minY;
        private float maxX;
        private float maxY;

        private GroupMarkerBounds(ResearchGroup group, float minX, float minY, float maxX, float maxY) {
            this.group = group;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private void include(float minX, float minY, float maxX, float maxY) {
            this.minX = Math.min(this.minX, minX);
            this.minY = Math.min(this.minY, minY);
            this.maxX = Math.max(this.maxX, maxX);
            this.maxY = Math.max(this.maxY, maxY);
        }

        private GroupBounds toBounds() {
            return new GroupBounds(
                    group,
                    minX - DEFAULT_GROUP_BOUNDS_PADDING_X,
                    minY - DEFAULT_GROUP_BOUNDS_PADDING_Y,
                    maxX + DEFAULT_GROUP_BOUNDS_PADDING_X,
                    maxY + DEFAULT_GROUP_BOUNDS_PADDING_Y
            );
        }
    }

    private static final class GroupVerticalCluster {
        private final ResearchGroup group;
        private final ObjectArrayList<ResearchNode> nodes = new ObjectArrayList<>();
        private float averageY;

        private GroupVerticalCluster(ResearchGroup group) {
            this.group = group;
        }

        private void sortAndCaptureAverageY() {
            nodes.sort(Comparator.comparingDouble(ResearchNode::getY).thenComparingInt(ResearchNode::getId));

            float sum = 0f;
            for (int i = 0, size = nodes.size(); i < size; i++) {
                sum += nodes.get(i).centerY();
            }
            averageY = nodes.isEmpty() ? 0f : sum / nodes.size();
        }

        private ResearchGroup group() {
            return group;
        }

        private float averageY() {
            return averageY;
        }
    }

    private record IncomingGroupLayout(ObjectArrayList<String> groupIds) {
    }

    private static final class GroupIncomingStats {
        private final ResearchGroup group;
        private float centerYSum;
        private int count;

        private GroupIncomingStats(ResearchGroup group) {
            this.group = group;
        }

        private ResearchGroup group() {
            return group;
        }

        private float averageCenterY() {
            return count <= 0 ? 0f : centerYSum / count;
        }
    }

    private record LinkRoute(ObjectArrayList<LinkRouteSegment> segments) {
        private float totalLength() {
            float total = 0f;
            for (int i = 0, size = segments.size(); i < size; i++) {
                total += segments.get(i).length();
            }
            return Math.max(0.0001f, total);
        }
    }

    private record LinkRouteSegment(float x1,
                                    float y1,
                                    float x2,
                                    float y2,
                                    int startColor,
                                    int endColor,
                                    float width) {
        private float length() {
            float deltaX = x2 - x1;
            float deltaY = y2 - y1;
            return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        }
    }

    private record RevealAnimation(int nodeId,
                                   long startedAtMs,
                                   float cameraStartOffsetX,
                                   float cameraStartOffsetY,
                                   float cameraTargetOffsetX,
                                   float cameraTargetOffsetY,
                                   boolean nodeEnterSoundPlayed,
                                   boolean nodeDropSoundPlayed,
                                   boolean linkConnectSoundPlayed) {
        private RevealAnimation withNodeDropSoundPlayed() {
            return new RevealAnimation(
                    nodeId,
                    startedAtMs,
                    cameraStartOffsetX,
                    cameraStartOffsetY,
                    cameraTargetOffsetX,
                    cameraTargetOffsetY,
                    nodeEnterSoundPlayed,
                    true,
                    linkConnectSoundPlayed
            );
        }

        private RevealAnimation withLinkConnectSoundPlayed() {
            return new RevealAnimation(
                    nodeId,
                    startedAtMs,
                    cameraStartOffsetX,
                    cameraStartOffsetY,
                    cameraTargetOffsetX,
                    cameraTargetOffsetY,
                    nodeEnterSoundPlayed,
                    nodeDropSoundPlayed,
                    true
            );
        }
    }
}
