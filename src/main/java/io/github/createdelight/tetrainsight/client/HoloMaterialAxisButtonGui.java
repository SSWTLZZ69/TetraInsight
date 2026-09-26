package io.github.createdelight.tetrainsight.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiStringSmall;
import se.mickelus.tetra.module.data.MaterialData;

/**
 * Compact text button that toggles sorting of holosphere material groups by
 * one material axis (hardness / density / flexibility). Left click toggles
 * the axis in descending order, right click selects ascending order. The
 * direction indicator is a separate small element at a fixed offset so the
 * axis label never shifts when the sort engages or changes direction.
 */
public final class HoloMaterialAxisButtonGui extends GuiClickable {
    private static final int ARROW_OFFSET = 6;

    public enum Axis {
        PRIMARY("tetra_insight.holo.material_sort.primary",
                material -> material.primary),
        SECONDARY("tetra_insight.holo.material_sort.secondary",
                material -> material.secondary),
        TERTIARY("tetra_insight.holo.material_sort.tertiary",
                material -> material.tertiary);

        private final String translationKey;
        private final java.util.function.Function<MaterialData, Float> value;

        Axis(String translationKey,
                java.util.function.Function<MaterialData, Float> value) {
            this.translationKey = translationKey;
            this.value = value;
        }

        public String label() {
            return Component.translatable(translationKey).getString();
        }

        public Float valueOf(MaterialData material) {
            return value.apply(material);
        }

        /**
         * Descending ordering, placing materials without a value at the end.
         */
        public java.util.Comparator<MaterialData> descending() {
            return java.util.Comparator.comparing(
                    this::valueOf,
                    java.util.Comparator.nullsLast(
                            java.util.Comparator.<Float>naturalOrder()
                                    .reversed()));
        }

        /**
         * Ascending ordering, placing materials without a value at the end.
         */
        public java.util.Comparator<MaterialData> ascending() {
            return java.util.Comparator.comparing(
                    this::valueOf,
                    java.util.Comparator.nullsLast(Float::compareTo));
        }
    }

    private final Axis axis;
    private final GuiString label;
    private final GuiStringSmall arrow;
    private final java.util.function.Consumer<Axis> onSort;
    private boolean active;
    private boolean ascending;

    public HoloMaterialAxisButtonGui(int x, int y, Axis axis,
            java.util.function.Consumer<Axis> onSort) {
        super(x, y, 11, 11, null);
        this.axis = axis;
        this.onSort = onSort;
        label = new GuiString(0, 1, axis.label());
        label.setColor(0x7f7f7f);
        addChild(label);
        // fixed slot at the right edge so toggling never shifts the label
        arrow = new GuiStringSmall(label.getWidth() + 2, 0, "");
        arrow.setColor(0xffffff);
        addChild(arrow);
        setWidth(label.getWidth() + ARROW_OFFSET);
    }

    public Axis axis() {
        return axis;
    }

    public void updateState(boolean active, boolean ascending) {
        this.active = active;
        this.ascending = ascending;
        arrow.setString(active ? (ascending ? "^" : "v") : "");
        int color = active ? 0xffffff : hasFocus() ? 0xffffcc : 0x7f7f7f;
        label.setColor(color);
        arrow.setColor(color);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        // left click cycles descending -> ascending -> off; right clicks are
        // not reliably routed by the holo screen, so they are not used
        if (hasFocus() && button == 0) {
            onSort.accept(axis);
            return true;
        }
        return false;
    }

    @Override
    protected void onFocus() {
        updateState(active, ascending);
    }

    @Override
    protected void onBlur() {
        updateState(active, ascending);
    }

    @Override
    public List<Component> getTooltipLines() {
        if (!hasFocus()) {
            return null;
        }
        return List.of(
                Component.translatable(
                        "tetra_insight.holo.material_sort.tooltip",
                        axis.label()).withStyle(ChatFormatting.GRAY),
                Component.translatable(
                                "tetra_insight.holo.material_sort.hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }
}
