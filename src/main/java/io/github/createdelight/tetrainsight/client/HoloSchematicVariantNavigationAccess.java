package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;

public interface HoloSchematicVariantNavigationAccess {
    default void tetraInsight$openVariantImprovements(OutcomePreview preview) {
        tetraInsight$openVariantImprovements(preview, "", ItemStack.EMPTY);
    }

    void tetraInsight$openVariantImprovements(
            OutcomePreview preview,
            String materialKey,
            ItemStack materialStack);

    /**
     * Selects the variant preview that was produced by the given dossier
     * material, when such a variant exists.
     */
    void tetraInsight$selectVariantByMaterial(
            String materialKey, ItemStack materialStack);
}
