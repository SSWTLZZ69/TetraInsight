package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageNavigationSnapshot;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public final class MaterialUsageNavigator {
    private MaterialUsageNavigator() {
    }

    public static boolean open(MaterialUsageNavigationSnapshot navigation) {
        ItemStack stack = navigation.itemStack();
        if (!(stack.getItem() instanceof IModularItem modularItem)) {
            return false;
        }

        HoloGui holoGui = HoloGui.getInstance();
        if (navigation.opensImprovementPage()) {
            UpgradeSchematic parent = SchematicRegistry.getSchematic(
                    navigation.parentSchematicKey());
            ItemStack baseStack = modularItem.getDefaultStack();
            OutcomePreview parentPreview = parent == null
                    ? null
                    : Arrays.stream(parent.getPreviews(baseStack, navigation.slot()))
                            .filter(preview -> preview != null
                                    && preview.itemStack != null
                                    && ItemStack.matches(preview.itemStack, stack))
                            .findFirst()
                            .orElse(null);
            if (parent == null || parentPreview == null) {
                return false;
            }
            ((HoloUsageNavigationAccess) holoGui).tetraInsight$navigateImprovement(
                    modularItem,
                    baseStack,
                    navigation.slot(),
                    parent,
                    parentPreview);
        } else {
            UpgradeSchematic schematic = SchematicRegistry.getSchematic(
                    navigation.schematicKey());
            if (schematic == null) {
                return false;
            }
            ((HoloUsageNavigationAccess) holoGui).tetraInsight$navigateSchematic(
                    modularItem,
                    stack,
                    navigation.slot(),
                    schematic);
        }
        holoGui.onShow();
        return Minecraft.getInstance().screen == holoGui;
    }
}
