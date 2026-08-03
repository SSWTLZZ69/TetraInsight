package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record MaterialItemUsageSnapshot(
        String itemId,
        ItemStack itemStack,
        String name,
        List<MaterialModuleUsageSnapshot> modules
) {
    public MaterialItemUsageSnapshot {
        itemStack = itemStack.copy();
        modules = List.copyOf(modules);
    }

    @Override
    public ItemStack itemStack() {
        return itemStack.copy();
    }
}
