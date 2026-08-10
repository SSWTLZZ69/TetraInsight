package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloMaterialGroupFoldAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierPanelGui;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialSelectionAccess;
import io.github.createdelight.tetrainsight.client.HoloSpecialMaterialDossierAccess;
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
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialDetailGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialListGui;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.MaterialData;
import net.minecraft.world.item.ItemStack;

@Mixin(value = HoloMaterialListGui.class, remap = false)
public abstract class HoloMaterialListGuiMixin
        implements HoloMaterialSelectionAccess, HoloMaterialDossierModalAccess,
        HoloMaterialDossierLifecycleAccess, HoloSpecialMaterialDossierAccess {
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

    @Unique
    private HoloMaterialDossierPanelGui tetraInsight$specialDossierPanel;

    @Unique
    private boolean tetraInsight$specialDossierOpen;

    @Unique
    private boolean tetraInsight$specialDetailWasVisible;

    @Unique
    private boolean tetraInsight$specialDetailStateCaptured;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addSpecialDossier(
            int x,
            int y,
            int width,
            int height,
            CallbackInfo ci
    ) {
        HoloMaterialListGui self = (HoloMaterialListGui) (Object) this;
        tetraInsight$specialDossierPanel = new HoloMaterialDossierPanelGui(
                0,
                0,
                width,
                205,
                this::tetraInsight$onSpecialDossierVisibilityChanged);
        self.addChild(tetraInsight$specialDossierPanel);
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
            groupsScroll.setVisible(false);
        } else if (tetraInsight$specialDossierOpen) {
            tetraInsight$specialDossierOpen = false;
            if (tetraInsight$specialDossierPanel != null) {
                tetraInsight$specialDossierPanel.closeImmediately();
                tetraInsight$specialDossierPanel.reset();
            }
            tetraInsight$restoreSpecialDetailVisibility();
            groupsScroll.setVisible(true);
        } else {
            groupsScroll.setVisible(true);
        }
    }

    @Override
    public boolean tetraInsight$isMaterialDossierModal() {
        return tetraInsight$dossierModal;
    }

    @Override
    public void tetraInsight$resetMaterialDossier() {
        tetraInsight$dossierModal = false;
        boolean hadSpecialDossier = tetraInsight$specialDossierOpen
                || tetraInsight$specialDetailStateCaptured;
        tetraInsight$specialDossierOpen = false;
        hoveredItem = null;
        groupsScroll.setVisible(true);
        if (tetraInsight$specialDossierPanel != null) {
            tetraInsight$specialDossierPanel.reset();
        }
        if (hadSpecialDossier) {
            tetraInsight$restoreSpecialDetailVisibility();
        }
        ((HoloMaterialDossierLifecycleAccess) detail)
                .tetraInsight$resetMaterialDossier();
    }

    @Override
    public void tetraInsight$openSpecialMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty() || tetraInsight$specialDossierPanel == null) {
            return;
        }
        if (!tetraInsight$specialDossierOpen) {
            tetraInsight$specialDetailWasVisible = detail.isVisible();
            tetraInsight$specialDetailStateCaptured = true;
        }
        tetraInsight$specialDossierOpen = true;
        tetraInsight$dossierModal = true;
        groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
        hoveredItem = null;
        groupsScroll.setVisible(false);
        detail.setVisible(false);
        tetraInsight$specialDossierPanel.updateSpecial(stack);
        tetraInsight$specialDossierPanel.open();
    }

    @Unique
    private void tetraInsight$onSpecialDossierVisibilityChanged(boolean visible) {
        if (visible) {
            return;
        }
        tetraInsight$specialDossierOpen = false;
        tetraInsight$dossierModal = false;
        tetraInsight$restoreSpecialDetailVisibility();
        groupsScroll.setVisible(true);
    }

    @Unique
    private void tetraInsight$restoreSpecialDetailVisibility() {
        if (tetraInsight$specialDetailStateCaptured) {
            detail.setVisible(tetraInsight$specialDetailWasVisible);
            tetraInsight$specialDetailStateCaptured = false;
        }
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
