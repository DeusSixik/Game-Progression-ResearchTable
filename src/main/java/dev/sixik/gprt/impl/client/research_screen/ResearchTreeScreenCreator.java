package dev.sixik.gprt.impl.client.research_screen;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.sixik.gprt.impl.client.research_screen.demo.GraphViewDemo;
import dev.sixik.gprt.impl.client.research_screen.demo.ResearchTreeScreenDebug;
import dev.vfyjxf.taffy.style.TaffyPosition;

public class ResearchTreeScreenCreator {

    public static ModularUI createMainScreen() {
        return ModularUI.of(UI.of(ResearchTreeScreenDebug.createView()));
    }

    private static UIElement creatView() {
        return new UIElement().layout(layout -> layout.gapAll(2).widthPercent(100).heightPercent(100)).addChildren(
                new GraphView()
                        .addContentChild(new Button().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .left(0)
                                .top(0)
                        ).transform(transform2D -> transform2D.rotation(-45)))
                        .addContentChild(new Button().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .left(15)
                                .top(50)
                        ))
                        .addContentChild(new TextField().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .width(150)
                                .left(30)
                                .top(20)
                        ))
                        .addContentChild(new UIElement().layout(layout -> layout
                                .positionType(TaffyPosition.ABSOLUTE)
                                .width(100)
                                .height(100)
                                .left(100)
                                .top(40)
                        ).style(style -> style.background(new SpriteTexture())).transform(transform2D -> transform2D.rotation(45)))
                    .layout(layout -> layout.widthPercent(100).heightPercent(100)).style((basicStyle -> basicStyle.backgroundTexture(new ColorRectTexture(0xFF000000))))
        );
    }
}
