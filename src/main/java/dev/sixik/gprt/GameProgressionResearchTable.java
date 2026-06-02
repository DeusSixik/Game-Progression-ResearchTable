package dev.sixik.gprt;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import dev.sixik.gprt.impl.client.research_screen.ResearchTreeScreenCreator;
import dev.sixik.gprt.registry.GPTRSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import org.slf4j.Logger;

@Mod(GameProgressionResearchTable.MODID)
public class GameProgressionResearchTable {
    public static final String MODID = "game_progression_research_table";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GameProgressionResearchTable(IEventBus modEventBus, ModContainer modContainer) {
        GPTRSounds.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ItemTossEvent.class, (itemTossEvent -> {

            final var minecraft = Minecraft.getInstance();
            if(minecraft == null) return;

            RenderSystem.recordRenderCall(() -> {
                minecraft.setScreen(new ModularUIScreen(ResearchTreeScreenCreator.createMainScreen(), Component.empty()));
            });
        }));
    }
}
