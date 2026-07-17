package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.FixedConsumableOutcomeSnapshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class FixedConsumableMaterialResolver {
    private FixedConsumableMaterialResolver() {
    }

    public static ItemStack[] resolve(FixedConsumableOutcomeSnapshot outcome) {
        List<ItemStack> resolved = new ArrayList<>();
        outcome.materials().stream()
                .map(ItemStack::copy)
                .forEach(resolved::add);

        if (outcome.materialTag() != null && ForgeRegistries.ITEMS.tags() != null) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, outcome.materialTag());
            ForgeRegistries.ITEMS.tags().getTag(tag).forEach(item -> {
                ItemStack stack = new ItemStack(item, Math.max(1, outcome.count()));
                boolean duplicate = resolved.stream()
                        .anyMatch(existing -> ItemStack.isSameItemSameTags(existing, stack)
                                && existing.getCount() == stack.getCount());
                if (!duplicate) {
                    resolved.add(stack);
                }
            });
        }

        return resolved.toArray(ItemStack[]::new);
    }
}
