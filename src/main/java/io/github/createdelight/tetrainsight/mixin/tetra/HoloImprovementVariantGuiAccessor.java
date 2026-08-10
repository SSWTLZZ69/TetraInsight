package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementVariantGui;

@Mixin(value = HoloImprovementVariantGui.class, remap = false)
public interface HoloImprovementVariantGuiAccessor {
    @Accessor("backdrop")
    GuiTexture tetraInsight$getBackdrop();

    @Accessor("label")
    GuiString tetraInsight$getLabel();
}
