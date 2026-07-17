package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.MaterialTranslationTooltipAccess;
import io.github.createdelight.tetrainsight.client.WorkbenchMaterialInfoButtonVisual;
import io.github.createdelight.tetrainsight.client.WorkbenchMaterialInfoAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialApplicable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialTranslationGui;

@Mixin(value = HoloMaterialApplicable.class, remap = false)
public abstract class HoloMaterialApplicableMixin implements WorkbenchMaterialInfoAccess {
    @Shadow
    @Final
    private GuiTexture icon;

    @Shadow
    private List<Component> tooltip;

    @Shadow
    private IModularItem item;

    @Unique
    private boolean tetraInsight$rowLink;

    @Unique
    private String tetraInsight$slotName = "";

    @Unique
    private HoloMaterialTranslationGui tetraInsight$translation;

    @Unique
    private WorkbenchMaterialInfoButtonVisual tetraInsight$buttonVisual;

    @Override
    public void tetraInsight$showAsRowLink(
            String slotName,
            HoloMaterialTranslationGui translation,
            int width
    ) {
        HoloMaterialApplicable self = (HoloMaterialApplicable) (Object) this;
        tetraInsight$rowLink = true;
        tetraInsight$slotName = slotName;
        tetraInsight$translation = translation;
        if (tetraInsight$buttonVisual == null) {
            tetraInsight$buttonVisual = new WorkbenchMaterialInfoButtonVisual(
                    self::hasFocus,
                    () -> item != null
            );
            self.addChild(tetraInsight$buttonVisual);
        }
        tetraInsight$buttonVisual.setText(slotName);
        tetraInsight$buttonVisual.setVisible(true);
        icon.setVisible(false);
        self.setX(Math.max(27, width - 46));
        self.setY(1);
        self.setWidth(46);
        self.setHeight(15);
    }

    @Override
    public void tetraInsight$restoreCompactIcon() {
        HoloMaterialApplicable self = (HoloMaterialApplicable) (Object) this;
        tetraInsight$rowLink = false;
        tetraInsight$slotName = "";
        tetraInsight$translation = null;
        if (tetraInsight$buttonVisual != null) {
            tetraInsight$buttonVisual.setVisible(false);
        }
        icon.setVisible(true);
        self.setX(1);
        self.setY(9);
        self.setWidth(9);
        self.setHeight(9);
    }

    @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$replaceDenseHoverWithMaterialEntry(
            CallbackInfoReturnable<List<Component>> cir
    ) {
        if (!tetraInsight$rowLink) {
            return;
        }

        HoloMaterialApplicable self = (HoloMaterialApplicable) (Object) this;
        boolean focused = self.hasFocus();
        boolean available = item != null;

        if (!focused) {
            cir.setReturnValue(null);
            return;
        }

        if (Screen.hasShiftDown()) {
            cir.setReturnValue(tetraInsight$fullTooltip());
            return;
        }

        List<Component> summary = new ArrayList<>();
        summary.add(Component.translatable(
                        "tetra_insight.workbench.material_info.heading",
                        tetraInsight$slotName
                )
                .withStyle(ChatFormatting.WHITE));
        summary.add(Component.translatable("tetra_insight.workbench.material_info.summary")
                .withStyle(ChatFormatting.GRAY));
        summary.add(Component.translatable(available
                        ? "tetra_insight.workbench.material_info.open"
                        : "tetra_insight.workbench.material_info.unavailable")
                .withStyle(available ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
        summary.add(Component.translatable("tetra_insight.workbench.material_info.shift")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        cir.setReturnValue(List.copyOf(summary));
    }

    @Unique
    private List<Component> tetraInsight$fullTooltip() {
        List<Component> combined = new ArrayList<>();
        if (tooltip != null) {
            combined.addAll(tooltip);
        }
        if (tetraInsight$translation instanceof MaterialTranslationTooltipAccess access) {
            List<Component> translationTooltip = access.tetraInsight$translationTooltip();
            if (translationTooltip != null && !translationTooltip.isEmpty()) {
                combined.add(Component.empty());
                combined.addAll(translationTooltip);
            }
        }
        return List.copyOf(combined);
    }
}
