package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.PersistentVerticalTabGroupAccess;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.blocks.workbench.gui.GuiSlotDetail;
import se.mickelus.tetra.gui.VerticalTabGroupGui;

@Mixin(value = GuiSlotDetail.class, remap = false)
public abstract class GuiSlotDetailMixin {
    @Shadow
    @Final
    private VerticalTabGroupGui tabGroup;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void tetraInsight$keepWorkbenchTabLabelsVisible(CallbackInfo ci) {
        ((PersistentVerticalTabGroupAccess) tabGroup).tetraInsight$setPersistentLabels(
                Component.translatable("tetra_insight.workbench.tabs.details_unavailable"),
                null,
                Component.translatable("tetra_insight.workbench.tabs.tweak_unavailable")
        );
    }
}
