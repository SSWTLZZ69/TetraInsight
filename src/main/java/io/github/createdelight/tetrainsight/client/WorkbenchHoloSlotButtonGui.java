package io.github.createdelight.tetrainsight.client;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.tetra.blocks.workbench.gui.GuiButtonOutlined;

public final class WorkbenchHoloSlotButtonGui extends GuiButtonOutlined {
    private final BooleanSupplier available;

    public WorkbenchHoloSlotButtonGui(
            Runnable onClick,
            BooleanSupplier available
    ) {
        super(0, 7, I18n.get("tetra_insight.workbench.schematic_list_holo.button"), onClick);
        this.available = available;
        setAttachment(GuiAttachment.middleCenter);
        setX(-getWidth() / 2);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        return available.getAsBoolean() && super.onMouseClick(mouseX, mouseY, button);
    }

    @Override
    public List<Component> getTooltipLines() {
        if (!hasFocus()) {
            return null;
        }

        boolean enabled = available.getAsBoolean();
        return List.of(Component.translatable(enabled
                        ? "tetra_insight.workbench.schematic_list_holo.open"
                        : "tetra_insight.workbench.schematic_list_holo.unavailable")
                .withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
    }

    @Override
    public void draw(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        setOpacity(available.getAsBoolean() ? 1.0f : 0.45f);
        super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTicks);
    }
}
