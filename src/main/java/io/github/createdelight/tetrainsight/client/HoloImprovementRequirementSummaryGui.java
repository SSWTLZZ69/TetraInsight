package io.github.createdelight.tetrainsight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.tetra.blocks.workbench.gui.GuiExperience;
import se.mickelus.tetra.blocks.workbench.gui.GuiTool;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact requirement row assembled from Tetra's own tool and experience
 * widgets. Values are the maximum across the currently displayed variants,
 * so collapsed alternatives never hide a stricter requirement.
 */
public class HoloImprovementRequirementSummaryGui extends GuiElement {
    private static final int ENTRY_SPACING = 18;

    private final UpgradeSchematic schematic;
    private final String slot;

    public HoloImprovementRequirementSummaryGui(int x, int y,
            UpgradeSchematic schematic, ItemStack itemStack, String slot,
            OutcomePreview[] previews) {
        super(x, y, 0, 16);
        this.schematic = schematic;
        this.slot = slot;
        update(itemStack, previews);
    }

    public void update(ItemStack itemStack, OutcomePreview[] previews) {
        clearChildren();

        Map<ToolAction, Integer> requiredTools = new LinkedHashMap<>();
        int experienceCost = 0;
        for (OutcomePreview preview : previews) {
            ItemStack[] materials = preview.materials != null
                    ? preview.materials
                    : new ItemStack[0];
            try {
                schematic.getRequiredToolLevels(itemStack, materials)
                        .forEach((tool, level) -> requiredTools.merge(
                                tool, level, Math::max));
                experienceCost = Math.max(experienceCost,
                        schematic.getExperienceCost(itemStack, materials, slot));
            } catch (RuntimeException ignored) {
                // Some custom Java schematics require a completed material
                // selection before exposing costs. Other requirements remain
                // visible and the summary updates again when selection changes.
            }
        }

        int cursor = 0;
        for (Map.Entry<ToolAction, Integer> entry : requiredTools.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            GuiTool tool = new GuiTool(cursor, -2, entry.getKey());
            tool.update(entry.getValue(), 0xffffff);
            addChild(tool);
            cursor += ENTRY_SPACING;
        }

        if (experienceCost > 0) {
            boolean available = Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.experienceLevel >= experienceCost;
            GuiExperience experience = new GuiExperience(
                    cursor, -2,
                    "tetra_insight.holo.improvement.experience_cost_max");
            experience.update(experienceCost, available);
            addChild(experience);
            cursor += ENTRY_SPACING;
        }

        setWidth(Math.max(0, cursor - (cursor > 0 ? 2 : 0)));
        setVisible(cursor > 0);
    }
}
