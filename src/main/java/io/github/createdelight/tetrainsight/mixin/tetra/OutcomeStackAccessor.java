package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.OutcomeStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = OutcomeStack.class, remap = false)
public interface OutcomeStackAccessor {
    @Accessor("schematic")
    UpgradeSchematic tetraInsight$getSchematic();

    @Accessor("preview")
    OutcomePreview tetraInsight$getPreview();
}
