package io.github.createdelight.tetrainsight.mixin.tetra;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiSynergyIndicator;

@Mixin(value = GuiSynergyIndicator.class, remap = false)
public interface GuiSynergyIndicatorAccessor {
    @Accessor("tooltip")
    List<Component> tetraInsight$getTooltip();

    @Accessor("indicator")
    GuiTexture tetraInsight$getIndicator();
}
