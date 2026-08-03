package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloMaterialGroupFoldAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialSelectionAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;
import se.mickelus.mutil.gui.impl.GuiHorizontalScrollable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialDetailGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialListGui;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.MaterialData;

@Mixin(value = HoloMaterialListGui.class, remap = false)
public abstract class HoloMaterialListGuiMixin
        implements HoloMaterialSelectionAccess, HoloMaterialDossierModalAccess,
        HoloMaterialDossierLifecycleAccess {
    @Shadow
    @Final
    private GuiHorizontalScrollable groupsScroll;

    @Shadow
    @Final
    private GuiHorizontalLayoutGroup groups;

    @Shadow
    @Final
    private HoloMaterialDetailGui detail;

    @Shadow
    private MaterialData hoveredItem;

    @Unique
    private boolean tetraInsight$dossierModal;

    @Shadow
    private void onSelect(MaterialData material) {
    }

    @Inject(method = "updateGroups", at = @At("RETURN"), remap = false)
    private void tetraInsight$configureMaterialGroupFolds(CallbackInfo ci) {
        for (GuiElement child : groups.getChildren()) {
            if (child instanceof HoloMaterialGroupFoldAccess group) {
                group.tetraInsight$configureFold(
                        () -> tetraInsight$toggleGroup(group));
            }
        }
        tetraInsight$refreshGroupLayout();
    }

    @Unique
    private void tetraInsight$toggleGroup(HoloMaterialGroupFoldAccess target) {
        boolean expandTarget = !target.tetraInsight$isExpanded();
        for (GuiElement child : groups.getChildren()) {
            if (child instanceof HoloMaterialGroupFoldAccess group) {
                group.tetraInsight$setExpanded(
                        expandTarget && group == target);
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

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$blockModalRightClick(
            int mouseX,
            int mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (tetraInsight$dossierModal && button == 1) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void tetraInsight$setMaterialDossierModal(boolean open) {
        tetraInsight$dossierModal = open;
        if (open) {
            groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
            hoveredItem = null;
        }
        groupsScroll.setVisible(!open);
    }

    @Override
    public boolean tetraInsight$isMaterialDossierModal() {
        return tetraInsight$dossierModal;
    }

    @Override
    public void tetraInsight$resetMaterialDossier() {
        tetraInsight$dossierModal = false;
        hoveredItem = null;
        groupsScroll.setVisible(true);
        ((HoloMaterialDossierLifecycleAccess) detail)
                .tetraInsight$resetMaterialDossier();
    }

    @Override
    public boolean tetraInsight$selectMaterial(String materialKey) {
        MaterialData material = DataManager.instance.materialData.getData().values().stream()
                .filter(candidate -> candidate != null && materialKey.equals(candidate.key))
                .findFirst()
                .orElse(null);
        if (material == null) {
            return false;
        }
        groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
        hoveredItem = null;
        onSelect(material);
        return true;
    }
}
