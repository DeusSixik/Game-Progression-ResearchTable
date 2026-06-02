package dev.sixik.gprt.registry;

import dev.sixik.gprt.GameProgressionResearchTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GPTRSounds {

    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, GameProgressionResearchTable.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SUCK_IN =
            register("suck_in");

    public static final DeferredHolder<SoundEvent, SoundEvent> SPIT_OUT =
            register("spit_out");

    private GPTRSounds() {
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, resourceLocation -> SoundEvent.createVariableRangeEvent(resourceLocation));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GameProgressionResearchTable.MODID, path);
    }
}
