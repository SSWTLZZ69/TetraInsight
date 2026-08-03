package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public interface HoloUsageNavigationAccess {
    void tetraInsight$navigateSchematic(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic
    );

    void tetraInsight$navigateImprovement(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic parentSchematic,
            OutcomePreview parentPreview
    );
}
