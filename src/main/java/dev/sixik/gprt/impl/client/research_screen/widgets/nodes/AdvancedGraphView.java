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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base graph widget used by the research UI and by debug/demo screens.
 * <p>
 * The class extends LDLib {@link GraphView}, but adds the pieces that are needed for a real
 * node editor / research tree instead of a simple visual prototype:
 * </p>
 * <ul>
 *     <li>typed storage for nodes and links through {@link NodeManager} and {@link NodeLinkManager};</li>
 *     <li>mapping between logical nodes and their on-screen {@link UIElement} widgets;</li>
 *     <li>camera helpers for centering and moving around the graph world;</li>
 *     <li>cached link geometry, so bend calculations are not repeated every frame;</li>
 *     <li>line batching by style, so many segments can be rendered with fewer state changes.</li>
 * </ul>
 *
 * <p><b>Quick navigation through the class:</b></p>
 * <ul>
 *     <li>{@link #constructorParams()} - default size, background and graph interaction config;</li>
 *     <li>{@link #focusCameraOnMouse(float, float)}, {@link #moveCameraToWorld(float, float)},
 *     {@link #centerCameraOn(float, float)}, {@link #syncCameraTransform()} - camera helpers;</li>
 *     <li>{@link #addNode(Node)}, {@link #addLink(NodeLink)}, {@link #removeNode(Node)},
 *     {@link #removeLink(NodeLink)} - graph data mutation entry points;</li>
 *     <li>{@link #syncNodeWidgetBounds(Node)}, {@link #syncAllNodeWidgetBounds()},
 *     {@link #setNodeWidgetAttached(int, boolean)} - widget lifecycle and positioning;</li>
 *     <li>{@link #buildLinkRenderData(NodeLink, Node, Node)} - one place where a logical link is
 *     converted into cached drawable segments;</li>
 *     <li>{@link #refreshLinkGeometry(NodeLink)}, {@link #invalidateNodeLinkGeometry(int)},
 *     {@link #invalidateLinkGeometry()} - cache invalidation and rebuild triggers;</li>
 *     <li>{@link #drawNodesLinks(GUIContext)}, {@link #drawCachedLineBatches(GUIContext)},
 *     {@link #drawLineBatch(GUIContext, LineBatch)} - actual cached link rendering path.</li>
 * </ul>
 *
 * <p><b>How to work with this class safely:</b></p>
 * <ul>
 *     <li>Subclass it when you need custom node widgets or custom link routing.</li>
 *     <li>Override {@link #createNodeWidget(Node)} to define how one logical node looks on screen.</li>
 *     <li>Override {@link #buildLinkRenderData(NodeLink, Node, Node)} when the default 3-segment
 *     routing is not enough and you need custom bends, gradients, priorities or batching behavior.</li>
 *     <li>After changing node positions, node size, visibility rules or link style inputs, call one
 *     of the invalidate methods so cached geometry stays in sync with the data model.</li>
 *     <li>The class is optimized around "rebuild on change, draw from cache every frame", so most
 *     heavy work should happen during edits, not inside the render loop.</li>
 * </ul>
 *
 * <p><b>Important mental model:</b></p>
 * <ul>
 *     <li>The graph has two layers: logical data ({@code nodes}/{@code links}) and visual widgets.</li>
 *     <li>Widgets are positioned in graph world coordinates, then the camera transform is applied.</li>
 *     <li>Links are not recalculated during draw; they are prebuilt into small polylines and grouped
 *     into style batches for rendering efficiency.</li>
 * </ul>
 */
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
    protected final IntOpenHashSet attachedNodeWidgetIds = new IntOpenHashSet();

    // Per-link geometry cache. Each link stores prebuilt polylines, so draw-time does not recalculate bends.
    protected final Object2ObjectOpenHashMap<LINK, LinkRenderData> linkRenderDataByLink = new Object2ObjectOpenHashMap<>();

    // Batches split cached polylines by render style, so one style uses one buffer acquisition + one batch method.
    private final Object2ObjectOpenHashMap<RenderBatchKey, LineBatch> lineBatchesByStyle = new Object2ObjectOpenHashMap<>();

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

    /**
     * Configures the default appearance and interaction rules of the graph view.
     * <p>
     * Subclasses can override this to change the background, zoom range or pan/zoom behavior
     * without touching the rest of the graph implementation.
     * </p>
     */
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

    /**
     * Centers the camera on the current mouse position in graph world space.
     */
    public void focusCameraOnMouse(float mouseX, float mouseY) {
        Vector2f mouseInGraphWorld = contentRoot.getLocalMouse(mouseX, mouseY);
        centerCameraOn(mouseInGraphWorld.x, mouseInGraphWorld.y);
    }

    /**
     * Moves the camera to an absolute world offset without recentering logic.
     */
    public void moveCameraToWorld(float worldX, float worldY) {
        setOffsetX(worldX);
        setOffsetY(worldY);
        syncCameraTransform();
    }

    /**
     * Centers the camera on the given node if that node exists.
     */
    public void centerCameraOn(int nodeId) {
        NODE node = nodeManager.getNodeById(nodes, nodeId);
        if (node != null) {
            centerCameraOn(node.centerX(), node.centerY());
        }
    }

    /**
     * Centers the visible area around the given world position.
     */
    public void centerCameraOn(float worldX, float worldY) {
        float halfVisibleWidth = getContentWidth() / (2f * getScale());
        float halfVisibleHeight = getContentHeight() / (2f * getScale());

        setOffsetX(worldX - halfVisibleWidth);
        setOffsetY(worldY - halfVisibleHeight);
        syncCameraTransform();
    }

    /**
     * Rebuilds the content transform from the current camera offsets and zoom.
     */
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

    /**
     * Draws cached link geometry in graph-world space using the current camera transform.
     */
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
        ObjectArrayList<LineBatch> orderedBatches = new ObjectArrayList<>(lineBatchesByStyle.values());
        orderedBatches.sort(Comparator.comparingInt(left -> left.renderPriority));

        for (LineBatch batch : orderedBatches) {
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

    /**
     * Adds a logical node, creates its widget and invalidates connected link geometry.
     */
    public void addNode(NODE node) {
        addToNodeList(node);

        UIElement nodeWidget = createNodeWidget(node);
        if (nodeWidget != null) {
            nodeWidgetsById.put(node.getId(), nodeWidget);
            addContentChild(nodeWidget);
            attachedNodeWidgetIds.add(node.getId());
        }

        invalidateNodeLinkGeometry(node.getId());
    }

    /**
     * Adds a logical link and immediately builds its cached render data.
     */
    public void addLink(LINK link) {
        addToLinkList(link);
        refreshLinkGeometry(link);
    }

    public boolean removeNodeById(int nodeId) {
        var node = getNodeById(nodeId);
        return node != null && removeNode(node);
    }

    /**
     * Removes a node, all links connected to it and its attached widget.
     */
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
        attachedNodeWidgetIds.remove(node.getId());

        return true;
    }

    public boolean removeLinkByNodes(int nodeFrom, int nodeTo) {
        var link = getLinkByNodes(nodeFrom, nodeTo);
        return link != null && removeLink(link);
    }

    /**
     * Removes a logical link and its cached render geometry.
     */
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

    /**
     * Synchronizes one widget's bounds from the logical node model.
     */
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

    /**
     * Synchronizes every currently known node widget with its logical node bounds.
     */
    public void syncAllNodeWidgetBounds() {
        for (NODE node : nodes) {
            syncNodeWidgetBounds(node);
        }
    }

    @Nullable
    protected UIElement getNodeWidget(int nodeId) {
        return nodeWidgetsById.get(nodeId);
    }

    protected boolean isNodeWidgetAttached(int nodeId) {
        return attachedNodeWidgetIds.contains(nodeId);
    }

    /**
     * Attaches or detaches an existing node widget without destroying the widget instance.
     */
    protected void setNodeWidgetAttached(int nodeId, boolean attached) {
        UIElement widget = nodeWidgetsById.get(nodeId);
        if (widget == null) {
            return;
        }

        boolean currentlyAttached = attachedNodeWidgetIds.contains(nodeId);
        if (attached == currentlyAttached) {
            return;
        }

        if (attached) {
            addContentChild(widget);
            attachedNodeWidgetIds.add(nodeId);
        } else {
            removeContentChild(widget);
            attachedNodeWidgetIds.remove(nodeId);
        }
    }

    @Nullable
    /**
     * Creates the visual widget for one logical node.
     * <p>
     * The default implementation delegates to the node manager, but subclasses may override it
     * when they want complete control over node visuals and click behavior.
     * </p>
     */
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
     * Higher priority links are drawn later and therefore visually stay on top.
     */
    protected int getLinkRenderPriority(LINK link, NODE from, NODE to) {
        return 0;
    }

    /**
     * Converts one logical link into cached drawable geometry.
     * <p>
     * The base implementation builds either one straight line or a simple 3-segment route.
     * Subclasses override this when they need custom routing, gradients, priorities or lane logic.
     * </p>
     */
    protected @Nullable LinkRenderData buildLinkRenderData(LINK link, NODE from, NODE to) {
        float startX = from.x + from.width;
        float startY = from.centerY();
        float endX = to.x;
        float endY = to.centerY();

        int startColor = getLinkStartColor(link);
        int endColor = getLinkEndColor(link);
        float width = getLinkWidth(link);
        float halfWidth = width * 0.5f;
        int renderPriority = getLinkRenderPriority(link, from, to);

        var renderData = createLinkRenderData(link, renderPriority);
        if (Math.abs(startY - endY) < 1.0f) {
            addLinkPolyline(renderData, startColor, endColor, width, startX, startY, endX, endY);
            return renderData;
        }

        float middleX = startX + (endX - startX) * 0.5f;

        addLinkPolyline(renderData, startColor, startColor, width, startX, startY, middleX + halfWidth, startY);
        addLinkPolyline(renderData, startColor, endColor, width, middleX, startY, middleX, endY);
        addLinkPolyline(renderData, endColor, endColor, width, middleX - halfWidth, endY, endX, endY);
        return renderData;
    }

    protected LinkRenderData createLinkRenderData(NodeLink owner, int renderPriority) {
        return new LinkRenderData(owner, renderPriority);
    }

    protected void addLinkPolyline(LinkRenderData renderData,
                                   int startColor,
                                   int endColor,
                                   float width,
                                   float x1,
                                   float y1,
                                   float x2,
                                   float y2
    ) {
        renderData.addPolyline(startColor, endColor, width, x1, y1, x2, y2);
    }

    /**
     * Rebuilds one link cache entry and updates its batch membership.
     */
    protected void refreshLinkGeometry(LINK link) {
        rebuildAllCachedLinkGeometryIfNeeded();
        removeCachedLinkGeometry(link);

        NODE from = getNodeById(link.nodeFrom);
        NODE to = getNodeById(link.nodeTo);
        if (from == null || to == null) {
            return;
        }

        LinkRenderData renderData = buildLinkRenderData(link, from, to);
        if (renderData == null) {
            return;
        }
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
            if (renderData == null) {
                continue;
            }
            linkRenderDataByLink.put(link, renderData);
            addCachedLinkToBatches(renderData);
        }

        rebuildAllLinkGeometryDirty = false;
    }

    private void addCachedLinkToBatches(LinkRenderData renderData) {
        for (int i = 0, size = renderData.polylines.size(); i < size; i++) {
            PolylineData polyline = renderData.polylines.get(i);
            LineBatch batch = lineBatchesByStyle.computeIfAbsent(polyline.batchKey, LineBatch::new);
            batch.polylines.add(polyline);
        }
    }

    private void removeCachedLinkFromBatches(LinkRenderData renderData) {
        for (int i = 0, size = renderData.polylines.size(); i < size; i++) {
            PolylineData polyline = renderData.polylines.get(i);
            LineBatch batch = lineBatchesByStyle.get(polyline.batchKey);
            if (batch == null) {
                continue;
            }

            batch.polylines.remove(polyline);
            if (batch.polylines.isEmpty()) {
                lineBatchesByStyle.remove(polyline.batchKey);
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

    protected static class LinkRenderData {
        @Getter
        private final NodeLink owner;
        private final int renderPriority;
        private final ObjectArrayList<PolylineData> polylines = new ObjectArrayList<>(3);

        protected LinkRenderData(NodeLink owner, int renderPriority) {
            this.owner = owner;
            this.renderPriority = renderPriority;
        }

        protected void addPolyline(int startColor, int endColor, float width, float x1, float y1, float x2, float y2) {
            polylines.add(new PolylineData(
                    new RenderBatchKey(new LineStyleKey(startColor, endColor, Float.floatToIntBits(width)), renderPriority),
                    x1,
                    y1,
                    x2,
                    y2
            ));
        }

    }

    protected static final class PolylineData {
        private final RenderBatchKey batchKey;
        private final ObjectArrayList<Vector2f> points = new ObjectArrayList<>(2);

        private PolylineData(RenderBatchKey batchKey, float x1, float y1, float x2, float y2) {
            this.batchKey = batchKey;
            this.points.add(new Vector2f(x1, y1));
            this.points.add(new Vector2f(x2, y2));
        }
    }

    protected record LineStyleKey(int startColor, int endColor, int widthBits) {
        public float width() {
            return Float.intBitsToFloat(widthBits);
        }
    }

    protected record RenderBatchKey(LineStyleKey style, int renderPriority) {
    }

    protected static final class LineBatch {
        private final LineStyleKey style;
        private final int renderPriority;
        private final ObjectArrayList<PolylineData> polylines = new ObjectArrayList<>();

        private LineBatch(RenderBatchKey batchKey) {
            this.style = batchKey.style();
            this.renderPriority = batchKey.renderPriority();
        }
    }
}
