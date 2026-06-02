package dev.sixik.gprt.impl.client.research_screen.research_tree;

import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative builder for assembling a research tree without manually creating
 * every node id and link.
 */
public final class ResearchTreeBuild {

    private final Map<String, ResearchGroup> groupsById = new LinkedHashMap<>();
    private final Map<String, NodeDefinition> nodeDefinitionsByKey = new LinkedHashMap<>();

    private ResearchTreeBuild() {
        groupsById.put(ResearchGroup.DEFAULT.getId(), ResearchGroup.DEFAULT);
    }

    public static ResearchTreeBuild create() {
        return new ResearchTreeBuild();
    }

    public ResearchGroup group(String id, String title, int primaryColor) {
        return group(id, title, primaryColor, primaryColor);
    }

    public ResearchGroup group(String id, String title, int primaryColor, int secondaryColor) {
        ResearchGroup group = ResearchGroup.of(id, title, primaryColor, secondaryColor);
        groupsById.put(group.getId(), group);
        return group;
    }

    public NodeBuilder node(String key) {
        NodeDefinition definition = nodeDefinitionsByKey.computeIfAbsent(key, NodeDefinition::new);
        return new NodeBuilder(definition);
    }

    public BuildResult applyTo(ResearchTreeScreen screen) {
        int nextNodeId = findNextNodeId(screen);
        Object2IntOpenHashMap<String> nodeIdsByKey = new Object2IntOpenHashMap<>(nodeDefinitionsByKey.size());
        nodeIdsByKey.defaultReturnValue(-1);
        Int2ObjectOpenHashMap<ResearchNode> nodesById = new Int2ObjectOpenHashMap<>(nodeDefinitionsByKey.size());

        screen.beginAutoLayoutBatch();
        try {
            for (NodeDefinition definition : nodeDefinitionsByKey.values()) {
                int nodeId = nextNodeId++;
                nodeIdsByKey.put(definition.key, nodeId);

                ResearchNode node = new ResearchNode(nodeId, definition.x, definition.y, definition.width, definition.height)
                        .setTitle(definition.title != null ? definition.title : definition.key)
                        .setVisibilityMode(definition.visibilityMode)
                        .setStudied(definition.studied)
                        .setGroup(resolveGroup(definition.groupId));

                screen.addNode(node);
                nodesById.put(nodeId, node);
            }

            for (NodeDefinition definition : nodeDefinitionsByKey.values()) {
                int childId = nodeIdsByKey.getInt(definition.key);
                for (String parentKey : definition.parents) {
                    int parentId = nodeIdsByKey.getInt(parentKey);
                    if (parentId < 0) {
                        throw new IllegalStateException("Unknown parent node key: " + parentKey + " for node " + definition.key);
                    }
                    screen.addLink(new ResearchLink(parentId, childId));
                }
            }
        } finally {
            screen.endAutoLayoutBatch();
        }

        return new BuildResult(nodeIdsByKey, nodesById);
    }

    private ResearchGroup resolveGroup(@Nullable String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return ResearchGroup.DEFAULT;
        }

        ResearchGroup group = groupsById.get(groupId);
        if (group == null) {
            throw new IllegalStateException("Unknown research group id: " + groupId);
        }
        return group;
    }

    private static int findNextNodeId(ResearchTreeScreen screen) {
        int maxNodeId = -1;
        for (ResearchNode node : screen.getNodes()) {
            maxNodeId = Math.max(maxNodeId, node.getId());
        }
        return maxNodeId + 1;
    }

    public static final class BuildResult {
        private final Object2IntOpenHashMap<String> nodeIdsByKey;
        private final Int2ObjectOpenHashMap<ResearchNode> nodesById;

        private BuildResult(Object2IntOpenHashMap<String> nodeIdsByKey, Int2ObjectOpenHashMap<ResearchNode> nodesById) {
            this.nodeIdsByKey = nodeIdsByKey;
            this.nodesById = nodesById;
        }

        public int nodeId(String key) {
            int nodeId = nodeIdsByKey.getInt(key);
            if (nodeId < 0) {
                throw new IllegalArgumentException("Unknown node key: " + key);
            }
            return nodeId;
        }

        @Nullable
        public ResearchNode node(String key) {
            int nodeId = nodeIdsByKey.getInt(key);
            return nodeId < 0 ? null : nodesById.get(nodeId);
        }
    }

    private static final class NodeDefinition {
        private final String key;
        private String title;
        private String groupId;
        private boolean studied;
        private float x;
        private float y;
        private float width = 132f;
        private float height = 36f;
        private ResearchNode.VisibilityMode visibilityMode = ResearchNode.VisibilityMode.ALWAYS_VISIBLE;
        private final List<String> parents = new ArrayList<>();

        private NodeDefinition(String key) {
            this.key = key;
        }
    }

    public final class NodeBuilder {
        private final NodeDefinition definition;

        private NodeBuilder(NodeDefinition definition) {
            this.definition = definition;
        }

        public NodeBuilder title(String title) {
            definition.title = title;
            return this;
        }

        public NodeBuilder group(ResearchGroup group) {
            definition.groupId = group == null ? null : group.getId();
            return this;
        }

        public NodeBuilder group(String groupId) {
            definition.groupId = groupId;
            return this;
        }

        public NodeBuilder visibility(ResearchNode.VisibilityMode visibilityMode) {
            if (visibilityMode != null) {
                definition.visibilityMode = visibilityMode;
            }
            return this;
        }

        public NodeBuilder studied(boolean studied) {
            definition.studied = studied;
            return this;
        }

        public NodeBuilder size(float width, float height) {
            definition.width = width;
            definition.height = height;
            return this;
        }

        public NodeBuilder position(float x, float y) {
            definition.x = x;
            definition.y = y;
            return this;
        }

        public NodeBuilder dependsOn(String... parentKeys) {
            if (parentKeys == null) {
                return this;
            }

            for (String parentKey : parentKeys) {
                if (parentKey != null && !parentKey.isEmpty() && !definition.parents.contains(parentKey)) {
                    definition.parents.add(parentKey);
                }
            }
            return this;
        }
    }
}
