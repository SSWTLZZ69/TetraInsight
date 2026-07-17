package io.github.createdelight.tetrainsight.mixin.tetra;

import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.gui.stats.getter.StatGetterAttribute;

@Mixin(value = StatGetterAttribute.class, remap = false)
public interface StatGetterAttributeAccessor {
    @Accessor("attribute")
    Attribute tetraInsight$getAttribute();
}
