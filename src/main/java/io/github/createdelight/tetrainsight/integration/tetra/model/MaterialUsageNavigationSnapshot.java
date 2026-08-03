package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.world.item.ItemStack;

public record MaterialUsageNavigationSnapshot(
        ItemStack itemStack,
        String slot,
        String schematicKey,
        String parentSchematicKey
) {
    public MaterialUsageNavigationSnapshot {
        itemStack = itemStack.copy();
    }

    @Override
    public ItemStack itemStack() {
        return itemStack.copy();
    }

    public static MaterialUsageNavigationSnapshot schematic(
            ItemStack itemStack,
            String slot,
            String schematicKey
    ) {
        return new MaterialUsageNavigationSnapshot(itemStack, slot, schematicKey, "");
    }

    public static MaterialUsageNavigationSnapshot improvement(
            ItemStack parentStack,
            String slot,
            String schematicKey,
            String parentSchematicKey
    ) {
        return new MaterialUsageNavigationSnapshot(
                parentStack, slot, schematicKey, parentSchematicKey);
    }

    public boolean opensImprovementPage() {
        return parentSchematicKey != null && !parentSchematicKey.isBlank();
    }
}
