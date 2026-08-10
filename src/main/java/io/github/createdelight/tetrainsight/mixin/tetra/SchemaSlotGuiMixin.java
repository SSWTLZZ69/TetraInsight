package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.WorkbenchMaterialInfoAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiItemRolling;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.blocks.workbench.gui.SchemaSlotGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloMaterialApplicable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloMaterialTranslationGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = SchemaSlotGui.class, remap = false)
public abstract class SchemaSlotGuiMixin {
    @Shadow
    private GuiString quantity;

    @Shadow
    private GuiItemRolling placeholder;

    @Shadow
    private GuiElement placeholderBorder;

    @Shadow
    private GuiTexture border;

    @Shadow
    private GuiString label;

    @Shadow
    private HoloMaterialTranslationGui materialTranslation;

    @Shadow
    private HoloMaterialApplicable applicableMaterials;

    @Shadow
    @Final
    private int index;

    @Shadow
    @Final
    private int fullWidth;

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$makeSingleMaterialSlotDiscoverable(
            UpgradeSchematic schematic,
            Player player,
            Level level,
            BlockPos pos,
            WorkbenchTile workbench,
            ItemStack targetStack,
            String slot,
            ItemStack[] materials,
            CallbackInfo ci
    ) {
        WorkbenchMaterialInfoAccess entry = (WorkbenchMaterialInfoAccess) applicableMaterials;
        if (index == 0 && schematic.getNumMaterialSlots() == 1) {
            String slotName = schematic.getSlotName(targetStack, index);
            label.setVisible(false);
            materialTranslation.setVisible(false);
            tetraInsight$positionSingleMaterialContent();
            entry.tetraInsight$showAsRowLink(slotName, materialTranslation, fullWidth - 4);
        } else {
            tetraInsight$restoreMaterialContentPosition();
            entry.tetraInsight$restoreCompactIcon();
        }
    }

    private void tetraInsight$positionSingleMaterialContent() {
        placeholder.setX(6);
        placeholderBorder.setX(-4);
        border.setX(6);
        quantity.setX(23);
    }

    private void tetraInsight$restoreMaterialContentPosition() {
        placeholder.setX(10);
        placeholderBorder.setX(0);
        border.setX(10);
        quantity.setX(27);
    }
}
