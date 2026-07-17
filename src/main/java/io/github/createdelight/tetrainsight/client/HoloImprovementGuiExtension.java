package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface HoloImprovementGuiExtension {
    void tetraInsight$refreshLayoutWidth();

    void tetraInsight$setImprovementChain(
            String improvementKey,
            List<ImprovementChainEntry> entries,
            ItemStack itemStack);
}
