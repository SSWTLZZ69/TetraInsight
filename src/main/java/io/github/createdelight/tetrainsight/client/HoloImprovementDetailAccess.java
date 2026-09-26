package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;

/**
 * Lets material-usage navigation open the improvement overview entry that
 * applies the dossier material instead of leaving the user at the overview.
 */
public interface HoloImprovementDetailAccess {
    void tetraInsight$openImprovementByMaterial(
            String materialKey, ItemStack materialStack);
}
