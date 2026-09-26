package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;

/**
 * Implemented by the improvement overview so navigation can open the detail
 * subpage for the improvement that uses a given dossier material.
 */
public interface HoloImprovementOverviewAccess {
    void tetraInsight$openDetailForMaterial(
            String materialKey, ItemStack materialStack);
}
