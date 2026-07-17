package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record FixedConsumableOutcomeSnapshot(
        List<ItemStack> materials,
        ResourceLocation materialTag,
        int count
) {
    public FixedConsumableOutcomeSnapshot {
        materials = materials.stream()
                .map(ItemStack::copy)
                .toList();
    }

}
