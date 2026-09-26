package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public interface HoloUsageNavigationAccess {
    default void tetraInsight$navigateSchematic(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic
    ) {
        tetraInsight$navigateSchematic(item, itemStack, slot, schematic,
                "", ItemStack.EMPTY);
    }

    void tetraInsight$navigateSchematic(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic,
            String materialKey,
            ItemStack materialStack
    );

    default void tetraInsight$navigateImprovement(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic parentSchematic,
            OutcomePreview parentPreview
    ) {
        tetraInsight$navigateImprovement(item, itemStack, slot, parentSchematic,
                parentPreview, "", ItemStack.EMPTY);
    }

    void tetraInsight$navigateImprovement(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic parentSchematic,
            OutcomePreview parentPreview,
            String materialKey,
            ItemStack materialStack
    );
}
