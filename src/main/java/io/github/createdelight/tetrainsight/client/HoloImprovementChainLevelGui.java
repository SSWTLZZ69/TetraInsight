package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.mixin.tetra.HoloImprovementVariantGuiAccessor;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.ColorHelper;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementVariantGui;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.List;
import java.util.function.Consumer;

public class HoloImprovementChainLevelGui extends HoloImprovementVariantGui {
    private static final int HOVER_COLOR = 0xffffcc;
    private static final int SELECTED_BACKDROP_COLOR = 0x8f8fcf;

    private final int tint;
    private final boolean available;
    private final List<Component> tooltip;
    private boolean groupActive;
    private boolean selected;

    public HoloImprovementChainLevelGui(int x, int y, String level,
            OutcomePreview preview, boolean nextInSeries,
            Consumer<OutcomePreview> onHover,
            Consumer<OutcomePreview> onBlur,
            Consumer<OutcomePreview> onSelect,
            int tint, boolean available, List<Component> tooltip) {
        super(x, y, level, 0, preview, nextInSeries, onHover, onBlur, onSelect);
        setX(x);
        setWidth(19);
        this.tint = tint;
        this.available = available;
        this.tooltip = List.copyOf(tooltip);
        tetraInsight$applyTint(false);
    }

    public void tetraInsight$setSelectionState(boolean groupActive, boolean selected) {
        this.groupActive = groupActive;
        this.selected = selected;
        tetraInsight$applyTint(hasFocus());
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        tetraInsight$applyTint(true);
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        tetraInsight$applyTint(false);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        return super.onMouseClick(mouseX, mouseY, button);
    }

    @Override
    public List<Component> getTooltipLines() {
        return hasFocus() ? tooltip : null;
    }

    private void tetraInsight$applyTint(boolean focused) {
        int backdropColor;
        int labelColor;
        if (focused) {
            backdropColor = HOVER_COLOR;
            labelColor = HOVER_COLOR;
        } else if (selected) {
            backdropColor = SELECTED_BACKDROP_COLOR;
            labelColor = HOVER_COLOR;
        } else {
            backdropColor = tint;
            labelColor = tint;
        }

        if ((!available || (groupActive && !selected)) && !focused) {
            backdropColor = ColorHelper.withBrightness(backdropColor, 0.55);
            labelColor = ColorHelper.withBrightness(labelColor, 0.55);
        }

        HoloImprovementVariantGuiAccessor access =
                (HoloImprovementVariantGuiAccessor) (Object) this;
        access.tetraInsight$getBackdrop().setColor(backdropColor);
        access.tetraInsight$getLabel().setColor(labelColor);
    }
}
