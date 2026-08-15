package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.getter.StatGetterOr;

@Mixin(value = StatGetterOr.class, remap = false)
public interface StatGetterOrAccessor {
    @Accessor("statGetters")
    IStatGetter[] tetraInsight$getStatGetters();
}
