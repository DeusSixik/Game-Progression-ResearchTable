package dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers;

import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.NodeLink;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class NodeLinkManager<LINK extends NodeLink, LINK_LIST extends List<LINK>> {

    public static final Object[] EMPTY_ARRAY = new Object[0];

    public abstract LINK_LIST createList();

    public abstract void sortLinks(LINK_LIST links);

    public void onLinkAdded(LINK_LIST links, LINK link) {
    }

    public void onLinkRemoved(LINK_LIST links, LINK link) {
    }

    @Nullable
    public abstract LINK getLinkById(LINK_LIST links, int id);

    @Nullable
    public abstract LINK getLinkByNodes(LINK_LIST links, int nodeFrom, int nodeTo);

    @Nullable
    public abstract LINK getLinkByNode(LINK_LIST links, int nodeId);

    public abstract LINK[] getLinksFromNode(LINK_LIST links, int node);

    public abstract LINK[] getLinksToNode(LINK_LIST links, int node);
}
