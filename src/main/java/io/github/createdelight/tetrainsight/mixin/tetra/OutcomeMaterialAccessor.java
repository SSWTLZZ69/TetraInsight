package io.github.createdelight.tetrainsight.mixin.tetra;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.module.schematic.OutcomeMaterial;

@Mixin(value = OutcomeMaterial.class, remap = false)
public interface OutcomeMaterialAccessor {
    @Accessor("tagLocation")
    TagKey<Item> tetraInsight$getTagLocation();
}
