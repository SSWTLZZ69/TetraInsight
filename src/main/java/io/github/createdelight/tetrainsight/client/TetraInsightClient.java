package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.effect.EffectApplicabilityResourceIndex;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class TetraInsightClient {
    private TetraInsightClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(TetraInsightClientCommands::register);
        MinecraftForge.EVENT_BUS.addListener(MaterialTooltipHandler::onTooltip);
        MinecraftForge.EVENT_BUS.addListener(MaterialDossierShortcut::onKeyPressed);
        MinecraftForge.EVENT_BUS.addListener(PaginationRuntimeProbe::onClientTick);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(MaterialDossierShortcut::registerKeyMapping);
        modBus.addListener(PaginationRuntimeProbe::onClientSetup);
        modBus.addListener(EffectApplicabilityResourceIndex::register);
    }
}
