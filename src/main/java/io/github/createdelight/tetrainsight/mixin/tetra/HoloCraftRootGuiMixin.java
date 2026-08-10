package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloCraftSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloCraftWorkingStackAccess;
import io.github.createdelight.tetrainsight.client.HoloCraftMaterialNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloHoningTargetAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialSelectionAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloSpecialMaterialDossierAccess;
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
import se.mickelus.tetra.items.modular.impl.dynamic.DynamicModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloCraftRootGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HolosphereCraftState;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HolosphereEntryStore;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSchematicGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.Map;

@Mixin(value = HoloCraftRootGui.class, remap = false)
public abstract class HoloCraftRootGuiMixin
        implements HoloCraftSlotNavigationAccess, HoloCraftMaterialNavigationAccess,
        HoloMaterialDossierModalAccess, HoloMaterialDossierLifecycleAccess,
        HoloUsageNavigationAccess, HoloCraftWorkingStackAccess {
    @Shadow
    @Final
    private HoloSchematicGui schematicView;

    @Shadow
    @Final
    private HoloMaterialListGui materialsView;

    @Shadow
    @Final
    private HolosphereCraftState state;

    @Inject(method = "openFromWorkbench", at = @At("RETURN"), remap = false)
    private void tetraInsight$restoreSlotNavigation(IModularItem item, ItemStack itemStack,
            String slot, UpgradeSchematic schematic, CallbackInfo ci) {
        if (slot != null && schematic == null) {
            tetraInsight$openFromWorkbenchState(item, itemStack, slot, null);
        }
    }

    @Inject(method = "openFromWorkbench", at = @At("HEAD"), remap = false)
    private void tetraInsight$forwardHoningTarget(IModularItem item, ItemStack itemStack,
            String slot, UpgradeSchematic schematic, CallbackInfo ci) {
        tetraInsight$prepareWorkingStack(item, itemStack);
        ((HoloHoningTargetAccess) schematicView).tetraInsight$setHoningTarget(itemStack);
    }

    @Inject(method = "onItemSelect", at = @At("HEAD"), remap = false)
    private void tetraInsight$resetWorkingStackOnItemSelection(
            String key,
            CallbackInfo ci
    ) {
        if (key != null) {
            tetraInsight$resetWorkingStack(key);
        }
    }

    @Override
    public void tetraInsight$openSlot(
            IModularItem item,
            ItemStack itemStack,
            String slot
    ) {
        ((HoloCraftRootGui) (Object) this)
                .openFromWorkbench(item, itemStack, slot, null);
    }

    @Override
    public void tetraInsight$openMaterial(String materialKey) {
        state.onMaterialsSelect();
        ((HoloMaterialSelectionAccess) materialsView)
                .tetraInsight$selectMaterial(materialKey);
    }

    @Override
    public void tetraInsight$openSpecialMaterial(ItemStack stack) {
        state.onMaterialsSelect();
        ((HoloSpecialMaterialDossierAccess) materialsView)
                .tetraInsight$openSpecialMaterial(stack);
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
    public void tetraInsight$resetWorkingStacks() {
        state.getItemState().keySet().forEach(this::tetraInsight$resetWorkingStack);
    }

    @Override
    public void tetraInsight$navigateSchematic(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic
    ) {
        ((HoloCraftRootGui) (Object) this)
                .openFromWorkbench(item, itemStack, slot, schematic);
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
                .openFromWorkbench(item, itemStack, slot, parentSchematic);
        ((HoloSchematicVariantNavigationAccess) schematicView)
                .tetraInsight$openVariantImprovements(parentPreview);
    }

    private void tetraInsight$prepareWorkingStack(IModularItem item, ItemStack itemStack) {
        String key = tetraInsight$findHolosphereKey(item, itemStack);
        if (key == null) {
            return;
        }

        HolosphereCraftState.ItemState itemState = state.getItemState().get(key);
        if (itemState != null) {
            itemState.setWorkingStack(itemStack.copy());
        }
    }

    private void tetraInsight$openFromWorkbenchState(IModularItem item, ItemStack itemStack,
            String slot, UpgradeSchematic schematic) {
        String key = tetraInsight$findHolosphereKey(item, itemStack);
        if (key == null || state.getItemState().get(key) == null) {
            state.onItemSelect(null);
            return;
        }

        state.getItemState().get(key).setWorkingStack(itemStack.copy());
        state.openFromWorkbench(key, itemStack, slot, schematic);
    }

    private void tetraInsight$resetWorkingStack(String key) {
        HolosphereCraftState.ItemState itemState = state.getItemState().get(key);
        if (itemState == null) {
            return;
        }

        ItemStack defaultStack = itemState.itemData().getDefaultStack();
        itemState.setWorkingStack(defaultStack == null
                ? ItemStack.EMPTY
                : defaultStack.copy());
    }

    private static String tetraInsight$findHolosphereKey(IModularItem item, ItemStack itemStack) {
        return HolosphereEntryStore.instance.getEntries().entrySet().stream()
                .filter(entry -> entry.getValue().item.equals(item))
                .filter(entry -> entry.getValue().archetype == null
                        || entry.getValue().archetype.equals(DynamicModularItem.getArchetypeKey(itemStack)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
