package dev.sixik.gprt.impl.client.research_screen.widgets.nodes;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.EnhancedPoseStack;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public class AdvancedGraphView<
        NODE extends Node,
        NODE_LIST extends List<NODE>,
        LINK extends NodeLink,
        LINK_LIST extends List<LINK>
        > extends GraphView {

    @Getter
    protected final NODE_LIST nodes;
    @Getter
    protected final LINK_LIST links;
    @Getter
    protected final NodeManager<NODE, NODE_LIST> nodeManager;
    @Getter
    protected final NodeLinkManager<LINK, LINK_LIST> linkManager;

    protected final Int2ObjectOpenHashMap<UIElement> nodeWidgetsById = new Int2ObjectOpenHashMap<>();

    // Per-link geometry cache. Each link stores prebuilt polylines, so draw-time does not recalculate bends.
    protected final Object2ObjectOpenHashMap<LINK, LinkRenderData> linkRenderDataByLink = new Object2ObjectOpenHashMap<>();

    // Batches split cached polylines by render style, so one style uses one buffer acquisition + one batch method.
    private final Object2ObjectOpenHashMap<LineStyleKey, LineBatch> lineBatchesByStyle = new Object2ObjectOpenHashMap<>();

    // Fallback flag for cases when caller changes a lot of data and wants full cache rebuild.
    protected boolean rebuildAllLinkGeometryDirty = true;

    protected boolean fittedOnce;

    public AdvancedGraphView(NodeManager<NODE, NODE_LIST> nodeManager,
                             NodeLinkManager<LINK, LINK_LIST> linkManager
    ) {
        this.nodeManager = nodeManager;
        this.linkManager = linkManager;
        this.nodes = nodeManager.createList();
        this.links = linkManager.createList();

        constructorParams();

        addEventListener(UIEvents.LAYOUT_CHANGED, this::onLayoutChanged);
    }

    protected void constructorParams() {
        layout(layout -> layout.widthPercent(100).heightPercent(100));
        style(style -> style.backgroundTexture(new ColorRectTexture(0xFF11161C)));

        graphViewStyle(style -> style
                .allowPan(true)
                .allowZoom(true)
                .minScale(0.5f)
                .maxScale(2.5f)
                .gridSize(48f)
        );
    }

    public void focusCameraOnMouse(float mouseX, float mouseY) {
        Vector2f mouseInGraphWorld = contentRoot.getLocalMouse(mouseX, mouseY);
        centerCameraOn(mouseInGraphWorld.x, mouseInGraphWorld.y);
    }

    public void moveCameraToWorld(float worldX, float worldY) {
        setOffsetX(worldX);
        setOffsetY(worldY);
        syncCameraTransform();
    }

    public void centerCameraOn(int nodeId) {
        NODE node = nodeManager.getNodeById(nodes, nodeId);
        if (node != null) {
            centerCameraOn(node.centerX(), node.centerY());
        }
    }

    public void centerCameraOn(float worldX, float worldY) {
        float halfVisibleWidth = getContentWidth() / (2f * getScale());
        float halfVisibleHeight = getContentHeight() / (2f * getScale());

        setOffsetX(worldX - halfVisibleWidth);
        setOffsetY(worldY - halfVisibleHeight);
        syncCameraTransform();
    }

    public void syncCameraTransform() {
        contentRoot.transform(transform -> transform
                .translate(-(getOffsetX() * getScale()), -(getOffsetY() * getScale()))
                .scale(getScale())
        );
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        drawNodesLinks(guiContext);
    }

    protected void drawNodesLinks(GUIContext guiContext) {
        rebuildAllCachedLinkGeometryIfNeeded();

        EnhancedPoseStack pose = guiContext.pose;
        pose.pushPose();
        pose.translate(getContentX(), getContentY(), 0f);
        pose.scale(getScale(), getScale(), 1f);
        pose.translate(-getOffsetX(), -getOffsetY(), 0f);

        drawCachedLineBatches(guiContext);

        pose.popPose();
    }

    /**
     * Draw all pre-cached link polylines grouped by style.
     * <p>
     * We still render each independent polyline, but batching by style means: <br>
     * - one render type / buffer fetch per style group; <br>
     * - no geometry recomputation in the hot path; <br>
     * - fewer state changes than one DrawerHelper.drawLines(...) call per link segment.
     * </p>
     */
    protected void drawCachedLineBatches(GUIContext guiContext) {
        for (LineBatch batch : lineBatchesByStyle.values()) {
            drawLineBatch(guiContext, batch);
        }
    }

    protected void drawLineBatch(GUIContext guiContext, LineBatch batch) {
        if (batch.polylines.isEmpty()) {
            return;
        }

        // For a single polyline we can still use DrawerHelper directly.
        if (batch.polylines.size() == 1) {
            var polyline = batch.polylines.get(0);
            DrawerHelper.drawLines(
                    guiContext.graphics,
                    polyline.points,
                    batch.style.startColor,
                    batch.style.endColor,
                    batch.style.width()
            );
            return;
        }

        // For real batching we share one buffer across all polylines with the same style.
        var buffer = guiContext.graphics.bufferSource().getBuffer(LDLibRenderTypes.stripLines());
        RenderSystem.disableDepthTest();
        for (int i = 0, size = batch.polylines.size(); i < size; i++) {
            var polyline = batch.polylines.get(i);
            RenderBufferUtils.drawColorLines(
                    guiContext.graphics.pose(),
                    buffer,
                    polyline.points,
                    batch.style.startColor,
                    batch.style.endColor,
                    batch.style.width()
            );
        }
    }

    //////////////////////////////////////////
    ///        MANAGERS INVOKES            ///
    //////////////////////////////////////////

    public void addNode(NODE node) {
        addToNodeList(node);

        UIElement nodeWidget = createNodeWidget(node);
        if (nodeWidget != null) {
            nodeWidgetsById.put(node.getId(), nodeWidget);
            addContentChild(nodeWidget);
        }

        invalidateNodeLinkGeometry(node.getId());
    }

    public void addLink(LINK link) {
        addToLinkList(link);
        refreshLinkGeometry(link);
    }

    public boolean removeNodeById(int nodeId) {
        var node = getNodeById(nodeId);
        return node != null && removeNode(node);
    }

    public boolean removeNode(NODE node) {
        if (node == null) {
            return false;
        }

        removeLinksConnectedToNode(node.getId());

        boolean removed = nodes.remove(node);
        if (!removed) {
            return false;
        }

        nodeManager.onNodeRemoved(nodes, node);

        var widget = nodeWidgetsById.remove(node.getId());
        if (widget != null) {
            removeContentChild(widget);
        }

        return true;
    }

    public boolean removeLinkByNodes(int nodeFrom, int nodeTo) {
        var link = getLinkByNodes(nodeFrom, nodeTo);
        return link != null && removeLink(link);
    }

    public boolean removeLink(LINK link) {
        if (link == null) {
            return false;
        }

        boolean removed = links.remove(link);
        if (!removed) {
            return false;
        }

        linkManager.onLinkRemoved(links, link);
        removeCachedLinkGeometry(link);
        return true;
    }

    /**
     * Marks the whole cache dirty. Use this if you bulk-edit node geometry externally.
     */
    public void invalidateLinkGeometry() {
        rebuildAllLinkGeometryDirty = true;
    }

    /**
     * Refresh only one link cache entry and its batch membership.
     */
    public void invalidateLinkGeometry(LINK link) {
        refreshLinkGeometry(link);
    }

    /**
     * Refresh only links that touch the given node. <br>
     * Call this after moving/resizing one node instead of invalidating the whole graph.
     */
    public void invalidateNodeLinkGeometry(int nodeId) {
        rebuildAllCachedLinkGeometryIfNeeded();

        ObjectOpenHashSet<LINK> affected = new ObjectOpenHashSet<>();
        collectLinks(affected, getLinksFromNode(nodeId));
        collectLinks(affected, getLinksToNode(nodeId));
        for (LINK link : affected) {
            refreshLinkGeometry(link);
        }
    }

    protected void addToNodeList(NODE node) {
        nodes.add(node);
        nodeManager.onNodeAdded(nodes, node);
    }

    protected void addToLinkList(LINK link) {
        links.add(link);
        linkManager.onLinkAdded(links, link);
    }

    protected void syncNodeWidgetBounds(NODE node) {
        UIElement widget = nodeWidgetsById.get(node.getId());
        if (widget == null) {
            return;
        }

        widget.layout(layout -> layout
                .left(node.getX())
                .top(node.getY())
                .width(node.getWidth())
                .height(node.getHeight())
        );
    }

    public void syncAllNodeWidgetBounds() {
        for (NODE node : nodes) {
            syncNodeWidgetBounds(node);
        }
    }

    @Nullable
    protected UIElement createNodeWidget(NODE node) {
        return nodeManager.createWidget(node);
    }

    public void sortNodes() {
        nodeManager.sortNodes(nodes);
    }

    public void sortLinks() {
        linkManager.sortLinks(links);
    }

    public NODE getNodeById(int id) {
        return nodeManager.getNodeById(nodes, id);
    }

    public LINK getLinkById(int id) {
        return linkManager.getLinkById(links, id);
    }

    public LINK getLinkByNodes(int nodeFrom, int nodeTo) {
        return linkManager.getLinkByNodes(links, nodeFrom, nodeTo);
    }

    public LINK[] getLinksFromNode(int node) {
        return linkManager.getLinksFromNode(links, node);
    }

    public LINK[] getLinksToNode(int node) {
        return linkManager.getLinksToNode(links, node);
    }

    protected int getLinkStartColor(LINK link) {
        return 0xFF87D4FF;
    }

    protected int getLinkEndColor(LINK link) {
        return 0xFF87D4FF;
    }

    protected float getLinkWidth(LINK link) {
        return 2.0f;
    }

    /**
     * Builds the cached geometry for one link only.
     * Logic mirrors the improved demo renderer:
     * <p>
     * - one straight polyline if source and target are on the same Y;
     * <br>
     * - otherwise 3 segments with clean overlap compensation on corners.
     * </p>
     */
    protected LinkRenderData buildLinkRenderData(LINK link, NODE from, NODE to) {
        float startX = from.x + from.width;
        float startY = from.centerY();
        float endX = to.x;
        float endY = to.centerY();

        int startColor = getLinkStartColor(link);
        int endColor = getLinkEndColor(link);
        float width = getLinkWidth(link);
        float halfWidth = width * 0.5f;

        var renderData = new LinkRenderData(link);
        if (Math.abs(startY - endY) < 1.0f) {
            renderData.addPolyline(startColor, endColor, width, startX, startY, endX, endY);
            return renderData;
        }

        float middleX = startX + (endX - startX) * 0.5f;

        renderData.addPolyline(startColor, startColor, width, startX, startY, middleX + halfWidth, startY);
        renderData.addPolyline(startColor, endColor, width, middleX, startY, middleX, endY);
        renderData.addPolyline(endColor, endColor, width, middleX - halfWidth, endY, endX, endY);
        return renderData;
    }

    protected void refreshLinkGeometry(LINK link) {
        rebuildAllCachedLinkGeometryIfNeeded();
        removeCachedLinkGeometry(link);

        NODE from = getNodeById(link.nodeFrom);
        NODE to = getNodeById(link.nodeTo);
        if (from == null || to == null) {
            return;
        }

        LinkRenderData renderData = buildLinkRenderData(link, from, to);
        linkRenderDataByLink.put(link, renderData);
        addCachedLinkToBatches(renderData);
    }

    protected void removeCachedLinkGeometry(LINK link) {
        LinkRenderData renderData = linkRenderDataByLink.remove(link);
        if (renderData == null) {
            return;
        }
        removeCachedLinkFromBatches(renderData);
    }

    protected void rebuildLineBatchesFromCache() {
        lineBatchesByStyle.clear();
        for (LinkRenderData renderData : linkRenderDataByLink.values()) {
            addCachedLinkToBatches(renderData);
        }
    }

    private void rebuildAllCachedLinkGeometryIfNeeded() {
        if (!rebuildAllLinkGeometryDirty) {
            return;
        }

        linkRenderDataByLink.clear();
        lineBatchesByStyle.clear();

        for (LINK link : links) {
            NODE from = getNodeById(link.nodeFrom);
            NODE to = getNodeById(link.nodeTo);
            if (from == null || to == null) {
                continue;
            }

            LinkRenderData renderData = buildLinkRenderData(link, from, to);
            linkRenderDataByLink.put(link, renderData);
            addCachedLinkToBatches(renderData);
        }

        rebuildAllLinkGeometryDirty = false;
    }

    private void addCachedLinkToBatches(LinkRenderData renderData) {
        for (int i = 0, size = renderData.polylines.size(); i < size; i++) {
            PolylineData polyline = renderData.polylines.get(i);
            LineBatch batch = lineBatchesByStyle.computeIfAbsent(polyline.style, LineBatch::new);
            batch.polylines.add(polyline);
        }
    }

    private void removeCachedLinkFromBatches(LinkRenderData renderData) {
        for (int i = 0, size = renderData.polylines.size(); i < size; i++) {
            PolylineData polyline = renderData.polylines.get(i);
            LineBatch batch = lineBatchesByStyle.get(polyline.style);
            if (batch == null) {
                continue;
            }

            batch.polylines.remove(polyline);
            if (batch.polylines.isEmpty()) {
                lineBatchesByStyle.remove(polyline.style);
            }
        }
    }

    private void removeLinksConnectedToNode(int nodeId) {
        ObjectOpenHashSet<LINK> toRemove = new ObjectOpenHashSet<>();
        collectLinks(toRemove, getLinksFromNode(nodeId));
        collectLinks(toRemove, getLinksToNode(nodeId));

        for (LINK link : toRemove) {
            removeLink(link);
        }
    }

    private void collectLinks(ObjectOpenHashSet<LINK> out, LINK[] links) {
        Collections.addAll(out, links);
    }

    private void onLayoutChanged(UIEvent event) {
        if (!fittedOnce && getContentWidth() > 0 && getContentHeight() > 0) {
            fittedOnce = true;
            fitToChildren(80f, 0.35f);
        }
    }

    protected static final class LinkRenderData {
        @Getter
        private final NodeLink owner;
        private final ObjectArrayList<PolylineData> polylines = new ObjectArrayList<>(3);

        private LinkRenderData(NodeLink owner) {
            this.owner = owner;
        }

        private void addPolyline(int startColor, int endColor, float width, float x1, float y1, float x2, float y2) {
            polylines.add(new PolylineData(new LineStyleKey(startColor, endColor, Float.floatToIntBits(width)), x1, y1, x2, y2));
        }

    }

    protected static final class PolylineData {
        private final LineStyleKey style;
        private final ObjectArrayList<Vector2f> points = new ObjectArrayList<>(2);

        private PolylineData(LineStyleKey style, float x1, float y1, float x2, float y2) {
            this.style = style;
            this.points.add(new Vector2f(x1, y1));
            this.points.add(new Vector2f(x2, y2));
        }
    }

    protected record LineStyleKey(int startColor, int endColor, int widthBits) {
        public float width() {
            return Float.intBitsToFloat(widthBits);
        }
    }

    protected static final class LineBatch {
        private final LineStyleKey style;
        private final ObjectArrayList<PolylineData> polylines = new ObjectArrayList<>();

        private LineBatch(LineStyleKey style) {
            this.style = style;
        }
    }
}
