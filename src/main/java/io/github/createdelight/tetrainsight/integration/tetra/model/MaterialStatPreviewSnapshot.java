package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.world.item.ItemStack;

public record MaterialStatPreviewSnapshot(
        ItemStack currentStack,
        ItemStack previewStack
) {
    public MaterialStatPreviewSnapshot {
        currentStack = currentStack.copy();
        previewStack = previewStack.copy();
    }

    @Override
    public ItemStack currentStack() {
        return currentStack.copy();
    }

    @Override
    public ItemStack previewStack() {
        return previewStack.copy();
    }
}
