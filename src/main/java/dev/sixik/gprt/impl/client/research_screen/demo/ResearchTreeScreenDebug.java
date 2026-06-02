package dev.sixik.gprt.impl.client.research_screen.demo;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.sixik.gprt.impl.client.research_screen.research_tree.ResearchTreeScreen;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchLink;
import dev.sixik.gprt.impl.client.research_screen.research_tree.nodes.ResearchNode;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Debug-screen with a live example of the optional auto-layout system.
 * <p>
 * Navigation through the class:
 * 1. {@link #createView()} - entry point that returns the root container for the screen.
 * 2. {@link Main} - outer container that holds the graph itself and an overlay panel with controls.
 * 3. {@link #ResearchTreeScreenDebug()} - creates the example graph, enables batching and turns on auto-layout.
 * 4. {@link #seedExampleTree()} - fills the graph with demo nodes/links to show the default dependency layout.
 * 5. {@link #createNode(float, float)} / {@link #createNodeWithLink(int, float, float)} - tiny helpers for graph construction.
 * 6. {@link #createNodeWidget(ResearchNode)} - shows how a graph node is converted into a visible UI widget.
 * <p>
 * What this demo shows in practice:
 * - auto-layout stays optional and can be turned on only for screens that need it;
 * - by default the dependency graph is laid out from left to right;
 * - begin/end batch prevents repeated rebuilds while many nodes and links are inserted;
 * - node widgets still use absolute world coordinates, but those coordinates are now written by the layout utility.
 */
public class ResearchTreeScreenDebug extends ResearchTreeScreen {

    private int currentIndex;

    public static UIElement createView() {
        return new Main();
    }

    private static class Main extends UIElement {
        public Main() {
            ResearchTreeScreenDebug graph = new ResearchTreeScreenDebug();

            layout(layout -> layout.widthPercent(100).heightPercent(100));
            style(style -> style.backgroundTexture(new ColorRectTexture(0xFF0E1116)));

            addChildren(graph, createOverlay(graph));
        }

        private static UIElement createOverlay(ResearchTreeScreenDebug graph) {
            UIElement panel = new UIElement()
                    .layout(layout -> layout
                            .positionType(TaffyPosition.ABSOLUTE)
                            .left(8)
                            .top(8)
                            .width(310)
                            .paddingAll(6)
                            .gapAll(4)
                    )
                    .style(style -> style.backgroundTexture(new ColorRectTexture(0xCC1A2330)));

            UIElement cameraButtons = new UIElement()
                    .layout(layout -> layout.widthPercent(100).gapAll(4));

            cameraButtons.addChildren(
                    new Button().setText("Fit").setOnClick(event -> graph.fitToChildren(80f, 0.35f)),
                    new Button().setText("Center Root").setOnClick(event -> graph.centerCameraOn(0)),
                    new Button().setText("Origin").setOnClick(event -> graph.moveCameraToWorld(0f, 0f))
            );

            panel.addChildren(
                    new Label().setText(Component.literal("ResearchTree auto-layout demo")),
                    new Label().setText(Component.literal("Default preset: dependency tree flows from left to right.")),
                    new Label().setText(Component.literal("LMB drag / wheel zoom / node click centers camera")),
                    cameraButtons
            );

            return panel;
        }
    }

    public ResearchTreeScreenDebug() {
        autoLayoutConfig()
                .origin(0f, 0f)
                .horizontalGap(130f)
                .verticalGap(42f);

        setAutoLayoutAutoFit(true);
        beginAutoLayoutBatch();
        try {
            seedExampleTree();
        } finally {
            endAutoLayoutBatch();
        }
        setAutoLayoutEnabled(true);
    }

    private void seedExampleTree() {
        int root = createNode(0, 0);

        int metallurgy = createNodeWithLink(root, 0, 0);
        int farming = createNodeWithLink(root, 0, 0);
        int logistics = createNodeWithLink(root, 0, 0);

        int alloying = createNodeWithLink(metallurgy, 0, 0);
        int steel = createNodeWithLink(metallurgy, 0, 0);
        int irrigation = createNodeWithLink(farming, 0, 0);
        int breeding = createNodeWithLink(farming, 0, 0);
        int carts = createNodeWithLink(logistics, 0, 0);
        int storage = createNodeWithLink(logistics, 0, 0);

        int steam = createNodeWithLink(alloying, 0, 0);
        int chemistry = createNodeWithLink(alloying, 0, 0);
        int machines = createNodeWithLink(steel, 0, 0);
        int greenhouses = createNodeWithLink(irrigation, 0, 0);
        int foodProcessing = createNodeWithLink(breeding, 0, 0);
        int rail = createNodeWithLink(carts, 0, 0);
        int warehouse = createNodeWithLink(storage, 0, 0);

        addLink(new ResearchLink(chemistry, machines));
        addLink(new ResearchLink(steam, machines));
        addLink(new ResearchLink(greenhouses, foodProcessing));
        addLink(new ResearchLink(machines, rail));
        addLink(new ResearchLink(machines, warehouse));
        addLink(new ResearchLink(foodProcessing, warehouse));
    }

    public int createNode(float x, float y) {
        addNode(new ResearchNode(currentIndex, x, y, 120, 34));
        return currentIndex++;
    }

    public int createNodeWithLink(int root, float x, float y) {
        int current = createNode(x, y);
        addLink(new ResearchLink(root, current));
        return current;
    }

    @Override
    protected @Nullable UIElement createNodeWidget(ResearchNode node) {
        Button nodeButton = new Button();
        nodeButton.setText("Node " + node.getId());
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
