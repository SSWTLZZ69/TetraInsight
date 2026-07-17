package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloImprovementButtonAccess;
import io.github.createdelight.tetrainsight.client.HoloImprovementCountAccess;
import io.github.createdelight.tetrainsight.client.HoloHoningTargetAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialImpactButtonGui;
import io.github.createdelight.tetrainsight.client.HoloMaterialImpactPanelGui;
import io.github.createdelight.tetrainsight.client.HoloSchematicImprovementEntryAccess;
import io.github.createdelight.tetrainsight.client.HoloSortMaterialScalingAccess;
import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialTranslationGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSchematicGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSortButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloVariantDetailGui;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;

import java.util.ArrayList;
import java.util.function.Consumer;

@Mixin(value = HoloSchematicGui.class, remap = false)
public abstract class HoloSchematicGuiMixin
        implements HoloSchematicImprovementEntryAccess, HoloHoningTargetAccess {
    @Shadow
    @Final
    private Consumer<OutcomePreview> onVariantOpen;

    @Shadow
    @Final
    private HoloVariantDetailGui detail;

    @Shadow
    @Final
    private GuiElement listGroup;

    @Shadow
    @Final
    private HoloMaterialTranslationGui translation;

    @Shadow
    @Final
    private HoloSortButton sortbutton;

    @Shadow
    private OutcomePreview selectedVariant;

    @Unique
    private HoloImprovementButton tetraInsight$improvementEntry;

    @Unique
    private HoloMaterialImpactButtonGui tetraInsight$materialImpactButton;

    @Unique
    private HoloMaterialImpactPanelGui tetraInsight$materialImpactPanel;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addPersistentImprovementEntry(int x, int y, int width, int height,
            Consumer<OutcomePreview> onVariantOpen, CallbackInfo ci) {
        tetraInsight$improvementEntry = new HoloImprovementButton(0, 0, () -> {
            if (selectedVariant != null) {
                this.onVariantOpen.accept(selectedVariant);
            }
        });
        ((HoloImprovementButtonAccess) tetraInsight$improvementEntry).tetraInsight$showSelectionPrompt();
        tetraInsight$repositionImprovementEntry();
        HoloSchematicGui self = (HoloSchematicGui) (Object) this;
        self.addChild(tetraInsight$improvementEntry);

        tetraInsight$materialImpactPanel = new HoloMaterialImpactPanelGui(
                4,
                14,
                () -> tetraInsight$materialImpactButton.setActive(false)
        );
        tetraInsight$materialImpactButton = new HoloMaterialImpactButtonGui(() -> {
            tetraInsight$materialImpactPanel.toggle();
            tetraInsight$materialImpactButton.setActive(tetraInsight$materialImpactPanel.isVisible());
        });
        translation.setVisible(false);
        translation.setWidth(0);
        listGroup.getChildren(GuiHorizontalLayoutGroup.class).stream()
                .filter(group -> group.getY() == 0)
                .findFirst()
                .ifPresent(group -> {
                    var existingControls = new ArrayList<>(group.getChildren());
                    group.clearChildren();
                    group.addChild(tetraInsight$materialImpactButton);
                    existingControls.forEach(group::addChild);
                    group.forceLayout();
                });
        self.addChild(tetraInsight$materialImpactPanel);
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$resetPersistentImprovementEntry(IModularItem item, String slot,
            UpgradeSchematic schematic, CallbackInfo ci) {
        tetraInsight$refreshImprovementEntry();
        TetraDataProbe.findSchematic(schematic.getKey())
                .filter(snapshot -> snapshot.displayTranslation().provenance()
                        != TranslationProvenance.UNAVAILABLE)
                .ifPresentOrElse(snapshot -> {
                    tetraInsight$materialImpactPanel.update(snapshot);
                    tetraInsight$materialImpactButton.setEnabled(true);
                }, () -> tetraInsight$materialImpactButton.setEnabled(false));
        tetraInsight$materialImpactPanel.close();
    }

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void tetraInsight$setActualSorterTargets(IModularItem item, String slot,
            UpgradeSchematic schematic, CallbackInfo ci) {
        ((HoloSortMaterialScalingAccess) sortbutton).tetraInsight$setActualMaterialScaling(
                TetraDataProbe.findActualMaterialScaling(schematic.getKey()));
    }

    @Inject(method = "onVariantSelect", at = @At("RETURN"), remap = false)
    private void tetraInsight$updatePersistentImprovementEntry(OutcomePreview preview, CallbackInfo ci) {
        tetraInsight$refreshImprovementEntry();
    }

    @Inject(method = "openVariant", at = @At("HEAD"), remap = false)
    private void tetraInsight$closeMaterialImpactForVariant(OutcomePreview preview, CallbackInfo ci) {
        tetraInsight$materialImpactPanel.close();
    }

    @Inject(method = "onHide", at = @At("HEAD"), remap = false)
    private void tetraInsight$closeMaterialImpactWhenHidden(CallbackInfoReturnable<Boolean> cir) {
        tetraInsight$materialImpactPanel.close();
    }

    @Unique
    private void tetraInsight$refreshImprovementEntry() {
        if (selectedVariant == null) {
            ((HoloImprovementButtonAccess) tetraInsight$improvementEntry).tetraInsight$showSelectionPrompt();
        } else {
            int count = ((HoloImprovementCountAccess) detail).tetraInsight$improvementCount();
            tetraInsight$improvementEntry.updateCount(count);
        }
        tetraInsight$repositionImprovementEntry();
    }

    @Unique
    private void tetraInsight$repositionImprovementEntry() {
        HoloSchematicGui self = (HoloSchematicGui) (Object) this;
        tetraInsight$improvementEntry.setX(self.getWidth() - tetraInsight$improvementEntry.getWidth());
        tetraInsight$improvementEntry.setY(0);
    }

    @Override
    public int tetraInsight$improvementEntryX() {
        return tetraInsight$improvementEntry.getX();
    }

    @Override
    public int tetraInsight$improvementEntryWidth() {
        return tetraInsight$improvementEntry.getWidth();
    }

    @Override
    public int tetraInsight$toolbarContentWidth() {
        return tetraInsight$findToolbarWidth((HoloSchematicGui) (Object) this, 0, 0);
    }

    @Override
    public void tetraInsight$setHoningTarget(ItemStack targetStack) {
        ((HoloHoningTargetAccess) detail).tetraInsight$setHoningTarget(targetStack);
    }

    @Unique
    private static int tetraInsight$findToolbarWidth(GuiElement parent, int offsetX, int offsetY) {
        int result = 0;
        for (GuiElement child : parent.getChildren()) {
            int childX = offsetX + child.getX();
            int childY = offsetY + child.getY();
            if (child instanceof GuiHorizontalLayoutGroup && childY == 0) {
                result = Math.max(result, childX + child.getWidth());
            }
            result = Math.max(result, tetraInsight$findToolbarWidth(child, childX, childY));
        }
        return result;
    }
}
