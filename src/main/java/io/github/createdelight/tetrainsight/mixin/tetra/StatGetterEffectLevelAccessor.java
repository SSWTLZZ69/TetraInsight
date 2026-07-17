package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectLevel;

@Mixin(value = StatGetterEffectLevel.class, remap = false)
public interface StatGetterEffectLevelAccessor {
    @Accessor("effect")
    ItemEffect tetraInsight$getEffect();
}
