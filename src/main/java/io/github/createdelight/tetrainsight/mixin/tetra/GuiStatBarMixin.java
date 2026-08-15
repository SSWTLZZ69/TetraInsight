package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.EffectApplicabilityTooltipFormatter;
import io.github.createdelight.tetrainsight.integration.tetra.effect.EffectApplicabilityResolver;
import io.github.createdelight.tetrainsight.integration.tetra.effect.EffectStatGetterResolver;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilitySnapshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.bar.GuiStatBar;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;

import java.util.List;

@Mixin(value = GuiStatBar.class, remap = false)
public abstract class GuiStatBarMixin {
    @Shadow
    protected IStatGetter statGetter;

    @Unique
    private EffectApplicabilitySnapshot tetraInsight$effectApplicability;

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$captureEffectContext(Player player,
            ItemStack currentStack, ItemStack previewStack, String slot, String improvement,
            CallbackInfo ci) {
        ItemEffect effect = EffectStatGetterResolver.resolve(statGetter).orElse(null);
        tetraInsight$effectApplicability = effect == null
                ? null
                : EffectApplicabilityResolver.resolve(effect, currentStack, previewStack);
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true, remap = false)
    private void tetraInsight$appendEffectApplicability(
            CallbackInfoReturnable<List<Component>> cir) {
        if (!((GuiStatBar) (Object) this).hasFocus()
                || tetraInsight$effectApplicability == null
                || cir.getReturnValue() == null) {
            return;
        }
        cir.setReturnValue(EffectApplicabilityTooltipFormatter.append(
                cir.getReturnValue(), tetraInsight$effectApplicability, Screen.hasShiftDown()));
    }
}
