package dev.sixik.gprt.impl.client.research_screen.research_tree.nodes;

import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeLinkManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public class ResearchNodeLinkManager extends NodeLinkManager<ResearchLink, ObjectArrayList<ResearchLink>> {

    private static final ResearchLink[] EMPTY_LINKS = new ResearchLink[0];

    private final Map<ObjectArrayList<ResearchLink>, LinkIndex> indexes = new IdentityHashMap<>();

    @Override
    public ObjectArrayList<ResearchLink> createList() {
        var links = new ObjectArrayList<ResearchLink>();
        indexes.put(links, new LinkIndex());
        return links;
    }

    @Override
    public void sortLinks(ObjectArrayList<ResearchLink> researchLinks) {
        // Для быстрого доступа используем индексы, а не сортировку списка.
        // Оставляем метод no-op ради совместимости с текущим API.
    }

    @Override
    public void onLinkAdded(ObjectArrayList<ResearchLink> researchLinks, ResearchLink link) {
        getOrCreateIndex(researchLinks).add(link);
    }

    @Override
    public void onLinkRemoved(ObjectArrayList<ResearchLink> researchLinks, ResearchLink link) {
        getOrCreateIndex(researchLinks).remove(link);
    }

    @Override
    @Nullable
    public ResearchLink getLinkById(ObjectArrayList<ResearchLink> researchLinks, int id) {
        int size = researchLinks.size();
        if(id < 0 || id >= size) {
            return null;
        }

        return researchLinks.elements()[id];
    }

    @Override
    @Nullable
    public ResearchLink getLinkByNodes(ObjectArrayList<ResearchLink> researchLinks, int nodeFrom, int nodeTo) {
        return getOrCreateIndex(researchLinks).linksByPair.get(packPair(nodeFrom, nodeTo));
    }

    @Override
    @Nullable
    public ResearchLink getLinkByNode(ObjectArrayList<ResearchLink> researchLinks, int nodeId) {
        var index = getOrCreateIndex(researchLinks);
        var fromBucket = index.linksFromNode.get(nodeId);
        if (fromBucket != null && !fromBucket.links.isEmpty()) {
            return fromBucket.links.get(0);
        }

        var toBucket = index.linksToNode.get(nodeId);
        if (toBucket != null && !toBucket.links.isEmpty()) {
            return toBucket.links.get(0);
        }

        return null;
    }

    @Override
    public ResearchLink[] getLinksFromNode(ObjectArrayList<ResearchLink> researchLinks, int node) {
        var bucket = getOrCreateIndex(researchLinks).linksFromNode.get(node);
        return bucket == null ? EMPTY_LINKS : bucket.asArray();
    }

    @Override
    public ResearchLink[] getLinksToNode(ObjectArrayList<ResearchLink> researchLinks, int node) {
        var bucket = getOrCreateIndex(researchLinks).linksToNode.get(node);
        return bucket == null ? EMPTY_LINKS : bucket.asArray();
    }

    private LinkIndex getOrCreateIndex(ObjectArrayList<ResearchLink> researchLinks) {
        var index = indexes.get(researchLinks);
        if (index != null) {
            return index;
        }

        index = new LinkIndex();
        var elements = researchLinks.elements();
        for (int i = 0, size = researchLinks.size(); i < size; i++) {
            index.add(elements[i]);
        }
        indexes.put(researchLinks, index);
        return index;
    }

    private static long packPair(int nodeFrom, int nodeTo) {
        return ((long) nodeFrom << 32) | (nodeTo & 0xFFFFFFFFL);
    }

    private static final class LinkIndex {
        private final Long2ObjectOpenHashMap<ResearchLink> linksByPair = new Long2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<LinkBucket> linksFromNode = new Int2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<LinkBucket> linksToNode = new Int2ObjectOpenHashMap<>();

        private void add(ResearchLink link) {
            linksByPair.put(packPair(link.getNodeFrom(), link.getNodeTo()), link);
            linksFromNode.computeIfAbsent(link.getNodeFrom(), ignored -> new LinkBucket()).add(link);
            linksToNode.computeIfAbsent(link.getNodeTo(), ignored -> new LinkBucket()).add(link);
        }

        private void remove(ResearchLink link) {
            linksByPair.remove(packPair(link.getNodeFrom(), link.getNodeTo()));
            removeFromBucket(linksFromNode, link.getNodeFrom(), link);
            removeFromBucket(linksToNode, link.getNodeTo(), link);
        }

        private void removeFromBucket(Int2ObjectOpenHashMap<LinkBucket> buckets, int nodeId, ResearchLink link) {
            var bucket = buckets.get(nodeId);
            if (bucket == null) {
                return;
            }

            bucket.remove(link);
            if (bucket.isEmpty()) {
                buckets.remove(nodeId);
            }
        }
    }

    private static final class LinkBucket {
        private final ObjectArrayList<ResearchLink> links = new ObjectArrayList<>(4);
        private ResearchLink[] cachedArray;

        private void add(ResearchLink link) {
            links.add(link);
            cachedArray = null;
        }

        private void remove(ResearchLink link) {
            if (links.remove(link)) {
                cachedArray = null;
            }
        }

        private boolean isEmpty() {
            return links.isEmpty();
        }

        private ResearchLink[] asArray() {
            if (cachedArray == null) {
                cachedArray = links.toArray(EMPTY_LINKS);
            }
            return cachedArray;
        }
    }
}
