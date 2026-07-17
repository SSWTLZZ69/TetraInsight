package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.PersistentVerticalTabButtonAccess;
import io.github.createdelight.tetrainsight.client.PersistentVerticalTabGroupAccess;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.gui.VerticalTabButtonGui;
import se.mickelus.tetra.gui.VerticalTabGroupGui;

@Mixin(value = VerticalTabGroupGui.class, remap = false)
public abstract class VerticalTabGroupGuiMixin implements PersistentVerticalTabGroupAccess {
    @Shadow
    @Final
    private VerticalTabButtonGui[] buttons;

    @Shadow
    @Final
    private Consumer<Integer> clickHandler;

    @Unique
    private boolean tetraInsight$persistentLabels;

    @Override
    public void tetraInsight$setPersistentLabels(@Nullable Component... unavailableReasons) {
        tetraInsight$persistentLabels = true;
        for (int index = 0; index < buttons.length; index++) {
            Component reason = index < unavailableReasons.length ? unavailableReasons[index] : null;
            ((PersistentVerticalTabButtonAccess) buttons[index])
                    .tetraInsight$setPersistentLabel(reason);
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$ignoreUnavailablePersistentShortcut(char key, CallbackInfo ci) {
        if (!tetraInsight$persistentLabels) {
            return;
        }

        String keybindings = "asdfg";
        for (int index = 0; index < buttons.length && index < keybindings.length(); index++) {
            if (keybindings.charAt(index) != key) {
                continue;
            }

            if (((PersistentVerticalTabButtonAccess) buttons[index]).tetraInsight$hasContent()) {
                clickHandler.accept(index);
                ((VerticalTabGroupGui) (Object) this).setActive(index);
            }
            ci.cancel();
            return;
        }
    }
}
