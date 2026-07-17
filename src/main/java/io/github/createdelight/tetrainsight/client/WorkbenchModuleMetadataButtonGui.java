package io.github.createdelight.tetrainsight.client;

import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiStringOutline;

public final class WorkbenchModuleMetadataButtonGui extends GuiClickable {
    private static final int ICON_WIDTH = 11;

    private final GuiStringOutline label;
    private final IntConsumer iconColor;
    private List<Component> tooltip = List.of();
    private int baseColor = 0x7f7f7f;
    private boolean active;

    public WorkbenchModuleMetadataButtonGui(
            int x,
            int y,
            int width,
            Runnable onClick,
            IntConsumer iconColor
    ) {
        super(x, y, width, 10, onClick);
        this.iconColor = iconColor;
        label = new GuiStringOutline(ICON_WIDTH, 0, "");
        addChild(label);
        updateStyling();
    }

    public void update(String text, int color, List<Component> tooltip) {
        String clipped = Minecraft.getInstance().font.plainSubstrByWidth(
                text,
                Math.max(0, getWidth() - ICON_WIDTH)
        );
        label.setString(clipped);
        baseColor = color;
        this.tooltip = List.copyOf(tooltip);
        updateStyling();
    }

    public void setActive(boolean active) {
        this.active = active;
        updateStyling();
    }

    @Override
    protected void onFocus() {
        updateStyling();
    }

    @Override
    protected void onBlur() {
        updateStyling();
    }

    @Override
    public List<Component> getTooltipLines() {
        return hasFocus() ? tooltip : null;
    }

    private void updateStyling() {
        int color = hasFocus() ? 0xffffcc : active ? 0xffffff : baseColor;
        label.setColor(color);
        iconColor.accept(color);
    }
}
