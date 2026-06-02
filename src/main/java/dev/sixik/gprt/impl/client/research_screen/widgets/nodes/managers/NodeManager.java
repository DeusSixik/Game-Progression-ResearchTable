package dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.AdvancedGraphView;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.Node;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class NodeManager<NODE extends Node, NODE_LIST extends List<NODE>> {

    public NodeManager() { }

    public abstract NODE_LIST createList();

    public abstract void sortNodes(NODE_LIST nodes);

    public void onNodeAdded(NODE_LIST nodes, NODE node) {
    }

    public void onNodeRemoved(NODE_LIST nodes, NODE node) {
    }

    @Nullable
    public abstract NODE getNodeById(NODE_LIST nodes, int id);

    @Nullable
    public UIElement createWidget(NODE node) {
        return null;
    }
}
