package dev.sixik.gprt.impl.client.research_screen.research_tree.layout;

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

/**
 * Optional utility that builds a readable dependency-tree layout from links.
 * <p>
 * The utility is designed for DAG/tree-like graphs, but it also keeps a safe
 * fallback for accidental cycles so the screen remains usable.
 */
public final class DependencyTreeAutoLayout {
    private DependencyTreeAutoLayout() {
    }

    public static final class Config {
        private float originX = 0f;
        private float originY = 0f;
        private float horizontalGap = 120f;
        private float verticalGap = 36f;

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
    }

    public static <NODE extends Node, LINK extends NodeLink> void apply(List<NODE> nodes, List<LINK> links, Config config) {
        if (nodes.isEmpty()) {
            return;
        }

        GraphData<NODE> graphData = analyzeGraph(nodes, links);
        applyLeftToRight(graphData, config);
    }

    private static <NODE extends Node, LINK extends NodeLink> GraphData<NODE> analyzeGraph(List<NODE> nodes, List<LINK> links) {
        Int2ObjectOpenHashMap<NODE> nodeById = new Int2ObjectOpenHashMap<>(nodes.size());
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
                        .thenComparingInt(Node::getId));
            }

            for (int index = 0, size = layerNodes.size(); index < size; index++) {
                rankByNodeId.put(layerNodes.get(index).getId(), index);
            }
        }

        float[] layerMaxWidth = new float[maxLayer + 1];
        float[] layerMaxHeight = new float[maxLayer + 1];
        for (int layer = 0; layer <= maxLayer; layer++) {
            List<NODE> layerNodes = nodesByLayer.get(layer);
            if (layerNodes == null || layerNodes.isEmpty()) {
                continue;
            }

            float maxWidth = 0f;
            float maxHeight = 0f;
            for (NODE node : layerNodes) {
                maxWidth = Math.max(maxWidth, node.getWidth());
                maxHeight = Math.max(maxHeight, node.getHeight());
            }
            layerMaxWidth[layer] = maxWidth;
            layerMaxHeight[layer] = maxHeight;
        }

        return new GraphData<>(nodesByLayer, parentsByNodeId, rankByNodeId, maxLayer, layerMaxWidth, layerMaxHeight);
    }

    private static <NODE extends Node> void applyLeftToRight(GraphData<NODE> graphData, Config config) {
        float currentX = config.originX();
        for (int layer = 0; layer <= graphData.maxLayer; layer++) {
            List<NODE> layerNodes = graphData.nodesByLayer.get(layer);
            if (layerNodes == null || layerNodes.isEmpty()) {
                currentX += graphData.layerMaxWidth[layer] + config.horizontalGap();
                continue;
            }

            float totalHeight = 0f;
            for (NODE node : layerNodes) {
                totalHeight += node.getHeight();
            }
            totalHeight += Math.max(0, layerNodes.size() - 1) * config.verticalGap();

            float currentY = config.originY() - totalHeight * 0.5f;
            for (NODE node : layerNodes) {
                node.setPosition(currentX, currentY);
                currentY += node.getHeight() + config.verticalGap();
            }

            currentX += graphData.layerMaxWidth[layer] + config.horizontalGap();
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

    private static final class GraphData<NODE extends Node> {
        private final Int2ObjectOpenHashMap<List<NODE>> nodesByLayer;
        @SuppressWarnings("unused")
        private final Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId;
        @SuppressWarnings("unused")
        private final Int2FloatOpenHashMap rankByNodeId;
        private final int maxLayer;
        private final float[] layerMaxWidth;
        private final float[] layerMaxHeight;

        private GraphData(Int2ObjectOpenHashMap<List<NODE>> nodesByLayer,
                          Int2ObjectOpenHashMap<IntArrayList> parentsByNodeId,
                          Int2FloatOpenHashMap rankByNodeId,
                          int maxLayer,
                          float[] layerMaxWidth,
                          float[] layerMaxHeight
        ) {
            this.nodesByLayer = nodesByLayer;
            this.parentsByNodeId = parentsByNodeId;
            this.rankByNodeId = rankByNodeId;
            this.maxLayer = maxLayer;
            this.layerMaxWidth = layerMaxWidth;
            this.layerMaxHeight = layerMaxHeight;
        }
    }
}
