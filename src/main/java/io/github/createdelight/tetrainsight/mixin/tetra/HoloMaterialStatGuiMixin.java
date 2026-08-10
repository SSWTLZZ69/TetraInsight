package io.github.createdelight.tetrainsight.mixin.tetra;

import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.gui.stats.getter.ILabelGetter;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialStatGui;
import se.mickelus.tetra.module.data.MaterialData;

@Mixin(value = HoloMaterialStatGui.class, remap = false)
public abstract class HoloMaterialStatGuiMixin {
    @Shadow
    List<Component> tooltip;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void tetraInsight$explainMaterialStat(
            int x,
            int y,
            String statKey,
            ILabelGetter valueFormatter,
            Function<MaterialData, Float> getter,
            CallbackInfo ci
    ) {
        tooltip = List.of(
                Component.translatable("tetra.holo.craft.materials.stat." + statKey)
                        .withStyle(ChatFormatting.WHITE),
                Component.translatable("tetra_insight.holo.material_stat." + statKey + ".description")
                        .withStyle(ChatFormatting.GRAY)
        );
    }
}
