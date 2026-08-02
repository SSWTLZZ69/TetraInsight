package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloCraftSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloHoningTargetAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloCraftRootGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloBreadcrumbsGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSchematicGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSchematicListGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = HoloCraftRootGui.class, remap = false)
public abstract class HoloCraftRootGuiMixin implements HoloCraftSlotNavigationAccess {
    @Shadow
    @Final
    private HoloSchematicGui schematicView;

    @Shadow
    @Final
    private HoloBreadcrumbsGui breadcrumbs;

    @Shadow
    @Final
    private HoloSchematicListGui schematicsView;

    @Shadow
    private IModularItem item;

    @Shadow
    private ItemStack itemStack;

    @Shadow
    private void onSlotSelect(String slot) {
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
        schematicsView.animateOpen();
        breadcrumbs.animateOpen(true);
    }
}
