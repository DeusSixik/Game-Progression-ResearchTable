package dev.sixik.gprt.impl.client.research_screen.widgets.nodes;

public class NodeLink {

    protected final int nodeFrom;
    protected final int nodeTo;

    public NodeLink(int nodeFrom, int nodeTo) {
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
    }

    public int getNodeFrom() {
        return nodeFrom;
    }

    public int getNodeTo() {
        return nodeTo;
    }
}
