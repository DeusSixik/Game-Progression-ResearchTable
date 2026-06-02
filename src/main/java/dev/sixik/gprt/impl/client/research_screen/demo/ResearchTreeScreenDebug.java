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
        addNode(new ResearchNode(0, 0, 0, 120, 34));
        addNode(new ResearchNode(1, 200, 100, 120, 34));

        addLink(new ResearchLink(0, 1));
    }

    @Override
    protected @Nullable UIElement createNodeWidget(ResearchNode node) {
        var nodeButton = new Button();
        nodeButton.setText(String.valueOf(node.getId()));

        // По клику на ноду центрируем камеру на ее центре.
        nodeButton.setOnClick(event -> centerCameraOn(node.getId()));

        // Ключевой момент GraphView:
        // дочерние элементы contentRoot можно свободно расставлять через ABSOLUTE.
        //
        // x/y - это координаты в world-space canvas.
        // Они не обязаны совпадать с экранными координатами и будут потом
        // автоматически преобразованы через zoom + pan.
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
