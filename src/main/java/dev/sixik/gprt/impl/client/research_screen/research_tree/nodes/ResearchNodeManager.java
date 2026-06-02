package dev.sixik.gprt.impl.client.research_screen.research_tree.nodes;

import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class ResearchNodeManager extends NodeManager<ResearchNode, ObjectArrayList<ResearchNode>> {

    private final Map<ObjectArrayList<ResearchNode>, Int2ObjectOpenHashMap<ResearchNode>> nodesById = new IdentityHashMap<>();

    @Override
    public ObjectArrayList<ResearchNode> createList() {
        var nodes = new ObjectArrayList<ResearchNode>();
        nodesById.put(nodes, new Int2ObjectOpenHashMap<>());
        return nodes;
    }

    @Override
    public void sortNodes(ObjectArrayList<ResearchNode> researchNodes) {
        // Lookup is backed by id -> node indexing, so sorting is unnecessary here.
        // Keep this as a no-op to preserve the current manager contract.
    }

    @Override
    public void onNodeAdded(ObjectArrayList<ResearchNode> researchNodes, ResearchNode node) {
        getOrCreateNodeIndex(researchNodes).put(node.getId(), node);
    }

    @Override
    public void onNodeRemoved(ObjectArrayList<ResearchNode> researchNodes, ResearchNode node) {
        getOrCreateNodeIndex(researchNodes).remove(node.getId());
    }

    @Override
    @Nullable
    public ResearchNode getNodeById(ObjectArrayList<ResearchNode> researchNodes, int id) {
        if (id < 0) {
            return null;
        }
        return getOrCreateNodeIndex(researchNodes).get(id);
    }

    private Int2ObjectOpenHashMap<ResearchNode> getOrCreateNodeIndex(ObjectArrayList<ResearchNode> researchNodes) {
        var index = nodesById.get(researchNodes);
        if (index != null) {
            return index;
        }

        index = new Int2ObjectOpenHashMap<>(researchNodes.size());
        var elements = researchNodes.elements();
        for (int i = 0, size = researchNodes.size(); i < size; i++) {
            var node = elements[i];
            index.put(node.getId(), node);
        }
        nodesById.put(researchNodes, index);
        return index;
    }
}
