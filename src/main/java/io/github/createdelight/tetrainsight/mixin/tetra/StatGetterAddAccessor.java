package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.getter.StatGetterAdd;

@Mixin(value = StatGetterAdd.class, remap = false)
public interface StatGetterAddAccessor {
    @Accessor("statGetters")
    IStatGetter[] tetraInsight$getStatGetters();
}
