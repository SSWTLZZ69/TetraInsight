package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloMaterialDossierButtonGui;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierPanelGui;
import io.github.createdelight.tetrainsight.integration.tetra.MaterialInsightIndex;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialDetailGui;
import se.mickelus.tetra.module.data.MaterialData;

@Mixin(value = HoloMaterialDetailGui.class, remap = false)
public abstract class HoloMaterialDetailGuiMixin
        implements HoloMaterialDossierLifecycleAccess {
    @Shadow
    @Final
    private GuiElement content;

    @Unique
    private HoloMaterialDossierButtonGui tetraInsight$dossierButton;

    @Unique
    private HoloMaterialDossierPanelGui tetraInsight$dossierPanel;

    @Unique
    private String tetraInsight$materialKey = "";

    @Unique
    private boolean tetraInsight$dossierAvailable;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addMaterialDossier(int x, int y, int width, CallbackInfo ci) {
        HoloMaterialDetailGui self = (HoloMaterialDetailGui) (Object) this;
        tetraInsight$dossierButton = new HoloMaterialDossierButtonGui(
                Math.max(2, width - 58),
                2,
                () -> {
                    tetraInsight$dossierPanel.toggle();
                    tetraInsight$dossierButton.setActive(tetraInsight$dossierPanel.isVisible());
                });
        tetraInsight$dossierButton.setVisible(false);
        self.addChild(tetraInsight$dossierButton);

        tetraInsight$dossierPanel = new HoloMaterialDossierPanelGui(
                0,
                -76,
                width,
                205,
                this::tetraInsight$setDossierModal);
        self.addChild(tetraInsight$dossierPanel);
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$updateMaterialDossier(
            MaterialData selected,
            MaterialData hovered,
            CallbackInfo ci
    ) {
        MaterialData displayed = hovered != null ? hovered : selected;
        if (displayed == null || displayed.key == null) {
            tetraInsight$materialKey = "";
            tetraInsight$dossierAvailable = false;
            tetraInsight$dossierButton.setVisible(false);
            tetraInsight$dossierPanel.update(null);
            return;
        }

        MaterialProfileSnapshot profile = MaterialInsightIndex.findProfile(displayed.key).orElse(null);
        boolean available = profile != null;
        tetraInsight$dossierAvailable = available;
        tetraInsight$dossierButton.setEnabled(available);
        tetraInsight$dossierButton.update(available
                ? MaterialInsightIndex.usageCount(displayed.key)
                : 0);

        tetraInsight$materialKey = displayed.key;
        tetraInsight$dossierPanel.update(profile);
        tetraInsight$dossierButton.setVisible(!tetraInsight$dossierPanel.isVisible());
        tetraInsight$dossierButton.setActive(tetraInsight$dossierPanel.isVisible());
    }

    @Inject(method = "hide", at = @At("HEAD"), remap = false)
    private void tetraInsight$closeMaterialDossier(CallbackInfo ci) {
        if (tetraInsight$dossierPanel != null) {
            tetraInsight$dossierPanel.closeImmediately();
        }
    }

    @Unique
    private void tetraInsight$setDossierModal(boolean open) {
        content.updateFocusState(0, 0, -1000000, -1000000);
        content.setVisible(!open);
        tetraInsight$dossierButton.setVisible(!open && !tetraInsight$materialKey.isBlank());
        tetraInsight$dossierButton.setEnabled(tetraInsight$dossierAvailable);
        tetraInsight$dossierButton.setActive(open);
        ((HoloMaterialDossierModalAccess) HoloGui.getInstance())
                .tetraInsight$setMaterialDossierModal(open);
    }

    @Override
    public void tetraInsight$resetMaterialDossier() {
        if (tetraInsight$dossierPanel != null) {
            tetraInsight$dossierPanel.reset();
        }
        tetraInsight$materialKey = "";
        tetraInsight$dossierAvailable = false;
        if (tetraInsight$dossierButton != null) {
            tetraInsight$dossierButton.setVisible(false);
            tetraInsight$dossierButton.setActive(false);
        }
        content.setVisible(true);
    }
}
