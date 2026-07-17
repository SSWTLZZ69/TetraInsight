package io.github.createdelight.tetrainsight.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiItemRolling;
import se.mickelus.tetra.gui.GuiTextures;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementVariantGui;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Keeps Tetra's text variant by default and overlays the matching consumable
 * when Shift is held.
 */
public class HoloConsumableImprovementVariantGui extends HoloImprovementVariantGui {
    private static final int tetraInsight$MAX_TOOLTIP_MATERIALS = 6;

    private final OutcomePreview tetraInsight$preview;
    private final GuiTexture tetraInsight$materialBackdrop;
    private final GuiItemRolling tetraInsight$material;
    private boolean tetraInsight$muted;

    public HoloConsumableImprovementVariantGui(int x, int y, String label, int labelStart,
            OutcomePreview preview, boolean nextInSeries,
            Consumer<OutcomePreview> onVariantHover,
            Consumer<OutcomePreview> onVariantBlur,
            Consumer<OutcomePreview> onVariantSelect) {
        super(x, y, label, labelStart, preview, nextInSeries,
                onVariantHover, onVariantBlur, onVariantSelect);
        setX(x);
        setWidth(19);
        tetraInsight$preview = preview;
        tetraInsight$materialBackdrop = new GuiTexture(
                0, 0, 17, 11, 0, 0, GuiTextures.holo);
        tetraInsight$material = new GuiItemRolling(-1, -1)
                .setItems(preview.materials);
    }

    @Override
    protected void drawChildren(GuiGraphics graphics, int x, int y,
            int mouseX, int mouseY, int guiLeft, int guiTop, float partialTicks) {
        super.drawChildren(graphics, x, y, mouseX, mouseY, guiLeft, guiTop, partialTicks);
        if (Screen.hasShiftDown()) {
            tetraInsight$materialBackdrop.draw(
                    graphics, x, y, mouseX, mouseY, guiLeft, guiTop, partialTicks);
            tetraInsight$material.draw(graphics,
                    x + tetraInsight$material.getX(),
                    y + tetraInsight$material.getY(),
                    mouseX, mouseY, guiLeft, guiTop, partialTicks);
        }
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        tetraInsight$materialBackdrop.setColor(0xffffcc);
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        tetraInsight$materialBackdrop.setColor(tetraInsight$muted ? 0x7f7f7f : 0xffffff);
    }

    @Override
    public void setMuted(boolean muted) {
        super.setMuted(muted);
        tetraInsight$muted = muted;
        tetraInsight$materialBackdrop.setColor(muted ? 0x7f7f7f : 0xffffff);
    }

    @Override
    public List<Component> getTooltipLines() {
        List<Component> original = super.getTooltipLines();
        if (original == null) {
            return null;
        }

        List<Component> tooltip = new ArrayList<>(original);
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable(
                    "tetra_insight.holo.improvement.hold_shift_consumables")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return tooltip;
        }

        tooltip.add(Component.translatable(
                "tetra_insight.holo.improvement.consumables")
                .withStyle(ChatFormatting.GRAY));
        int shown = Math.min(tetraInsight$preview.materials.length,
                tetraInsight$MAX_TOOLTIP_MATERIALS);
        for (int index = 0; index < shown; index++) {
            ItemStack stack = tetraInsight$preview.materials[index];
            tooltip.add(Component.literal("  ")
                    .append(stack.getHoverName())
                    .append(stack.getCount() > 1 ? " ×" + stack.getCount() : ""));
        }
        if (tetraInsight$preview.materials.length > shown) {
            tooltip.add(Component.translatable(
                    "tetra_insight.holo.improvement.more_consumables",
                    tetraInsight$preview.materials.length - shown)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return tooltip;
    }
}
