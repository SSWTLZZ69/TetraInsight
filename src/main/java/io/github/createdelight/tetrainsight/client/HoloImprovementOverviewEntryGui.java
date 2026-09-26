package io.github.createdelight.tetrainsight.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.blocks.workbench.gui.GuiModuleGlyph;
import se.mickelus.tetra.gui.GuiTextures;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.OutcomeStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A compact, Tetra-styled link used by the improvement overview. It only
 * answers what the option is and whether it is selected; levels, materials,
 * costs and requirements surface in the tooltip and on the detail page.
 */
public final class HoloImprovementOverviewEntryGui extends GuiClickable {
    private static final int WIDTH = 96;
    private static final int HEIGHT = 12;
    private static final int LABEL_X = 15;
    private static final int SUMMARY_RIGHT = WIDTH - 2;
    private static final int NORMAL_COLOR = 0xffffff;
    private static final int HOVER_COLOR = 0xffffcc;
    private static final int SELECTED_COLOR = 0xffffff;
    private static final int MUTED_COLOR = 0x7f7f7f;

    private final ImprovementDisplayEntry entry;
    private final boolean available;
    private final GuiTexture backdrop;
    private final GuiTexture plus;
    private final GuiString label;
    private final GuiString summary;
    private final Consumer<OutcomeStack> onDeselect;
    private final List<Component> conditions;
    private boolean selected;
    private OutcomeStack selectedOutcome;
    private String selectedSummary = "";

    public HoloImprovementOverviewEntryGui(int x, int y,
            ImprovementDisplayEntry entry, boolean available,
            List<Component> conditions, Runnable onClick,
            Consumer<OutcomeStack> onDeselect) {
        super(x, y, WIDTH, HEIGHT, onClick);
        this.entry = entry;
        this.available = available;
        this.conditions = conditions == null ? List.of() : conditions;
        this.onDeselect = onDeselect;

        backdrop = new GuiTexture(1, 2, 13, 8, 52, 3, GuiTextures.workbench);
        backdrop.setOpacity(0.6f);
        addChild(backdrop);
        addChild(new GuiModuleGlyph(0, 0, 11, 11, entry.schematic().getGlyph())
                .setShift(false));

        plus = new GuiTexture(5, 7, 5, 5, 68, 16, GuiTextures.workbench);
        addChild(plus);

        summary = new GuiString(0, 1, tetraInsight$defaultSummary());
        summary.setColor(MUTED_COLOR);
        summary.setX(SUMMARY_RIGHT - summary.getWidth());
        addChild(summary);

        int labelWidth = Math.max(12, summary.getX() - LABEL_X - 4);
        String title = tetraInsight$title();
        label = new GuiString(LABEL_X, 1,
                Minecraft.getInstance().font.plainSubstrByWidth(title, labelWidth));
        addChild(label);
        tetraInsight$updateStyling();
    }

    public void updateSelection(List<OutcomeStack> selectedOutcomes) {
        selected = false;
        selectedOutcome = null;
        selectedSummary = "";
        if (entry.isChain()) {
            for (ImprovementChainEntry chainEntry : entry.chain()) {
                for (OutcomeStack stack : selectedOutcomes) {
                    if (stack.schematicEquals(chainEntry.schematic())
                            && stack.previewEquals(chainEntry.preview())) {
                        selected = true;
                        selectedOutcome = stack;
                        selectedSummary = tetraInsight$formatLevel(
                                chainEntry.preview().level);
                        break;
                    }
                }
                if (selected) {
                    break;
                }
            }
        } else {
            for (OutcomePreview preview : entry.previews()) {
                for (OutcomeStack stack : selectedOutcomes) {
                    if (stack.schematicEquals(entry.schematic())
                            && stack.previewEquals(preview)) {
                        selected = true;
                        selectedOutcome = stack;
                        selectedSummary = preview.level > 0
                                ? tetraInsight$formatLevel(preview.level)
                                : "";
                        break;
                    }
                }
                if (selected) {
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
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        if (button == 1 && hasFocus()
                && TetraInsightConfig.improvementRightClickDeselect.get()) {
            if (selected && selectedOutcome != null) {
                onDeselect.accept(selectedOutcome);
            }
            return true;
        }
        return super.onMouseClick(mouseX, mouseY, button);
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
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(tetraInsight$title()));
        lines.add(Component.translatable(tetraInsight$typeTranslation())
                .withStyle(ChatFormatting.GRAY));
        if (TetraInsightConfig.improvementOverviewRequirements.get()) {
            lines.addAll(conditions);
        }
        lines.add(Component.translatable(
                        "tetra_insight.holo.improvement.open_detail")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (selected && TetraInsightConfig.improvementRightClickDeselect.get()) {
            lines.add(Component.translatable(
                            "tetra_insight.holo.improvement.deselect")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.copyOf(lines);
    }

    private void tetraInsight$updateStyling() {
        int color = !available ? MUTED_COLOR
                : hasFocus() ? HOVER_COLOR
                : selected ? SELECTED_COLOR
                : NORMAL_COLOR;
        int textColor = hasFocus() ? HOVER_COLOR
                : selected ? SELECTED_COLOR
                : available ? NORMAL_COLOR : MUTED_COLOR;
        backdrop.setColor(color);
        backdrop.setOpacity(selected ? 1.0f : hasFocus() ? 0.8f : 0.6f);
        plus.setColor(selected ? SELECTED_COLOR : MUTED_COLOR);
        label.setColor(textColor);
        summary.setColor(selected ? SELECTED_COLOR : MUTED_COLOR);
    }

    private String tetraInsight$title() {
        if (entry.isChain()) {
            OutcomePreview[] previews = entry.previews();
            ItemStack stack = previews.length > 0 && previews[0].itemStack != null
                    ? previews[0].itemStack
                    : ItemStack.EMPTY;
            return IModularItem.getImprovementName(entry.improvementKey(), 0, stack);
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
        if (entry.schematic().isHoning()
                || entry.chain().stream()
                        .anyMatch(chainEntry -> chainEntry.schematic().isHoning())) {
            return "tetra_insight.holo.improvement.type.honing";
        }
        if (Objects.equals(entry.schematic().getKey(), "book_enchant")) {
            return "tetra_insight.holo.improvement.type.enchantment";
        }
        if (java.util.Arrays.stream(entry.previews())
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
