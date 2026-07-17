package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.FixedConsumableOutcomeSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.FixedConsumableMaterialResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.tetra.gui.GuiItemRolling;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact fixed-consumable summary for Tetra's improvement title row.
 * A single raw outcome is always visible. Multiple raw outcomes retain the
 * title by default and expand into one rolling item glyph per outcome while
 * Shift is held.
 */
public class HoloFixedConsumableSummaryGui extends GuiElement {
    private static final int ITEM_SPACING = 17;
    private static final int MAX_TOOLTIP_MATERIALS = 6;

    private final List<FixedConsumableOutcomeSnapshot> outcomes;
    private final List<GuiItemRolling> itemGroups;
    private final GuiString shiftHint;
    private final boolean shiftOnly;

    public HoloFixedConsumableSummaryGui(int x, int y,
            List<FixedConsumableOutcomeSnapshot> outcomes) {
        super(x, y, getSummaryWidth(outcomes), 16);
        this.outcomes = List.copyOf(outcomes);
        shiftOnly = outcomes.size() > 1;
        itemGroups = new ArrayList<>();

        for (int index = 0; index < outcomes.size(); index++) {
            GuiItemRolling item = new GuiItemRolling(index * ITEM_SPACING, 0)
                    .setItems(FixedConsumableMaterialResolver.resolve(outcomes.get(index)))
                    .setTooltip(false);
            itemGroups.add(item);
            addChild(item);
        }

        shiftHint = new GuiString(0, 3, "Shift");
        shiftHint.setColor(0x7f7f7f);
        addChild(shiftHint);
        tetraInsight$updateVisibility();
    }

    @Override
    public void updateFocusState(int parentX, int parentY, int mouseX, int mouseY) {
        tetraInsight$updateVisibility();
        super.updateFocusState(parentX, parentY, mouseX, mouseY);
    }

    @Override
    protected void drawChildren(GuiGraphics graphics, int x, int y,
            int mouseX, int mouseY, int guiLeft, int guiTop, float partialTicks) {
        tetraInsight$updateVisibility();
        super.drawChildren(graphics, x, y, mouseX, mouseY,
                guiLeft, guiTop, partialTicks);
    }

    @Override
    public List<Component> getTooltipLines() {
        if (!hasFocus()) {
            return null;
        }
        if (shiftOnly && !Screen.hasShiftDown()) {
            return List.of(Component.translatable(
                    "tetra_insight.holo.improvement.hold_shift_consumables")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        List<ItemStack> materials = outcomes.stream()
                .flatMap(outcome -> java.util.Arrays.stream(
                        FixedConsumableMaterialResolver.resolve(outcome)))
                .toList();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(
                "tetra_insight.holo.improvement.consumables")
                .withStyle(ChatFormatting.GRAY));
        int shown = Math.min(materials.size(), MAX_TOOLTIP_MATERIALS);
        for (int index = 0; index < shown; index++) {
            ItemStack stack = materials.get(index);
            tooltip.add(Component.literal("  ")
                    .append(stack.getHoverName())
                    .append(stack.getCount() > 1 ? " x" + stack.getCount() : ""));
        }
        if (materials.size() > shown) {
            tooltip.add(Component.translatable(
                    "tetra_insight.holo.improvement.more_consumables",
                    materials.size() - shown)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return tooltip;
    }

    private void tetraInsight$updateVisibility() {
        boolean showItems = !shiftOnly || Screen.hasShiftDown();
        for (GuiItemRolling item : itemGroups) {
            item.setVisible(showItems);
        }
        shiftHint.setVisible(shiftOnly && !showItems);
    }

    private static int getSummaryWidth(List<FixedConsumableOutcomeSnapshot> outcomes) {
        return outcomes.size() <= 1
                ? 16
                : Math.max(24, outcomes.size() * ITEM_SPACING - 1);
    }
}
