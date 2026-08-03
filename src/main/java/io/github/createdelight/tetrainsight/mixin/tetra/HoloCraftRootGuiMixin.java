package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloCraftSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloCraftMaterialNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloHoningTargetAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialSelectionAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloSchematicVariantNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloUsageNavigationAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloCraftRootGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSchematicGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;

@Mixin(value = HoloCraftRootGui.class, remap = false)
public abstract class HoloCraftRootGuiMixin
        implements HoloCraftSlotNavigationAccess, HoloCraftMaterialNavigationAccess,
        HoloMaterialDossierModalAccess, HoloMaterialDossierLifecycleAccess,
        HoloUsageNavigationAccess {
    @Shadow
    @Final
    private HoloSchematicGui schematicView;

    @Shadow
    @Final
    private HoloMaterialListGui materialsView;

    @Shadow
    private IModularItem item;

    @Shadow
    private ItemStack itemStack;

    @Shadow
    private void onSlotSelect(String slot) {
    }

    @Shadow
    private void onMaterialsSelect() {
    }

    @Inject(method = "updateState", at = @At("HEAD"), remap = false)
    private void tetraInsight$forwardHoningTarget(IModularItem item, ItemStack itemStack,
            String slot, UpgradeSchematic schematic, CallbackInfo ci) {
        ((HoloHoningTargetAccess) schematicView).tetraInsight$setHoningTarget(itemStack);
    }

    @Override
    public void tetraInsight$openSlot(
            IModularItem item,
            ItemStack itemStack,
            String slot
    ) {
        this.item = item;
        this.itemStack = itemStack;
        onSlotSelect(slot);
    }

    @Override
    public void tetraInsight$openMaterial(String materialKey) {
        onMaterialsSelect();
        ((HoloMaterialSelectionAccess) materialsView)
                .tetraInsight$selectMaterial(materialKey);
    }

    @Override
    public void tetraInsight$setMaterialDossierModal(boolean open) {
        ((HoloMaterialDossierModalAccess) materialsView)
                .tetraInsight$setMaterialDossierModal(open);
    }

    @Override
    public boolean tetraInsight$isMaterialDossierModal() {
        return ((HoloMaterialDossierModalAccess) materialsView)
                .tetraInsight$isMaterialDossierModal();
    }

    @Override
    public void tetraInsight$resetMaterialDossier() {
        ((HoloMaterialDossierLifecycleAccess) materialsView)
                .tetraInsight$resetMaterialDossier();
    }

    @Override
    public void tetraInsight$navigateSchematic(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic
    ) {
        ((HoloCraftRootGui) (Object) this).updateState(item, itemStack, slot, schematic);
    }

    @Override
    public void tetraInsight$navigateImprovement(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic parentSchematic,
            OutcomePreview parentPreview
    ) {
        ((HoloCraftRootGui) (Object) this)
                .updateState(item, itemStack, slot, parentSchematic);
        ((HoloSchematicVariantNavigationAccess) schematicView)
                .tetraInsight$openVariantImprovements(parentPreview);
    }
}
