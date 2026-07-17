package io.github.createdelight.tetrainsight.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiTextures;

public final class WorkbenchMaterialInfoButtonVisual extends GuiElement {
    private final GuiTexture backdrop;
    private final GuiStringOutline label;
    private final BooleanSupplier focused;
    private final BooleanSupplier available;

    public WorkbenchMaterialInfoButtonVisual(
            BooleanSupplier focused,
            BooleanSupplier available
    ) {
        super(0, 0, 46, 15);
        this.focused = focused;
        this.available = available;

        backdrop = new GuiTexture(0, 0, 46, 15, 176, 16, GuiTextures.workbench);
        backdrop.setAttachment(GuiAttachment.middleCenter);
        addChild(backdrop);

        label = new GuiStringOutline(0, 1, "");
        label.setAttachment(GuiAttachment.middleCenter);
        addChild(label);
    }

    public void setText(String text) {
        label.setString(Minecraft.getInstance().font.plainSubstrByWidth(text, 38));
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
        int color;
        if (!available.getAsBoolean()) {
            color = 0x7f7f7f;
        } else if (focused.getAsBoolean()) {
            color = 0xffffcc;
        } else {
            color = 0xffffff;
        }
        backdrop.setColor(color);
        label.setColor(color);
        super.draw(graphics, x, y, width, height, mouseX, mouseY, partialTicks);
    }
}
