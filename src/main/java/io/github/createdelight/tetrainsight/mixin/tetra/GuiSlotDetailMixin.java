package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.PersistentVerticalTabGroupAccess;
import io.github.createdelight.tetrainsight.client.WorkbenchEmptySchematicHoloAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.blocks.workbench.gui.GuiSchematicList;
import se.mickelus.tetra.blocks.workbench.gui.GuiSlotDetail;
import se.mickelus.tetra.gui.VerticalTabGroupGui;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = GuiSlotDetail.class, remap = false)
public abstract class GuiSlotDetailMixin {
    @Shadow
    @Final
    private VerticalTabGroupGui tabGroup;

    @Shadow
    @Final
    private GuiSchematicList schematicList;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void tetraInsight$keepWorkbenchTabLabelsVisible(CallbackInfo ci) {
        ((PersistentVerticalTabGroupAccess) tabGroup).tetraInsight$setPersistentLabels(
                Component.translatable("tetra_insight.workbench.tabs.details_unavailable"),
                null,
                Component.translatable("tetra_insight.workbench.tabs.tweak_unavailable")
        );
    }

    @Inject(method = "onTileEntityChange", at = @At("RETURN"), remap = false)
    private void tetraInsight$forwardHoloSlotContext(
            Player player,
            WorkbenchTile workbench,
            ItemStack itemStack,
            String slot,
            UpgradeSchematic schematic,
            CallbackInfo ci
    ) {
        IModularItem item = itemStack.getItem() instanceof IModularItem modular
                ? modular
                : null;
        ((WorkbenchEmptySchematicHoloAccess) schematicList).tetraInsight$setHoloSlotContext(
                player,
                workbench,
                item,
                itemStack,
                slot
        );
    }
}
