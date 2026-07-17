package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.mixin.minecraft.SlotAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.ToggleableSlot;
import se.mickelus.tetra.blocks.workbench.WorkbenchContainer;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = WorkbenchContainer.class, remap = false)
public abstract class WorkbenchContainerMixin {
    private static final int tetraInsight$singleMaterialOffset = -4;

    @Shadow
    @Final
    private WorkbenchTile workbench;

    @Shadow
    private ToggleableSlot[] materialSlots;

    @Inject(method = "updateSlots", at = @At("RETURN"), remap = false)
    private void tetraInsight$alignSingleMaterialSlot(CallbackInfo ci) {
        UpgradeSchematic schematic = workbench.getCurrentSchematic();
        if (schematic == null || schematic.getNumMaterialSlots() != 1
                || materialSlots.length == 0) {
            return;
        }

        ToggleableSlot materialSlot = materialSlots[0];
        ((SlotAccessor) (Object) materialSlot).tetraInsight$setX(
                materialSlot.x + tetraInsight$singleMaterialOffset
        );
    }
}
