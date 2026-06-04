package dev.sixik.gprt.impl.client.research_screen;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.sixik.gprt.impl.client.research_screen.demo.ResearchTreeScreenDebug;
import dev.sixik.gprt.impl.client.research_screen.research_table.ResearchTableScreen;

public class ResearchTreeScreenCreator {

    private static final UIElement defaultScreen = new ResearchTableScreen().createView();

    public static ModularUI createMainScreen() {
        return ModularUI.of(UI.of(new ResearchTreeScreenDebug().createView()));
    }
}
