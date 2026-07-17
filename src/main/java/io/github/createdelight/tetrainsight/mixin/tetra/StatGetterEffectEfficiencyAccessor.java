package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectEfficiency;

@Mixin(value = StatGetterEffectEfficiency.class, remap = false)
public interface StatGetterEffectEfficiencyAccessor {
    @Accessor("effect")
    ItemEffect tetraInsight$getEffect();
}
