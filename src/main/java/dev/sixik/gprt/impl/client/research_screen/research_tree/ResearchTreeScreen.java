package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNodeManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.AdvancedGraphView;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeLinkManager;
import dev.sixik.gprt.impl.client.research_screen.widgets.nodes.managers.NodeManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ResearchTreeScreen extends AdvancedGraphView<
        ResearchNode,
        ObjectArrayList<ResearchNode>,
        ResearchLink,
        ObjectArrayList<ResearchLink>
> {

    public ResearchTreeScreen() {
        this(new ResearchNodeManager(), new ResearchNodeLinkManager());
    }

    public ResearchTreeScreen(NodeManager<ResearchNode, ObjectArrayList<ResearchNode>> nodeManager,
                              NodeLinkManager<ResearchLink, ObjectArrayList<ResearchLink>> linkManager
    ) {
        super(nodeManager, linkManager);
    }

    private static class Main extends UIElement {
        public Main() {
            ResearchTreeScreen graph = new ResearchTreeScreen();
            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));
            addChildren(graph);
        }
    }
}
