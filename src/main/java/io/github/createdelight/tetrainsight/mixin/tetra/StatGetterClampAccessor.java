package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.getter.StatGetterClamp;

@Mixin(value = StatGetterClamp.class, remap = false)
public interface StatGetterClampAccessor {
    @Accessor("statGetter")
    IStatGetter tetraInsight$getStatGetter();
}
