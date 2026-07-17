package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.PersistentVerticalTabButtonAccess;
import io.github.createdelight.tetrainsight.client.TabUnavailableReasonGui;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.gui.GuiKeybinding;
import se.mickelus.tetra.gui.VerticalTabButtonGui;

@Mixin(value = VerticalTabButtonGui.class, remap = false)
public abstract class VerticalTabButtonGuiMixin implements PersistentVerticalTabButtonAccess {
    @Shadow
    protected boolean hasContent;

    @Shadow
    protected boolean isActive;

    @Shadow
    protected GuiString label;

    @Shadow
    protected GuiKeybinding keybinding;

    @Shadow
    protected KeyframeAnimation labelShow;

    @Shadow
    protected KeyframeAnimation labelHide;

    @Shadow
    protected KeyframeAnimation keybindShow;

    @Shadow
    protected KeyframeAnimation keybindHide;

    @Unique
    private boolean tetraInsight$persistentLabel;

    @Override
    public void tetraInsight$setPersistentLabel(@Nullable Component unavailableReason) {
        tetraInsight$persistentLabel = true;
        if (unavailableReason != null) {
            VerticalTabButtonGui button = (VerticalTabButtonGui) (Object) this;
            button.addChild(new TabUnavailableReasonGui(
                    button.getWidth(),
                    button::hasFocus,
                    () -> hasContent,
                    unavailableReason
            ));
        }
        tetraInsight$applyPersistentStyling();
    }

    @Override
    public boolean tetraInsight$hasContent() {
        return hasContent;
    }

    @Inject(method = "updateStyling", at = @At("RETURN"), remap = false)
    private void tetraInsight$keepLabelStyled(CallbackInfo ci) {
        tetraInsight$applyPersistentStyling();
    }

    @Inject(method = "onFocus", at = @At("RETURN"), remap = false)
    private void tetraInsight$keepLabelVisibleOnFocus(CallbackInfo ci) {
        tetraInsight$applyPersistentStyling();
    }

    @Inject(method = "onBlur", at = @At("RETURN"), remap = false)
    private void tetraInsight$keepLabelVisibleOnBlur(CallbackInfo ci) {
        tetraInsight$applyPersistentStyling();
    }

    @Unique
    private void tetraInsight$applyPersistentStyling() {
        if (!tetraInsight$persistentLabel || label == null || keybinding == null) {
            return;
        }

        labelShow.stop();
        labelHide.stop();
        keybindShow.stop();
        keybindHide.stop();

        boolean focused = ((VerticalTabButtonGui) (Object) this).hasFocus();
        int color;
        if (isActive) {
            color = focused ? 0xffffcc : 0xffffff;
        } else if (hasContent) {
            color = focused ? 0x8f8faf : 0x7f7f7f;
        } else {
            color = focused ? 0x7f7f7f : 0x5f5f5f;
        }

        label.setColor(color);
        label.setOpacity(1.0f);
        keybinding.setOpacity(hasContent || isActive ? 1.0f : 0.5f);
    }
}
