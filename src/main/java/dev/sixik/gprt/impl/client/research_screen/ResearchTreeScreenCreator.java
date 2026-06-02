package dev.sixik.gprt.impl.client.research_screen;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import dev.sixik.gprt.impl.client.research_screen.demo.ResearchTreeScreenDebug;

public class ResearchTreeScreenCreator {

    public static ModularUI createMainScreen() {
        return ModularUI.of(UI.of(new ResearchTreeScreenDebug().createView()));
    }
}
