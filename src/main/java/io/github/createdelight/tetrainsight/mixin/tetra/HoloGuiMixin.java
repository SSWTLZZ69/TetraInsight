package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloCraftSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloCraftWorkingStackAccess;
import io.github.createdelight.tetrainsight.client.HoloCraftMaterialNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloUsageNavigationAccess;
import io.github.createdelight.tetrainsight.client.MaterialDossierSession;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.HoloPage;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloRootBaseGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloCraftRootGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;

@Mixin(value = HoloGui.class, remap = false)
public abstract class HoloGuiMixin
        implements HoloSlotNavigationAccess, HoloMaterialNavigationAccess, HoloUsageNavigationAccess,
        HoloMaterialDossierModalAccess, HoloMaterialDossierLifecycleAccess {
    @Shadow
    @Final
    private HoloRootBaseGui[] pages;

    @Shadow
    private Runnable closeCallback;

    @Shadow
    private void changePage(HoloPage page) {
    }

    @Override
    public void tetraInsight$openSlot(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            Runnable closeCallback
    ) {
        changePage(HoloPage.craft);
        ((HoloCraftSlotNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$openSlot(item, itemStack, slot);
        this.closeCallback = closeCallback;
    }

    @Override
    public void tetraInsight$openMaterial(String materialKey, Runnable closeCallback) {
        changePage(HoloPage.craft);
        ((HoloCraftMaterialNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$openMaterial(materialKey);
        this.closeCallback = closeCallback;
    }

    @Override
    public void tetraInsight$openSpecialMaterial(ItemStack stack, Runnable closeCallback) {
        changePage(HoloPage.craft);
        ((HoloCraftMaterialNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$openSpecialMaterial(stack);
        this.closeCallback = closeCallback;
    }

    @Override
    public void tetraInsight$navigateMaterial(String materialKey) {
        changePage(HoloPage.craft);
        ((HoloCraftMaterialNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$openMaterial(materialKey);
    }

    @Override
    public void tetraInsight$navigateSchematic(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic
    ) {
        changePage(HoloPage.craft);
        ((HoloUsageNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$navigateSchematic(item, itemStack, slot, schematic);
    }

    @Override
    public void tetraInsight$navigateImprovement(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic parentSchematic,
            OutcomePreview parentPreview
    ) {
        changePage(HoloPage.craft);
        ((HoloUsageNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$navigateImprovement(
                        item, itemStack, slot, parentSchematic, parentPreview);
    }

    @Override
    public void tetraInsight$setMaterialDossierModal(boolean open) {
        ((HoloMaterialDossierModalAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$setMaterialDossierModal(open);
    }

    @Override
    public boolean tetraInsight$isMaterialDossierModal() {
        return ((HoloMaterialDossierModalAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$isMaterialDossierModal();
    }

    @Override
    public void tetraInsight$resetMaterialDossier() {
        ((HoloMaterialDossierLifecycleAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$resetMaterialDossier();
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "removed",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            remap = true)
    private void tetraInsight$clearMaterialDossierSession(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ((HoloCraftWorkingStackAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$resetWorkingStacks();
        tetraInsight$resetMaterialDossier();
        MaterialDossierSession.clear();
    }
}
