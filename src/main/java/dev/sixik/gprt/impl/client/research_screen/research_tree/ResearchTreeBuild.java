package dev.sixik.gprt.impl.client.research_screen.research_tree;

import dev.sixik.gprt.impl.client.research_screen.research_tree.definition.ResearchDefinition;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.progress.ResearchStudyType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative builder for assembling a {@link ResearchTreeScreen} without manually creating every
 * runtime node id and dependency link.
 * <p>
 * The builder stores groups and logical node definitions first, then materializes them into a screen
 * in one batched pass. This keeps research tree declarations compact and preserves insertion order for
 * predictable layout/group behavior later.
 * </p>
 *
 * <p><b>Quick navigation:</b></p>
 * <ul>
 *     <li>{@link #create()} - create a new builder;</li>
 *     <li>{@link #group(String, String, int)} / {@link #group(String, String, int, int)} - register groups;</li>
 *     <li>{@link #node(String)} - start configuring one logical node;</li>
 *     <li>{@link #applyTo(ResearchTreeScreen)} - materialize nodes and links into the target screen;</li>
 *     <li>{@link BuildResult} - map builder keys back to runtime ids and node instances;</li>
 *     <li>{@link NodeBuilder} - fluent API for one node definition.</li>
 * </ul>
 */
public final class ResearchTreeBuild {

    private final Map<String, ResearchGroup> groupsById = new LinkedHashMap<>();
    private final Map<String, NodeDefinition> nodeDefinitionsByKey = new LinkedHashMap<>();

    private ResearchTreeBuild() {
        groupsById.put(ResearchGroup.DEFAULT.getId(), ResearchGroup.DEFAULT);
    }

    /**
     * Creates a fresh builder with the default fallback group already registered.
     */
    public static ResearchTreeBuild create() {
        return new ResearchTreeBuild();
    }

    /**
     * Registers a group that uses the same color for both its primary and secondary theme slots.
     * <p>
     * This is the most convenient overload when a branch does not need a dedicated studied-state
     * accent yet and one base color is enough for the whole group.
     * </p>
     */
    public ResearchGroup group(String id, String title, int primaryColor) {
        return group(id, title, primaryColor, primaryColor);
    }

    /**
     * Registers or replaces a logical research group that can later be referenced from nodes.
     * <p>
     * Groups are stored by id and keep insertion order, which is useful when the screen builds
     * category lists or default theme presets from the same declaration order.
     * </p>
     */
    public ResearchGroup group(String id, String title, int primaryColor, int secondaryColor) {
        ResearchGroup group = ResearchGroup.of(id, title, primaryColor, secondaryColor);
        groupsById.put(group.getId(), group);
        return group;
    }

    /**
     * Starts configuring one logical node entry by string key.
     * <p>
     * Repeated calls with the same key reopen the same definition, so different parts of the
     * screen setup can contribute title, position, dependencies or study mode incrementally.
     * </p>
     */
    public NodeBuilder node(String key) {
        NodeDefinition definition = nodeDefinitionsByKey.computeIfAbsent(key, NodeDefinition::new);
        return new NodeBuilder(definition);
    }

    /**
     * Materializes the declarative tree into the target screen.
     * <p>
     * Nodes are created first, then links are resolved in a second pass. The whole operation runs
     * inside an auto-layout batch to avoid repeated rebuilds while the tree is being populated.
     * </p>
     */
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
                        .setDefinition(definition.toResearchDefinition())
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

    /**
     * Result of {@link #applyTo(ResearchTreeScreen)} that maps logical builder keys back to runtime
     * node ids and actual {@link ResearchNode} instances.
     * <p>
     * This object is useful whenever setup code needs to keep writing imperative logic after the
     * declarative build step. Typical examples:
     * </p>
     * <ul>
     *     <li>focus the camera on a specific node by its logical key;</li>
     *     <li>attach custom debug state to one generated node;</li>
     *     <li>bridge declarative tree construction with older code that still works in runtime ids.</li>
     * </ul>
     */
    public static final class BuildResult {
        private final Object2IntOpenHashMap<String> nodeIdsByKey;
        private final Int2ObjectOpenHashMap<ResearchNode> nodesById;

        private BuildResult(Object2IntOpenHashMap<String> nodeIdsByKey, Int2ObjectOpenHashMap<ResearchNode> nodesById) {
            this.nodeIdsByKey = nodeIdsByKey;
            this.nodesById = nodesById;
        }

        /**
         * Resolves the generated runtime node id by logical builder key.
         */
        public int nodeId(String key) {
            int nodeId = nodeIdsByKey.getInt(key);
            if (nodeId < 0) {
                throw new IllegalArgumentException("Unknown node key: " + key);
            }
            return nodeId;
        }

        /**
         * Resolves the generated runtime node instance by logical builder key.
         * <p>
         * Returns {@code null} when the key was not materialized, which makes this overload useful
         * for optional post-processing code that does not want to throw.
         * </p>
         */
        @Nullable
        public ResearchNode node(String key) {
            int nodeId = nodeIdsByKey.getInt(key);
            return nodeId < 0 ? null : nodesById.get(nodeId);
        }
    }

    private static final class NodeDefinition {
        private final String key;
        private String title;
        private String description;
        private String groupId;
        private boolean studied;
        private ResearchStudyType studyType = ResearchStudyType.INSTANT;
        private long studyDurationMs;
        private float x;
        private float y;
        private float width = 132f;
        private float height = 36f;
        private ResearchNode.VisibilityMode visibilityMode = ResearchNode.VisibilityMode.ALWAYS_VISIBLE;
        private final List<String> parents = new ArrayList<>();

        private NodeDefinition(String key) {
            this.key = key;
        }

        private ResearchDefinition toResearchDefinition() {
            return new ResearchDefinition(
                    key,
                    title,
                    description,
                    studyType,
                    studyDurationMs
            );
        }
    }

    /**
     * Fluent configuration API for one logical research node definition.
     * <p>
     * This builder is intentionally declarative: it stores what the node should become, but it
     * does not touch the screen immediately. The real {@link ResearchNode} is created only during
     * {@link #applyTo(ResearchTreeScreen)}.
     * </p>
     *
     * <p><b>Quick navigation:</b></p>
     * <ul>
     *     <li>{@link #title(String)} / {@link #description(String)} - basic metadata;</li>
     *     <li>{@link #definition(ResearchDefinition)} - copy metadata from a ready definition;</li>
     *     <li>{@link #studyType(ResearchStudyType)} / {@link #timedStudy(long)} - study mode setup;</li>
     *     <li>{@link #group(ResearchGroup)} / {@link #group(String)} - branch assignment;</li>
     *     <li>{@link #visibility(ResearchNode.VisibilityMode)} / {@link #studied(boolean)} - state defaults;</li>
     *     <li>{@link #size(float, float)} / {@link #position(float, float)} - fixed placement;</li>
     *     <li>{@link #dependsOn(String...)} - declare parent dependencies.</li>
     * </ul>
     */
    public final class NodeBuilder {
        private final NodeDefinition definition;

        private NodeBuilder(NodeDefinition definition) {
            this.definition = definition;
        }

        /**
         * Sets the display title used by the generated {@link ResearchDefinition}.
         */
        public NodeBuilder title(String title) {
            definition.title = title;
            return this;
        }

        /**
         * Sets the descriptive text used by the generated {@link ResearchDefinition}.
         */
        public NodeBuilder description(String description) {
            definition.description = description;
            return this;
        }

        /**
         * Copies metadata and study-mode settings from an existing immutable definition.
         * <p>
         * This is the bridge between registry-like research definitions and the screen-local
         * declarative tree builder.
         * </p>
         */
        public NodeBuilder definition(ResearchDefinition researchDefinition) {
            if (researchDefinition == null) {
                return this;
            }

            definition.title = researchDefinition.getTitle();
            definition.description = researchDefinition.getDescription();
            definition.studyType = researchDefinition.getStudyType();
            definition.studyDurationMs = researchDefinition.getStudyDurationMs();
            return this;
        }

        /**
         * Explicitly sets the study mode used by the generated node definition.
         */
        public NodeBuilder studyType(ResearchStudyType studyType) {
            if (studyType != null) {
                definition.studyType = studyType;
            }
            return this;
        }

        /**
         * Convenience helper for timed research.
         * <p>
         * This switches the node to {@link ResearchStudyType#TIMED} and clamps the duration to a
         * non-negative value.
         * </p>
         */
        public NodeBuilder timedStudy(long durationMs) {
            definition.studyType = ResearchStudyType.TIMED;
            definition.studyDurationMs = Math.max(0L, durationMs);
            return this;
        }

        /**
         * Assigns the node to a previously registered group.
         */
        public NodeBuilder group(ResearchGroup group) {
            definition.groupId = group == null ? null : group.getId();
            return this;
        }

        /**
         * Assigns the node to a group by raw id.
         * <p>
         * The id must be registered through {@link #group(String, String, int)} or
         * {@link #group(String, String, int, int)} before {@link #applyTo(ResearchTreeScreen)} runs.
         * </p>
         */
        public NodeBuilder group(String groupId) {
            definition.groupId = groupId;
            return this;
        }

        /**
         * Sets how the node should become visible relative to its dependencies.
         */
        public NodeBuilder visibility(ResearchNode.VisibilityMode visibilityMode) {
            if (visibilityMode != null) {
                definition.visibilityMode = visibilityMode;
            }
            return this;
        }

        /**
         * Marks the node as already studied in the initial tree snapshot.
         */
        public NodeBuilder studied(boolean studied) {
            definition.studied = studied;
            return this;
        }

        /**
         * Overrides the node widget bounds used by the graph.
         */
        public NodeBuilder size(float width, float height) {
            definition.width = width;
            definition.height = height;
            return this;
        }

        /**
         * Sets the absolute graph position used by the generated node.
         */
        public NodeBuilder position(float x, float y) {
            definition.x = x;
            definition.y = y;
            return this;
        }

        /**
         * Declares one or more parent node keys that must connect into this node.
         * <p>
         * Duplicate and blank keys are ignored. Parent keys are resolved only when the final build
         * is materialized, so declaration order between child and parent does not matter.
         * </p>
         */
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
