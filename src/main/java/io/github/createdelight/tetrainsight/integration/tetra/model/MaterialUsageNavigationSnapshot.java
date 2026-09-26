package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.world.item.ItemStack;

public record MaterialUsageNavigationSnapshot(
        ItemStack itemStack,
        String slot,
        String schematicKey,
        String parentSchematicKey,
        String materialKey,
        ItemStack materialStack
) {
    public MaterialUsageNavigationSnapshot {
        itemStack = itemStack.copy();
        materialKey = materialKey != null ? materialKey : "";
        materialStack = materialStack != null ? materialStack.copy()
                : ItemStack.EMPTY;
    }

    @Override
    public ItemStack itemStack() {
        return itemStack.copy();
    }

    @Override
    public ItemStack materialStack() {
        return materialStack.copy();
    }

    public static MaterialUsageNavigationSnapshot schematic(
            ItemStack itemStack,
            String slot,
            String schematicKey
    ) {
        return schematic(itemStack, slot, schematicKey, "", ItemStack.EMPTY);
    }

    public static MaterialUsageNavigationSnapshot schematic(
            ItemStack itemStack,
            String slot,
            String schematicKey,
            String materialKey,
            ItemStack materialStack
    ) {
        return new MaterialUsageNavigationSnapshot(
                itemStack, slot, schematicKey, "", materialKey, materialStack);
    }

    public static MaterialUsageNavigationSnapshot improvement(
            ItemStack parentStack,
            String slot,
            String schematicKey,
            String parentSchematicKey
    ) {
        return improvement(parentStack, slot, schematicKey, parentSchematicKey,
                "", ItemStack.EMPTY);
    }

    public static MaterialUsageNavigationSnapshot improvement(
            ItemStack parentStack,
            String slot,
            String schematicKey,
            String parentSchematicKey,
            String materialKey,
            ItemStack materialStack
    ) {
        return new MaterialUsageNavigationSnapshot(
                parentStack, slot, schematicKey, parentSchematicKey,
                materialKey, materialStack);
    }

    public boolean opensImprovementPage() {
        return parentSchematicKey != null && !parentSchematicKey.isBlank();
    }

    /**
     * The material context of the dossier entry this navigation came from;
     * either a resolved material key or a special ingredient stack.
     */
    public boolean hasMaterialContext() {
        return !materialKey.isBlank() || !materialStack.isEmpty();
    }
}
