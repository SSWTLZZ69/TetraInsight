package io.github.createdelight.tetrainsight.mixin.tetra;

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
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSchematicGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = HoloCraftRootGui.class, remap = false)
public abstract class HoloCraftRootGuiMixin {
    @Shadow
    @Final
    private HoloSchematicGui schematicView;

    @Inject(method = "updateState", at = @At("HEAD"), remap = false)
    private void tetraInsight$forwardHoningTarget(IModularItem item, ItemStack itemStack,
            String slot, UpgradeSchematic schematic, CallbackInfo ci) {
        ((HoloHoningTargetAccess) schematicView).tetraInsight$setHoningTarget(itemStack);
    }
}
