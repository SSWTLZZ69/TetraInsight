package io.github.createdelight.tetrainsight.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.blocks.workbench.gui.GuiModuleGlyph;
import se.mickelus.tetra.gui.GuiTextures;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.OutcomeStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.List;
import java.util.Objects;

/**
 * A compact, Tetra-styled link used by the improvement overview. It only
 * answers what the option is and whether it is selected; levels, materials,
 * costs and requirements remain on the detail page.
 */
public final class HoloImprovementOverviewEntryGui extends GuiClickable {
    private static final int WIDTH = 96;
    private static final int LABEL_X = 24;
    private static final int SUMMARY_RIGHT = WIDTH - 2;
    private static final int NORMAL_COLOR = 0xffffff;
    private static final int HOVER_COLOR = 0xffffcc;
    private static final int SELECTED_COLOR = 0x8f8fcf;
    private static final int MUTED_COLOR = 0x7f7f7f;

    private final ImprovementDisplayEntry entry;
    private final boolean available;
    private final GuiTexture backdrop;
    private final GuiTexture plus;
    private final GuiString label;
    private final GuiString summary;
    private boolean selected;
    private String selectedSummary = "";

    public HoloImprovementOverviewEntryGui(int x, int y,
            ImprovementDisplayEntry entry, boolean available, Runnable onClick) {
        super(x, y, WIDTH, 16, onClick);
        this.entry = entry;
        this.available = available;

        backdrop = new GuiTexture(1, 5, 16, 9, 52, 3, GuiTextures.workbench);
        addChild(backdrop);
        addChild(new GuiModuleGlyph(0, 2, 16, 16, entry.schematic().getGlyph())
                .setShift(false));

        plus = new GuiTexture(7, 10, 7, 7, 68, 16, GuiTextures.workbench);
        addChild(plus);

        summary = new GuiString(0, 0, tetraInsight$defaultSummary());
        summary.setColor(MUTED_COLOR);
        summary.setX(SUMMARY_RIGHT - summary.getWidth());
        addChild(summary);

        int labelWidth = Math.max(12, summary.getX() - LABEL_X - 4);
        String title = tetraInsight$title();
        label = new GuiString(LABEL_X, 0,
                Minecraft.getInstance().font.plainSubstrByWidth(title, labelWidth));
        addChild(label);
        tetraInsight$updateStyling();
    }

    public void updateSelection(List<OutcomeStack> selectedOutcomes) {
        selected = false;
        selectedSummary = "";
        if (entry.isChain()) {
            for (ImprovementChainEntry chainEntry : entry.chain()) {
                if (selectedOutcomes.stream().anyMatch(stack ->
                        stack.schematicEquals(chainEntry.schematic())
                                && stack.previewEquals(chainEntry.preview()))) {
                    selected = true;
                    selectedSummary = tetraInsight$formatLevel(chainEntry.preview().level);
                    break;
                }
            }
        } else {
            for (OutcomePreview preview : entry.previews()) {
                if (selectedOutcomes.stream().anyMatch(stack ->
                        stack.schematicEquals(entry.schematic())
                                && stack.previewEquals(preview))) {
                    selected = true;
                    selectedSummary = preview.level > 0
                            ? tetraInsight$formatLevel(preview.level)
                            : "";
                    break;
                }
            }
        }

        String value = selectedSummary.isEmpty()
                ? tetraInsight$defaultSummary()
                : selectedSummary;
        summary.setString(value);
        summary.setX(SUMMARY_RIGHT - summary.getWidth());
        tetraInsight$updateStyling();
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        tetraInsight$updateStyling();
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        tetraInsight$updateStyling();
    }

    @Override
    public List<Component> getTooltipLines() {
        if (!hasFocus()) {
            return null;
        }
        Component type = Component.translatable(tetraInsight$typeTranslation())
                .withStyle(ChatFormatting.GRAY);
        Component action = Component.translatable(
                        "tetra_insight.holo.improvement.open_detail")
                .withStyle(ChatFormatting.DARK_GRAY);
        return List.of(Component.literal(tetraInsight$title()), type, action);
    }

    private void tetraInsight$updateStyling() {
        int color = !available ? MUTED_COLOR
                : hasFocus() ? HOVER_COLOR
                : selected ? SELECTED_COLOR
                : NORMAL_COLOR;
        int textColor = hasFocus() ? HOVER_COLOR
                : selected ? HOVER_COLOR
                : color;
        backdrop.setColor(color);
        plus.setColor(selected ? HOVER_COLOR : MUTED_COLOR);
        label.setColor(textColor);
        summary.setColor(selected ? HOVER_COLOR : MUTED_COLOR);
    }

    private String tetraInsight$title() {
        if (entry.isChain()) {
            return IModularItem.getImprovementName(entry.improvementKey(), 0);
        }
        return entry.schematic().getName();
    }

    private String tetraInsight$defaultSummary() {
        OutcomePreview[] previews = entry.previews();
        if (entry.isChain()) {
            int maximum = entry.chain().stream()
                    .map(ImprovementChainEntry::preview)
                    .mapToInt(preview -> preview.level)
                    .max()
                    .orElse(0);
            return maximum > 0 ? tetraInsight$formatLevel(maximum) : "";
        }
        if (previews.length == 1 && previews[0].level > 0) {
            return tetraInsight$formatLevel(previews[0].level);
        }
        return Integer.toString(previews.length);
    }

    private String tetraInsight$typeTranslation() {
        if (entry.isChain() || entry.schematic().isHoning()) {
            return "tetra_insight.holo.improvement.type.honing";
        }
        if (Objects.equals(entry.schematic().getKey(), "book_enchant")) {
            return "tetra_insight.holo.improvement.type.enchantment";
        }
        if (entry.previews().length > 0
                && java.util.Arrays.stream(entry.previews())
                        .anyMatch(preview -> preview.materials != null
                                && preview.materials.length > 0)) {
            return "tetra_insight.holo.improvement.type.material";
        }
        return "tetra_insight.holo.improvement.type.improvement";
    }

    private static String tetraInsight$formatLevel(int level) {
        if (level >= 1 && level <= 10) {
            return Component.translatable("enchantment.level." + level).getString();
        }
        return Integer.toString(level);
    }
}
