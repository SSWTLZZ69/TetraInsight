package io.github.createdelight.tetrainsight.client;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiElement;

public final class TabUnavailableReasonGui extends GuiElement {
    private final BooleanSupplier focused;
    private final BooleanSupplier available;
    private final Component reason;

    public TabUnavailableReasonGui(
            int width,
            BooleanSupplier focused,
            BooleanSupplier available,
            Component reason
    ) {
        super(0, 0, width, 15);
        this.focused = focused;
        this.available = available;
        this.reason = reason;
    }

    @Override
    public List<Component> getTooltipLines() {
        if (focused.getAsBoolean() && !available.getAsBoolean()) {
            return List.of(reason);
        }
        return null;
    }
}
