package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

public class ResearchTreeScreenDebug extends ResearchTreeScreen {

    public static UIElement createView() {
        return new Main();
    }

    private static class Main extends UIElement {
        public Main() {
            ResearchTreeScreen graph = new ResearchTreeScreenDebug();
            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));
            addChildren(graph);
        }
    }

    public ResearchTreeScreenDebug() {
        int node = createNode(0, 0);
        createNodeWithLink(node, 120+20, 0);
        createNodeWithLink(node, 0, 60);
        node = createNodeWithLink(node, 120+20, 60);

        createNodeWithLink(node, (120+20) * 2, 0);
        createNodeWithLink(node, (120+20) * 2, -60);
        createNodeWithLink(node, (120+20) * 2, 60);
    }

    private int currentIndex = 0;

    public int createNode(int x, int y) {
        addNode(new ResearchNode(currentIndex,x,y, 120, 34));
        return currentIndex++;
    }

    public int createNodeWithLink(int root, int x, int y) {
        int current = createNode(x, y);
        addLink(new ResearchLink(root, current));
        return current;
    }

    @Override
    protected @Nullable UIElement createNodeWidget(ResearchNode node) {
        Button nodeButton = new Button();
        nodeButton.setText(String.valueOf(node.getId()));
        nodeButton.setOnClick(event -> centerCameraOn(node.getId()));
        nodeButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(node.getX())
                .top(node.getY())
                .width(node.getWidth())
                .height(node.getHeight())
        );

        return nodeButton;
    }
}
