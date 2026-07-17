package io.github.createdelight.tetrainsight.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class TetraInsightClient {
    private TetraInsightClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(TetraInsightClientCommands::register);
        MinecraftForge.EVENT_BUS.addListener(PaginationRuntimeProbe::onClientTick);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PaginationRuntimeProbe::onClientSetup);
    }
}
