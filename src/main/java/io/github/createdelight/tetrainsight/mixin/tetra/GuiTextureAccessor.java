package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.mutil.gui.GuiTexture;

@Mixin(value = GuiTexture.class, remap = false)
public interface GuiTextureAccessor {
    @Accessor("textureX")
    int tetraInsight$getTextureX();
}
