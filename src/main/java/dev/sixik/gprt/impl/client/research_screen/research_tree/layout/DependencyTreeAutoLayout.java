package dev.sixik.gprt.impl.client.research_screen.research_tree.layout;

import dev.sixik.gprt.impl.client.research_screen.research_tree.node_widgets.ResearchNodeWrapper;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.Node;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.NodeLink;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Optional utility that builds a readable dependency-tree layout from links.
 * <p>
 * The utility is designed for DAG/tree-like graphs, but it also keeps a safe
 * fallback for accidental cycles so the screen remains usable.
 * </p>
 *
 * <p><b>Quick navigation:</b></p>
 * <ul>
 *     <li>{@link Config} - origin and spacing settings;</li>
 *     <li>{@link #apply(List, List, Config)} - public entry point;</li>
 *     <li>{@link #apply(List, List, Config, Function)} - wrapper-aware spacing variant;</li>
 *     <li>{@link #analyzeGraph(List, List, Config, Function)} - derive layers, parent/child maps and ranks;</li>
 *     <li>{@link #initializeLayerOrder(Int2ObjectOpenHashMap, Int2ObjectOpenHashMap, Int2ObjectOpenHashMap, Int2FloatOpenHashMap, int)}
 *     - first ordering pass inside layers;</li>
 *     <li>{@link #refineLayerOrder(Int2ObjectOpenHashMap, Int2ObjectOpenHashMap, Int2ObjectOpenHashMap, Int2FloatOpenHashMap, int)}
 *     - iterative ordering refinement;</li>
 *     <li>{@link #applyLeftToRight(GraphData, Config)} - write initial positions;</li>
 *     <li>{@link #relaxNodeVerticalPlacement(GraphData, Config)} - smooth vertical placement by connectivity.</li>
 * </ul>
 *
 * <p><b>Algorithm summary:</b></p>
 * <ul>
 *     <li>Assign nodes to horizontal layers from dependency depth.</li>
 *     <li>Sort nodes inside each layer using the average ranks of connected parents and children.</li>
 *     <li>Place layers from left to right with configurable gaps.</li>
 *     <li>Run a few relaxation passes so nodes line up better with their connected neighbors.</li>
 * </ul>
 */
public final class DependencyTreeAutoLayout {
    private DependencyTreeAutoLayout() {
    }

    /**
     * Mutable layout settings reused across layout passes.
     */
    public static final class Config {
        private float originX = 0f;
        private float originY = 0f;
        private float horizontalGap = 120f;
        private float verticalGap = 36f;
        private boolean useNodeWrappersForSpacing;

        public float originX() {
            return originX;
        }

        public float originY() {
            return originY;
        }

        public float horizontalGap() {
            return horizontalGap;
        }

        public float verticalGap() {
            return verticalGap;
        }

        public boolean useNodeWrappersForSpacing() {
            return useNodeWrappersForSpacing;
        }

        public Config origin(float x, float y) {
            this.originX = x;
            this.originY = y;
            return this;
        }

        public Config horizontalGap(float horizontalGap) {
            this.horizontalGap = horizontalGap;
            return this;
        }

        public Config verticalGap(float verticalGap) {
            this.verticalGap = verticalGap;
            return this;
        }

        /**
         * Controls whether spacing calculations should use effective wrapper size instead of the
         * raw logical node size.
         */
        public Config useNodeWrappersForSpacing(boolean useNodeWrappersForSpacing) {
            this.useNodeWrappersForSpacing = useNodeWrappersForSpacing;
            return this;
        }
    }

    /**
     * Analyzes the dependency graph and writes left-to-right positions back into the node list.
     */
    public static <NODE extends Node, LINK extends NodeLink> void apply(List<NODE> nodes, List<LINK> links, Config config) {
        apply(nodes, links, config, ignored -> null);
    }

    /**
     * Variant that can optionally use runtime wrapper size when spacing layers and rows.
     */
    public static <NODE extends Node, LINK extends NodeLink> void apply(List<NODE> nodes,
                                                                        List<LINK> links,
                                                                        Config config,
                                                                        Function<NODE, ResearchNodeWrapper> wrapperProvider
    ) {
        if (nodes.isEmpty()) {
            return;
        }

        GraphData<NODE> graphData = analyzeGraph(nodes, links, config, wrapperProvider);
        applyLeftToRight(graphData, config);
    }

    private static <NODE extends Node, LINK extends NodeLink> GraphData<NODE> analyzeGraph(List<NODE> nodes,
                                                                                           List<LINK> links,
                                                                                           Config config,
                                                                                           Function<NODE, ResearchNodeWrapper> wrapperProvider
    ) {
        Int2ObjectOpenHashMap<NODE> nodeById = new Int2ObjectOpenHashMap<>(nodes.size());
        Int2ObjectOpenHashMap<NodeLayoutMetrics> metricsByNodeId = new Int2ObjectOpenHashMap<>(nodes.size());
        Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId = new Int2ObjectOpenHashMap<>();
        Int2ObjectOpenHashMap<IntArrayList> childrenByNodeId = new Int2ObjectOpenHashMap<>();
        Int2IntOpenHashMap indegree = new Int2IntOpenHashMap();
        Int2IntOpenHashMap remainingIndegree = new Int2IntOpenHashMap();
        Int2IntOpenHashMap layerByNodeId = new Int2IntOpenHashMap();
        Int2FloatOpenHashMap rankByNodeId = new Int2FloatOpenHashMap();

        indegree.defaultReturnValue(0);
        remainingIndegree.defaultReturnValue(0);
        layerByNodeId.defaultReturnValue(0);
        rankByNodeId.defaultReturnValue(-1f);

        for (NODE node : nodes) {
            nodeById.put(node.getId(), node);
            ResearchNodeWrapper wrapper = resolveWrapper(node, config, wrapperProvider);
            metricsByNodeId.put(node.getId(), NodeLayoutMetrics.capture(node, wrapper));
            indegree.put(node.getId(), 0);
            remainingIndegree.put(node.getId(), 0);
        }

        for (LINK link : links) {
            if (!nodeById.containsKey(link.getNodeFrom()) || !nodeById.containsKey(link.getNodeTo())) {
                continue;
            }

            childrenByNodeId.computeIfAbsent(link.getNodeFrom(), ignored -> new IntArrayList()).add(link.getNodeTo());
            parentsByNodeId.computeIfAbsent(link.getNodeTo(), ignored -> new IntArrayList()).add(link.getNodeFrom());
            indegree.addTo(link.getNodeTo(), 1);
            remainingIndegree.addTo(link.getNodeTo(), 1);
        }

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (NODE node : nodes) {
            if (indegree.get(node.getId()) == 0) {
                queue.add(node.getId());
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            int currentId = queue.removeFirst();
            processed++;

            IntArrayList children = childrenByNodeId.get(currentId);
            if (children == null) {
                continue;
            }

            int currentLayer = layerByNodeId.get(currentId);
            for (int i = 0, size = children.size(); i < size; i++) {
                int childId = children.getInt(i);
                layerByNodeId.put(childId, Math.max(layerByNodeId.get(childId), currentLayer + 1));

                int newIndegree = remainingIndegree.addTo(childId, -1) - 1;
                if (newIndegree == 0) {
                    queue.addLast(childId);
                }
            }
        }

        if (processed < nodeById.size()) {
            for (NODE node : nodes) {
                int nodeId = node.getId();
                if (remainingIndegree.get(nodeId) <= 0) {
                    continue;
                }

                IntArrayList parents = parentsByNodeId.get(nodeId);
                int fallbackLayer = 0;
                if (parents != null) {
                    for (int i = 0, size = parents.size(); i < size; i++) {
                        fallbackLayer = Math.max(fallbackLayer, layerByNodeId.get(parents.getInt(i)) + 1);
                    }
                }
                layerByNodeId.put(nodeId, fallbackLayer);
            }
        }

        int maxLayer = 0;
        Int2ObjectOpenHashMap<List<NODE>> nodesByLayer = new Int2ObjectOpenHashMap<>();
        for (NODE node : nodes) {
            int layer = layerByNodeId.get(node.getId());
            nodesByLayer.computeIfAbsent(layer, ignored -> new ArrayList<>()).add(node);
            maxLayer = Math.max(maxLayer, layer);
        }

        initializeLayerOrder(nodesByLayer, parentsByNodeId, childrenByNodeId, rankByNodeId, maxLayer);
        refineLayerOrder(nodesByLayer, parentsByNodeId, childrenByNodeId, rankByNodeId, maxLayer);

        float[] layerMaxWidth = new float[maxLayer + 1];
        float[] layerMaxHeight = new float[maxLayer + 1];
        float[] layerMinLeftOffset = new float[maxLayer + 1];
        float[] layerMaxRightOffset = new float[maxLayer + 1];
        for (int layer = 0; layer <= maxLayer; layer++) {
            List<NODE> layerNodes = nodesByLayer.get(layer);
            if (layerNodes == null || layerNodes.isEmpty()) {
                continue;
            }

            float maxWidth = 0f;
            float maxHeight = 0f;
            float minLeftOffset = Float.MAX_VALUE;
            float maxRightOffset = -Float.MAX_VALUE;
            for (NODE node : layerNodes) {
                NodeLayoutMetrics metrics = metricsByNodeId.get(node.getId());
                maxWidth = Math.max(maxWidth, metrics.width());
                maxHeight = Math.max(maxHeight, metrics.height());
                minLeftOffset = Math.min(minLeftOffset, metrics.leftOffset());
                maxRightOffset = Math.max(maxRightOffset, metrics.rightOffset());
            }
            layerMaxWidth[layer] = Math.max(maxWidth, maxRightOffset - minLeftOffset);
            layerMaxHeight[layer] = maxHeight;
            layerMinLeftOffset[layer] = minLeftOffset;
            layerMaxRightOffset[layer] = maxRightOffset;
        }

        return new GraphData<>(
                nodeById,
                metricsByNodeId,
                nodesByLayer,
                parentsByNodeId,
                childrenByNodeId,
                rankByNodeId,
                maxLayer,
                layerMaxWidth,
                layerMaxHeight,
                layerMinLeftOffset,
                layerMaxRightOffset
        );
    }

    private static <NODE extends Node> void applyLeftToRight(GraphData<NODE> graphData, Config config) {
        float currentLayerLeft = config.originX();
        for (int layer = 0; layer <= graphData.maxLayer; layer++) {
            List<NODE> layerNodes = graphData.nodesByLayer.get(layer);
            if (layerNodes == null || layerNodes.isEmpty()) {
                currentLayerLeft += graphData.layerMaxWidth[layer] + config.horizontalGap();
                continue;
            }

            float totalHeight = 0f;
            for (NODE node : layerNodes) {
                totalHeight += graphData.metricsByNodeId.get(node.getId()).height();
            }
            totalHeight += Math.max(0, layerNodes.size() - 1) * config.verticalGap();

            float layerLogicalX = currentLayerLeft - graphData.layerMinLeftOffset[layer];
            float currentWrapperTop = config.originY() - totalHeight * 0.5f;
            for (NODE node : layerNodes) {
                NodeLayoutMetrics metrics = graphData.metricsByNodeId.get(node.getId());
                node.setPosition(layerLogicalX, currentWrapperTop - metrics.topOffset());
                currentWrapperTop += metrics.height() + config.verticalGap();
            }

            currentLayerLeft += graphData.layerMaxWidth[layer] + config.horizontalGap();
        }

        relaxNodeVerticalPlacement(graphData, config);
    }

    private static <NODE extends Node> void initializeLayerOrder(Int2ObjectOpenHashMap<List<NODE>> nodesByLayer,
                                                                 Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId,
                                                                 Int2ObjectOpenHashMap<IntArrayList> childrenByNodeId,
                                                                 Int2FloatOpenHashMap rankByNodeId,
                                                                 int maxLayer
    ) {
        for (int layer = 0; layer <= maxLayer; layer++) {
            List<NODE> layerNodes = nodesByLayer.get(layer);
            if (layerNodes == null || layerNodes.isEmpty()) {
                continue;
            }

            if (layer == 0) {
                layerNodes.sort(Comparator.comparingInt(Node::getId));
            } else {
                layerNodes.sort(Comparator
                        .comparingDouble((NODE node) -> averageParentRank(node.getId(), parentsByNodeId, rankByNodeId))
                        .thenComparingDouble(node -> averageChildRank(node.getId(), childrenByNodeId, rankByNodeId))
                        .thenComparingInt(Node::getId));
            }
        }

        rebuildRanks(nodesByLayer, rankByNodeId, maxLayer);
    }

    private static <NODE extends Node> void refineLayerOrder(Int2ObjectOpenHashMap<List<NODE>> nodesByLayer,
                                                             Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId,
                                                             Int2ObjectOpenHashMap<IntArrayList> childrenByNodeId,
                                                             Int2FloatOpenHashMap rankByNodeId,
                                                             int maxLayer
    ) {
        for (int iteration = 0; iteration < 6; iteration++) {
            for (int layer = 1; layer <= maxLayer; layer++) {
                List<NODE> layerNodes = nodesByLayer.get(layer);
                if (layerNodes == null || layerNodes.size() <= 1) {
                    continue;
                }

                layerNodes.sort(Comparator
                        .comparingDouble((NODE node) -> averageParentRank(node.getId(), parentsByNodeId, rankByNodeId))
                        .thenComparingDouble(node -> averageChildRank(node.getId(), childrenByNodeId, rankByNodeId))
                        .thenComparingInt(Node::getId));
            }
            rebuildRanks(nodesByLayer, rankByNodeId, maxLayer);

            for (int layer = maxLayer - 1; layer >= 0; layer--) {
                List<NODE> layerNodes = nodesByLayer.get(layer);
                if (layerNodes == null || layerNodes.size() <= 1) {
                    continue;
                }

                layerNodes.sort(Comparator
                        .comparingDouble((NODE node) -> averageChildRank(node.getId(), childrenByNodeId, rankByNodeId))
                        .thenComparingDouble(node -> averageParentRank(node.getId(), parentsByNodeId, rankByNodeId))
                        .thenComparingInt(Node::getId));
            }
            rebuildRanks(nodesByLayer, rankByNodeId, maxLayer);
        }
    }

    private static <NODE extends Node> void rebuildRanks(Int2ObjectOpenHashMap<List<NODE>> nodesByLayer,
                                                         Int2FloatOpenHashMap rankByNodeId,
                                                         int maxLayer
    ) {
        for (int layer = 0; layer <= maxLayer; layer++) {
            List<NODE> layerNodes = nodesByLayer.get(layer);
            if (layerNodes == null) {
                continue;
            }

            for (int index = 0, size = layerNodes.size(); index < size; index++) {
                rankByNodeId.put(layerNodes.get(index).getId(), index);
            }
        }
    }

    private static <NODE extends Node> void relaxNodeVerticalPlacement(GraphData<NODE> graphData, Config config) {
        for (int iteration = 0; iteration < 4; iteration++) {
            for (int layer = 1; layer <= graphData.maxLayer; layer++) {
                relaxLayerTowardsParents(graphData, config, layer);
            }
            for (int layer = graphData.maxLayer - 1; layer >= 0; layer--) {
                relaxLayerTowardsChildren(graphData, config, layer);
            }
        }
    }

    private static <NODE extends Node> void relaxLayerTowardsParents(GraphData<NODE> graphData, Config config, int layer) {
        List<NODE> layerNodes = graphData.nodesByLayer.get(layer);
        if (layerNodes == null || layerNodes.isEmpty()) {
            return;
        }

        float[] desiredTop = new float[layerNodes.size()];
        for (int i = 0, size = layerNodes.size(); i < size; i++) {
            NODE node = layerNodes.get(i);
            desiredTop[i] = desiredTopFromConnectedCenters(
                    node,
                    graphData.parentsByNodeId.get(node.getId()),
                    graphData.nodeById,
                    graphData.metricsByNodeId,
                    node.getY()
            );
        }

        applyLayerVerticalTargets(graphData, layerNodes, desiredTop, config);
    }

    private static <NODE extends Node> void relaxLayerTowardsChildren(GraphData<NODE> graphData, Config config, int layer) {
        List<NODE> layerNodes = graphData.nodesByLayer.get(layer);
        if (layerNodes == null || layerNodes.isEmpty()) {
            return;
        }

        float[] desiredTop = new float[layerNodes.size()];
        for (int i = 0, size = layerNodes.size(); i < size; i++) {
            NODE node = layerNodes.get(i);
            desiredTop[i] = desiredTopFromConnectedCenters(
                    node,
                    graphData.childrenByNodeId.get(node.getId()),
                    graphData.nodeById,
                    graphData.metricsByNodeId,
                    node.getY()
            );
        }

        applyLayerVerticalTargets(graphData, layerNodes, desiredTop, config);
    }

    private static <NODE extends Node> float desiredTopFromConnectedCenters(NODE node,
                                                                            IntArrayList connectedNodeIds,
                                                                            Int2ObjectOpenHashMap<NODE> nodeById,
                                                                            Int2ObjectOpenHashMap<NodeLayoutMetrics> metricsByNodeId,
                                                                            float fallbackTop
    ) {
        if (connectedNodeIds == null || connectedNodeIds.isEmpty()) {
            return fallbackTop;
        }

        float centerSum = 0f;
        int counted = 0;
        for (int i = 0, size = connectedNodeIds.size(); i < size; i++) {
            NODE connected = nodeById.get(connectedNodeIds.getInt(i));
            if (connected == null) {
                continue;
            }
            NodeLayoutMetrics connectedMetrics = metricsByNodeId.get(connected.getId());
            centerSum += connected.getY() + connectedMetrics.centerOffsetY();
            counted++;
        }

        if (counted == 0) {
            return fallbackTop;
        }

        float desiredCenter = centerSum / counted;
        return desiredCenter - metricsByNodeId.get(node.getId()).centerOffsetY();
    }

    private static <NODE extends Node> void applyLayerVerticalTargets(GraphData<NODE> graphData,
                                                                      List<NODE> layerNodes,
                                                                      float[] desiredTop,
                                                                      Config config
    ) {
        float currentTop = desiredTop[0];
        layerNodes.get(0).setPosition(layerNodes.get(0).getX(), currentTop);
        NodeLayoutMetrics firstMetrics = graphData.metricsByNodeId.get(layerNodes.get(0).getId());
        float currentBottom = currentTop + firstMetrics.bottomOffset();

        for (int i = 1, size = layerNodes.size(); i < size; i++) {
            NODE node = layerNodes.get(i);
            NodeLayoutMetrics metrics = graphData.metricsByNodeId.get(node.getId());
            float minLogicalTop = currentBottom + config.verticalGap() - metrics.topOffset();
            float logicalTop = Math.max(desiredTop[i], minLogicalTop);
            node.setPosition(node.getX(), logicalTop);
            currentBottom = logicalTop + metrics.bottomOffset();
        }

        float minTop = Float.MAX_VALUE;
        float maxBottom = -Float.MAX_VALUE;
        for (NODE node : layerNodes) {
            NodeLayoutMetrics metrics = graphData.metricsByNodeId.get(node.getId());
            minTop = Math.min(minTop, node.getY() + metrics.topOffset());
            maxBottom = Math.max(maxBottom, node.getY() + metrics.bottomOffset());
        }

        float currentCenter = (minTop + maxBottom) * 0.5f;
        float targetCenter = config.originY();
        float offset = targetCenter - currentCenter;
        if (Math.abs(offset) < 0.001f) {
            return;
        }

        for (NODE node : layerNodes) {
            node.setPosition(node.getX(), node.getY() + offset);
        }
    }

    private static double averageParentRank(int nodeId,
                                            Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId,
                                            Int2FloatOpenHashMap rankByNodeId
    ) {
        IntArrayList parents = parentsByNodeId.get(nodeId);
        if (parents == null || parents.isEmpty()) {
            return Double.MAX_VALUE;
        }

        float sum = 0f;
        int counted = 0;
        for (int i = 0, size = parents.size(); i < size; i++) {
            float rank = rankByNodeId.get(parents.getInt(i));
            if (rank >= 0f) {
                sum += rank;
                counted++;
            }
        }
        return counted == 0 ? Double.MAX_VALUE : sum / counted;
    }

    private static double averageChildRank(int nodeId,
                                           Int2ObjectOpenHashMap<IntArrayList> childrenByNodeId,
                                           Int2FloatOpenHashMap rankByNodeId
    ) {
        IntArrayList children = childrenByNodeId.get(nodeId);
        if (children == null || children.isEmpty()) {
            return Double.MAX_VALUE;
        }

        float sum = 0f;
        int counted = 0;
        for (int i = 0, size = children.size(); i < size; i++) {
            float rank = rankByNodeId.get(children.getInt(i));
            if (rank >= 0f) {
                sum += rank;
                counted++;
            }
        }
        return counted == 0 ? Double.MAX_VALUE : sum / counted;
    }

    private static final class GraphData<NODE extends Node> {
        private final Int2ObjectOpenHashMap<NODE> nodeById;
        private final Int2ObjectOpenHashMap<NodeLayoutMetrics> metricsByNodeId;
        private final Int2ObjectOpenHashMap<List<NODE>> nodesByLayer;
        private final Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId;
        private final Int2ObjectOpenHashMap<IntArrayList> childrenByNodeId;
        private final Int2FloatOpenHashMap rankByNodeId;
        private final int maxLayer;
        private final float[] layerMaxWidth;
        private final float[] layerMaxHeight;
        private final float[] layerMinLeftOffset;
        private final float[] layerMaxRightOffset;

        private GraphData(Int2ObjectOpenHashMap<NODE> nodeById,
                          Int2ObjectOpenHashMap<NodeLayoutMetrics> metricsByNodeId,
                          Int2ObjectOpenHashMap<List<NODE>> nodesByLayer,
                          Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId,
                          Int2ObjectOpenHashMap<IntArrayList> childrenByNodeId,
                          Int2FloatOpenHashMap rankByNodeId,
                          int maxLayer,
                          float[] layerMaxWidth,
                          float[] layerMaxHeight,
                          float[] layerMinLeftOffset,
                          float[] layerMaxRightOffset
        ) {
            this.nodeById = nodeById;
            this.metricsByNodeId = metricsByNodeId;
            this.nodesByLayer = nodesByLayer;
            this.parentsByNodeId = parentsByNodeId;
            this.childrenByNodeId = childrenByNodeId;
            this.rankByNodeId = rankByNodeId;
            this.maxLayer = maxLayer;
            this.layerMaxWidth = layerMaxWidth;
            this.layerMaxHeight = layerMaxHeight;
            this.layerMinLeftOffset = layerMinLeftOffset;
            this.layerMaxRightOffset = layerMaxRightOffset;
        }
    }

    private static <NODE extends Node> ResearchNodeWrapper resolveWrapper(NODE node,
                                                                          Config config,
                                                                          Function<NODE, ResearchNodeWrapper> wrapperProvider
    ) {
        if (config.useNodeWrappersForSpacing() && wrapperProvider != null) {
            ResearchNodeWrapper wrapper = wrapperProvider.apply(node);
            if (wrapper != null) {
                return wrapper;
            }
        }
        return ResearchNodeWrapper.ofBounds(node.getX(), node.getY(), node.getWidth(), node.getHeight());
    }

    private static <NODE extends Node> float effectiveHeight(GraphData<NODE> graphData, NODE node) {
        NodeLayoutMetrics metrics = graphData.metricsByNodeId.get(node.getId());
        return metrics != null ? metrics.height() : node.getHeight();
    }

    private record NodeLayoutMetrics(
            float leftOffset,
            float topOffset,
            float rightOffset,
            float bottomOffset,
            float width,
            float height,
            float centerOffsetX,
            float centerOffsetY
    ) {
        private static NodeLayoutMetrics capture(Node node, ResearchNodeWrapper wrapper) {
            float leftOffset = wrapper.getX() - node.getX();
            float topOffset = wrapper.getY() - node.getY();
            float rightOffset = leftOffset + wrapper.getWidth();
            float bottomOffset = topOffset + wrapper.getHeight();
            float centerOffsetX = leftOffset + wrapper.getWidth() * 0.5f;
            float centerOffsetY = topOffset + wrapper.getHeight() * 0.5f;
            return new NodeLayoutMetrics(
                    leftOffset,
                    topOffset,
                    rightOffset,
                    bottomOffset,
                    wrapper.getWidth(),
                    wrapper.getHeight(),
                    centerOffsetX,
                    centerOffsetY
            );
        }
    }
}
