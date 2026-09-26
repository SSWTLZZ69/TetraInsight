package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.TetraInsightConfig;
import io.github.createdelight.tetrainsight.client.WorkbenchSchematicListContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.blocks.workbench.gui.GuiSchematicListItem;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

/**
 * Adds crafting materials, tool levels and experience cost to schematic list
 * entries, replacing the otherwise uninformative hover state.
 */
@Mixin(value = GuiSchematicListItem.class, remap = false)
public abstract class GuiSchematicListItemMixin {
    @Unique
    private UpgradeSchematic tetraInsight$schematic;

    @Inject(method = "<init>(IILse/mickelus/tetra/module/schematic/UpgradeSchematic;Ljava/lang/Runnable;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void tetraInsight$captureSchematic4(int x, int y,
            UpgradeSchematic schematic, Runnable onClick, CallbackInfo ci) {
        tetraInsight$schematic = schematic;
    }

    @Inject(method = "<init>(IIILse/mickelus/tetra/module/schematic/UpgradeSchematic;Ljava/lang/Runnable;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private void tetraInsight$captureSchematic(int x, int y, int width,
            UpgradeSchematic schematic, Runnable onClick, CallbackInfo ci) {
        tetraInsight$schematic = schematic;
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true,
            remap = false)
    private void tetraInsight$addCraftingInfo(
            CallbackInfoReturnable<List<Component>> cir) {
        if (tetraInsight$schematic == null
                || !TetraInsightConfig.workbenchSchematicDetails.get()
                || !WorkbenchSchematicListContext.present()) {
            return;
        }
        ItemStack itemStack = WorkbenchSchematicListContext.itemStack();
        String slot = WorkbenchSchematicListContext.slot();
        List<Component> lines = new ArrayList<>();
        if (cir.getReturnValue() != null) {
            lines.addAll(cir.getReturnValue());
        }

        int materialSlots = tetraInsight$schematic.getNumMaterialSlots();
        for (int index = 0; index < materialSlots; index++) {
            try {
                ItemStack[] placeholders = tetraInsight$schematic
                        .getSlotPlaceholders(itemStack, index);
                int quantity = tetraInsight$schematic.getRequiredQuantity(
                        itemStack, index, null);
                String slotName = tetraInsight$schematic.getSlotName(
                        itemStack, index);
                if (placeholders != null && placeholders.length > 0) {
                    List<String> materials = new ArrayList<>();
                    for (ItemStack placeholder : placeholders) {
                        if (placeholder == null || placeholder.isEmpty()) {
                            continue;
                        }
                        materials.add(placeholder.getHoverName().getString());
                        if (materials.size() >= 3) {
                            break;
                        }
                    }
                    if (!materials.isEmpty()) {
                        String suffix = quantity > 1 ? " ×" + quantity : "";
                        lines.add(Component.literal(
                                        slotName + ": "
                                                + String.join(" / ", materials)
                                                + suffix)
                                .withStyle(ChatFormatting.GRAY));
                    }
                } else if (slotName != null && !slotName.isBlank()) {
                    lines.add(Component.literal(slotName)
                            .withStyle(ChatFormatting.GRAY));
                }
            } catch (RuntimeException ignored) {
                // custom schematics may need a completed selection first
            }
        }

        try {
            Map<ToolAction, Integer> tools = tetraInsight$schematic
                    .getRequiredToolLevels(itemStack, new ItemStack[0]);
            tools.forEach((tool, level) -> {
                if (level > 0) {
                    lines.add(Component.translatable(
                                    "tetra.tool." + tool.name())
                            .append(Component.literal(" ≥ " + level))
                            .withStyle(ChatFormatting.GRAY));
                }
            });
            int experience = tetraInsight$schematic.getExperienceCost(
                    itemStack, new ItemStack[0], slot);
            if (experience > 0) {
                lines.add(Component.translatable(
                                "tetra_insight.holo.improvement.experience_cost_max",
                                experience)
                        .withStyle(ChatFormatting.GRAY));
            }
        } catch (RuntimeException ignored) {
        }

        cir.setReturnValue(List.copyOf(lines));
    }
}
