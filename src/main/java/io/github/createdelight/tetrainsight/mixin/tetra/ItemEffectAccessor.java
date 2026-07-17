package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.effect.ItemEffect;

import java.util.Map;

@Mixin(value = ItemEffect.class, remap = false)
public interface ItemEffectAccessor {
    @Accessor("effectMap")
    static Map<String, ItemEffect> tetraInsight$getEffectMap() {
        throw new AssertionError();
    }
}
