package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloVariantGroupFoldAccess;
import io.github.createdelight.tetrainsight.client.TetraInsightConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;
import se.mickelus.mutil.gui.impl.GuiHorizontalScrollable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloVariantListGui;

@Mixin(value = HoloVariantListGui.class, remap = false)
public abstract class HoloVariantListGuiMixin {
    @Shadow
    @Final
    private GuiHorizontalScrollable groupsScroll;

    @Shadow
    @Final
    private GuiHorizontalLayoutGroup groups;

    @Inject(method = "update()V", at = @At("RETURN"), remap = false)
    private void tetraInsight$configureVariantGroupFolds(CallbackInfo ci) {
        for (GuiElement child : groups.getChildren()) {
            if (child instanceof HoloVariantGroupFoldAccess group) {
                group.tetraInsight$configureFold(
                        () -> tetraInsight$toggleGroup(group));
            }
        }
        tetraInsight$refreshGroupLayout();
    }

    @Unique
    private void tetraInsight$toggleGroup(HoloVariantGroupFoldAccess target) {
        boolean expandTarget = !target.tetraInsight$isExpanded();
        boolean singleExpansion =
                TetraInsightConfig.materialSingleExpansion.get();
        for (GuiElement child : groups.getChildren()) {
            if (child instanceof HoloVariantGroupFoldAccess group) {
                group.tetraInsight$setExpanded(
                        singleExpansion
                                ? expandTarget && group == target
                                : group == target ? expandTarget
                                        : group.tetraInsight$isExpanded());
            }
        }
        tetraInsight$refreshGroupLayout();
    }

    @Unique
    private void tetraInsight$refreshGroupLayout() {
        groups.forceLayout();
        groupsScroll.markDirty();
        groupsScroll.forceRefreshBounds();
    }
}
