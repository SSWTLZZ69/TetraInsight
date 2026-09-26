package io.github.createdelight.tetrainsight.mixin.tetra;

import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloFilterButton;

/**
 * Tetra's filter field only reacts to Backspace. This mixin adds Delete and
 * Ctrl+Delete handling while the input is focused, matching what the sorter
 * search already forwards from its popover.
 */
@Mixin(value = HoloFilterButton.class, remap = false)
public abstract class HoloFilterButtonMixin {
    @Shadow
    private boolean inputFocused;

    @Shadow
    private String filter;

    @Shadow
    public abstract void updateFilter(String filter);

    @Inject(method = "onKeyPress", at = @At("HEAD"), cancellable = true,
            remap = false)
    private void tetraInsight$forwardDeleteKey(int keyCode, int scanCode,
            int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!inputFocused || keyCode != GLFW.GLFW_KEY_DELETE) {
            return;
        }
        updateFilter(Screen.hasControlDown()
                ? ""
                : StringUtils.chop(filter));
        cir.setReturnValue(true);
    }
}
