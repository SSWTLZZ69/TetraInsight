package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloFilterButton;

@Mixin(value = HoloFilterButton.class, remap = false)
public interface HoloFilterButtonAccessor {
    @Accessor("inputFocused")
    boolean tetraInsight$isInputFocused();
}
