package io.github.createdelight.tetrainsight.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.mutil.gui.GuiStringSmall;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiTextures;

import java.util.List;

/** Tetra-styled controls for expanding and collapsing a holo option group. */
public final class HoloVariantGroupFoldButtonGui extends GuiClickable {
    private static final int NORMAL_COLOR = 0xffffff;
    private static final int HOVER_COLOR = 0xffffcc;

    private final boolean collapseLink;
    private final GuiTexture backdrop;
    private final GuiString slotLabel;
    private final GuiStringOutline linkLabel;
    private int hiddenCount;

    private HoloVariantGroupFoldButtonGui(
            GuiElement reservedSlot, Runnable onClick) {
        super(0, 0,
                Math.max(1, reservedSlot.getWidth()),
                Math.max(1, reservedSlot.getHeight()),
                onClick);
        collapseLink = false;
        SlotVisual visual = SlotVisual.from(reservedSlot);
        backdrop = new GuiTexture(
                visual.offsetX(), visual.offsetY(),
                visual.width(), visual.height(),
                visual.textureX(), visual.textureY(),
                GuiTextures.workbench);
        slotLabel = visual.width() <= 11
                ? new GuiStringSmall(0, 0, "+0")
                : new GuiStringScaledOutline(0, 0, "+0");
        linkLabel = null;
        addChild(backdrop);
        addChild(slotLabel);
        updateStyling();
    }

    private HoloVariantGroupFoldButtonGui(Runnable onClick) {
        super(0, 0, 1, 7, onClick);
        collapseLink = true;
        backdrop = null;
        slotLabel = null;
        linkLabel = new GuiStringOutline(0, 0,
                I18n.get("tetra_insight.holo.material_group.collapse_short"));
        addChild(linkLabel);
        setWidth(linkLabel.getWidth());
        updateStyling();
    }

    public static HoloVariantGroupFoldButtonGui expandSlot(
            GuiElement reservedSlot, Runnable onClick) {
        return new HoloVariantGroupFoldButtonGui(reservedSlot, onClick);
    }

    public static HoloVariantGroupFoldButtonGui collapseLink(
            Runnable onClick) {
        return new HoloVariantGroupFoldButtonGui(onClick);
    }

    public void placeExpandSlot(int x, int y, int hiddenCount) {
        this.hiddenCount = Math.max(0, hiddenCount);
        setX(x);
        setY(y);
        slotLabel.setString("+" + this.hiddenCount);
        slotLabel.setX((getWidth() - slotLabel.getWidth()) / 2);
        slotLabel.setY((getHeight() - slotLabel.getHeight()) / 2);
        setVisible(true);
        updateStyling();
    }

    public void placeCollapseLink(int x, int y) {
        setX(x);
        setY(y);
        setVisible(true);
        updateStyling();
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        updateStyling();
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        updateStyling();
    }

    @Override
    public List<Component> getTooltipLines() {
        if (!hasFocus()) {
            return null;
        }
        Component tooltip = collapseLink
                ? Component.translatable(
                        "tetra_insight.holo.material_group.collapse")
                : Component.translatable(
                        "tetra_insight.holo.material_group.more", hiddenCount);
        return List.of(tooltip.copy().withStyle(ChatFormatting.GRAY));
    }

    private void updateStyling() {
        if (collapseLink) {
            linkLabel.setColor(hasFocus() ? HOVER_COLOR : NORMAL_COLOR);
            return;
        }
        backdrop.setColor(hasFocus() ? HOVER_COLOR : NORMAL_COLOR);
        slotLabel.setColor(hasFocus() ? NORMAL_COLOR : HOVER_COLOR);
    }

    private record SlotVisual(
            int offsetX,
            int offsetY,
            int width,
            int height,
            int textureX,
            int textureY) {
        private static SlotVisual from(GuiElement reservedSlot) {
            if (reservedSlot.getWidth() <= 11
                    && reservedSlot.getHeight() <= 11) {
                return new SlotVisual(0, 0, 11, 11, 68, 0);
            }
            if (reservedSlot.getClass().getSimpleName()
                    .contains("VariantMajor")) {
                return new SlotVisual(1, 0, 15, 15, 52, 0);
            }
            return new SlotVisual(0, 0, 16, 16, 52, 16);
        }
    }

    private static final class GuiStringScaledOutline extends GuiStringOutline {
        private static final float SCALE = 0.75f;

        private GuiStringScaledOutline(int x, int y, String value) {
            super(scaleUp(x), scaleUp(y), value);
        }

        @Override
        public void setX(int x) {
            super.setX(scaleUp(x));
        }

        @Override
        public void setY(int y) {
            super.setY(scaleUp(y));
        }

        @Override
        public int getX() {
            return Math.round(super.getX() * SCALE);
        }

        @Override
        public int getY() {
            return Math.round(super.getY() * SCALE);
        }

        @Override
        public int getWidth() {
            return (int) Math.ceil(super.getWidth() * SCALE);
        }

        @Override
        public int getHeight() {
            return (int) Math.ceil(super.getHeight() * SCALE);
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y,
                int mouseX, int mouseY, int guiLeft, int guiTop,
                float partialTicks) {
            graphics.pose().pushPose();
            try {
                graphics.pose().scale(SCALE, SCALE, 1.0f);
                super.draw(graphics,
                        scaleUp(x), scaleUp(y),
                        scaleUp(mouseX), scaleUp(mouseY),
                        scaleUp(guiLeft), scaleUp(guiTop), partialTicks);
            } finally {
                graphics.pose().popPose();
            }
        }

        private static int scaleUp(int value) {
            return Math.round(value / SCALE);
        }
    }
}
