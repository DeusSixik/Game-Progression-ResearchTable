package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.EnhancedPoseStack;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import dev.sixik.gprt.impl.client.research_screen.research_tree.layout.DependencyTreeAutoLayout;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNodeManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.AdvancedGraphView;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.joml.Vector2f;

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
    private int autoLayoutSuspendDepth;

    public ResearchTreeScreen() {
        this(new ResearchNodeManager(), new ResearchNodeLinkManager());
    }

    public ResearchTreeScreen(NodeManager<ResearchNode, ObjectArrayList<ResearchNode>> nodeManager,
                              NodeLinkManager<ResearchLink, ObjectArrayList<ResearchLink>> linkManager
    ) {
        super(nodeManager, linkManager);
    }

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

    public ResearchTreeScreen setAutoLayoutAutoFit(boolean autoLayoutAutoFit) {
        this.autoLayoutAutoFit = autoLayoutAutoFit;
        return this;
    }

    public DependencyTreeAutoLayout.Config autoLayoutConfig() {
        return autoLayoutConfig;
    }

    public void beginAutoLayoutBatch() {
        autoLayoutSuspendDepth++;
    }

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

    public void applyAutoLayout() {
        ObjectArrayList<ResearchNode> visibleNodes = collectVisibleNodesForLayout();
        ObjectArrayList<ResearchLink> visibleLinks = collectVisibleLinksForLayout();

        DependencyTreeAutoLayout.apply(visibleNodes, visibleLinks, autoLayoutConfig);
        syncAllNodeWidgetBounds();
        syncResearchNodeVisibility();
        invalidateLinkGeometry();
        onResearchProgressionUpdated();

        if (autoLayoutAutoFit && getContentWidth() > 0 && getContentHeight() > 0) {
            fitToChildren(80f, 0.35f);
        }
    }

    public ResearchTreeScreen refreshResearchProgression() {
        syncAllNodeWidgetBounds();
        syncResearchNodeVisibility();
        invalidateLinkGeometry();
        onResearchProgressionUpdated();

        if (autoLayoutAutoFit && getContentWidth() > 0 && getContentHeight() > 0) {
            fitToChildren(80f, 0.35f);
        }
        return this;
    }

    public ResearchTreeScreen setNodeStudied(int nodeId, boolean studied) {
        ResearchNode node = getNodeById(nodeId);
        if (node == null || node.isStudied() == studied) {
            return this;
        }

        node.setStudied(studied);
        requestProgressionRefresh();
        return this;
    }

    public boolean isNodeStudied(int nodeId) {
        ResearchNode node = getNodeById(nodeId);
        return node != null && node.isStudied();
    }

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

    public boolean centerCameraOnGroup(String groupId) {
        GroupBounds bounds = getVisibleGroupBounds(groupId);
        if (bounds == null) {
            return false;
        }

        centerCameraOn(bounds.centerX(), bounds.centerY());
        return true;
    }

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

    public ResearchTreeScreen setHighlightedGroup(@Nullable String groupId) {
        highlightedGroupId = groupId;
        return this;
    }

    public @Nullable String getHighlightedGroupId() {
        return highlightedGroupId;
    }

    public ResearchTreeScreen clearHighlightedGroup() {
        highlightedGroupId = null;
        return this;
    }

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

    @Override
    protected @Nullable LinkRenderData buildLinkRenderData(ResearchLink link, ResearchNode from, ResearchNode to) {
        if (!isNodeVisible(from) || !isNodeVisible(to)) {
            return null;
        }
        return super.buildLinkRenderData(link, from, to);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        drawHighlightedGroupBounds(guiContext);
    }

    private void requestAutoLayout() {
        if (autoLayoutEnabled && !isAutoLayoutSuspended()) {
            applyAutoLayout();
        }
    }

    private void requestProgressionRefresh() {
        if (isAutoLayoutSuspended()) {
            return;
        }

        if (autoLayoutEnabled) {
            applyAutoLayout();
        } else {
            refreshResearchProgression();
        }
    }

    private void syncResearchNodeVisibility() {
        for (ResearchNode node : nodes) {
            setNodeWidgetAttached(node.getId(), isNodeVisible(node));
        }
    }

    private void drawHighlightedGroupBounds(GUIContext guiContext) {
        if (highlightedGroupId == null) {
            return;
        }

        GroupBounds bounds = getVisibleGroupBounds(highlightedGroupId);
        if (bounds == null) {
            return;
        }

        float pulse = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.012d));
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

    private ObjectArrayList<ResearchNode> collectVisibleNodesForLayout() {
        ObjectArrayList<ResearchNode> visibleNodes = new ObjectArrayList<>();
        for (ResearchNode node : nodes) {
            if (isNodeVisible(node)) {
                visibleNodes.add(node);
            }
        }
        return visibleNodes;
    }

    private ObjectArrayList<ResearchLink> collectVisibleLinksForLayout() {
        ObjectArrayList<ResearchLink> visibleLinks = new ObjectArrayList<>();
        for (ResearchLink link : links) {
            ResearchNode from = getNodeById(link.getNodeFrom());
            ResearchNode to = getNodeById(link.getNodeTo());
            if (from != null && to != null && isNodeVisible(from) && isNodeVisible(to)) {
                visibleLinks.add(link);
            }
        }
        return visibleLinks;
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
}
