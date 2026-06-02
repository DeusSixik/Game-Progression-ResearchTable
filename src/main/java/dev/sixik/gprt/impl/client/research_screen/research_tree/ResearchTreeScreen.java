package dev.sixik.gprt.impl.client.research_screen.research_tree;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.research_tree.layout.DependencyTreeAutoLayout;
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

    private final DependencyTreeAutoLayout.Config autoLayoutConfig = new DependencyTreeAutoLayout.Config();
    private boolean autoLayoutEnabled;
    private boolean autoLayoutAutoFit = true;
    private int autoLayoutSuspendDepth;

    public ResearchTreeScreen() {
        this(new ResearchNodeManager(), new ResearchNodeLinkManager());
    }

    public ResearchTreeScreen(NodeManager<ResearchNode, ObjectArrayList<ResearchNode>> nodeManager,
                              NodeLinkManager<ResearchLink, ObjectArrayList<ResearchLink>> linkManager
    ) {
        super(nodeManager, linkManager);
    }

    public ResearchTreeScreen setAutoLayoutEnabled(boolean autoLayoutEnabled) {
        this.autoLayoutEnabled = autoLayoutEnabled;
        if (autoLayoutEnabled && !isAutoLayoutSuspended()) {
            applyAutoLayout();
        }
        return this;
    }

    public boolean isAutoLayoutEnabled() {
        return autoLayoutEnabled;
    }

    public ResearchTreeScreen setAutoLayoutAutoFit(boolean autoLayoutAutoFit) {
        this.autoLayoutAutoFit = autoLayoutAutoFit;
        return this;
    }

    public DependencyTreeAutoLayout.Config autoLayoutConfig() {
        return autoLayoutConfig;
    }

    public void beginAutoLayoutBatch() {
        autoLayoutSuspendDepth++;
    }

    public void endAutoLayoutBatch() {
        if (autoLayoutSuspendDepth > 0) {
            autoLayoutSuspendDepth--;
        }

        if (autoLayoutSuspendDepth == 0 && autoLayoutEnabled) {
            applyAutoLayout();
        }
    }

    public boolean isAutoLayoutSuspended() {
        return autoLayoutSuspendDepth > 0;
    }

    public void applyAutoLayout() {
        DependencyTreeAutoLayout.apply(nodes, links, autoLayoutConfig);
        syncAllNodeWidgetBounds();
        invalidateLinkGeometry();

        if (autoLayoutAutoFit && getContentWidth() > 0 && getContentHeight() > 0) {
            fitToChildren(80f, 0.35f);
        }
    }

    @Override
    public void addNode(ResearchNode node) {
        super.addNode(node);
        requestAutoLayout();
    }

    @Override
    public void addLink(ResearchLink link) {
        super.addLink(link);
        requestAutoLayout();
    }

    @Override
    public boolean removeNode(ResearchNode node) {
        boolean removed = super.removeNode(node);
        if (removed) {
            requestAutoLayout();
        }
        return removed;
    }

    @Override
    public boolean removeLink(ResearchLink link) {
        boolean removed = super.removeLink(link);
        if (removed) {
            requestAutoLayout();
        }
        return removed;
    }

    private void requestAutoLayout() {
        if (autoLayoutEnabled && !isAutoLayoutSuspended()) {
            applyAutoLayout();
        }
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
