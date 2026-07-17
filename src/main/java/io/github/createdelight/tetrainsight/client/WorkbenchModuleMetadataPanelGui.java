package io.github.createdelight.tetrainsight.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiRect;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.tetra.gui.ZOffsetGui;

public final class WorkbenchModuleMetadataPanelGui extends ZOffsetGui {
    private static final int PANEL_WIDTH = 216;
    private static final int PANEL_HEIGHT = 63;
    private static final int CONTENT_WIDTH = PANEL_WIDTH - 14;
    private static final int LINES_PER_PAGE = 4;

    private final GuiStringOutline title;
    private final ComponentLinesGui content;
    private final HoloSortPageControls pageControls;
    private final Runnable onClose;
    private List<FormattedCharSequence> lines = List.of();
    private int page;

    public WorkbenchModuleMetadataPanelGui(int x, int y, Runnable onClose) {
        super(x, y, 230.0D);
        this.onClose = onClose;
        setWidth(PANEL_WIDTH);
        setHeight(PANEL_HEIGHT);

        GuiRect backdrop = new GuiRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0);
        backdrop.setOpacity(0.94f);
        addChild(backdrop);
        addCornerStrokes();

        title = new GuiStringOutline(6, 3, "");
        addChild(title);

        addChild(new PanelTextButton(PANEL_WIDTH - 12, 2, "x", this::close));

        content = new ComponentLinesGui(7, 15, CONTENT_WIDTH, 38);
        addChild(content);

        pageControls = new HoloSortPageControls(this::previousPage, this::nextPage);
        pageControls.setX(6);
        pageControls.setY(PANEL_HEIGHT - 11);
        addChild(pageControls);
        setVisible(false);
    }

    public void open(String titleText, List<Component> components) {
        title.setString(titleText);
        lines = wrap(components);
        page = 0;
        refreshPage();
        setVisible(true);
    }

    public void close() {
        if (!isVisible()) {
            return;
        }
        setVisible(false);
        onClose.run();
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (isVisible() && hasFocus() && totalPages() > 1 && amount != 0) {
            int previousPage = page;
            if (amount < 0) {
                nextPage();
            } else {
                previousPage();
            }
            return previousPage != page;
        }
        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    private List<FormattedCharSequence> wrap(List<Component> components) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> result = new ArrayList<>();
        for (Component component : components) {
            if (component.getString().isEmpty()) {
                result.add(FormattedCharSequence.EMPTY);
                continue;
            }
            result.addAll(font.split(component, CONTENT_WIDTH));
        }
        if (result.isEmpty()) {
            result.add(FormattedCharSequence.EMPTY);
        }
        return List.copyOf(result);
    }

    private void previousPage() {
        if (page > 0) {
            page--;
            refreshPage();
        }
    }

    private void nextPage() {
        if (page + 1 < totalPages()) {
            page++;
            refreshPage();
        }
    }

    private void refreshPage() {
        int from = page * LINES_PER_PAGE;
        int to = Math.min(lines.size(), from + LINES_PER_PAGE);
        content.update(lines.subList(from, to));
        pageControls.update(PANEL_WIDTH - 12, page + 1, totalPages());
    }

    private int totalPages() {
        return Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
    }

    private void addCornerStrokes() {
        addChild(new GuiRect(1, 1, 6, 1, 0xffffff));
        addChild((GuiRect) new GuiRect(-1, 1, 6, 1, 0xffffff)
                .setAttachment(GuiAttachment.topRight));
        addChild((GuiRect) new GuiRect(-1, -1, 6, 1, 0xffffff)
                .setAttachment(GuiAttachment.bottomRight));
        addChild((GuiRect) new GuiRect(1, -1, 6, 1, 0xffffff)
                .setAttachment(GuiAttachment.bottomLeft));
    }

    private static final class ComponentLinesGui extends GuiElement {
        private List<FormattedCharSequence> lines = List.of();

        private ComponentLinesGui(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        private void update(List<FormattedCharSequence> lines) {
            this.lines = List.copyOf(lines);
        }

        @Override
        protected void drawChildren(
                GuiGraphics graphics,
                int x,
                int y,
                int mouseX,
                int mouseY,
                int guiLeft,
                int guiTop,
                float partialTicks
        ) {
            Font font = Minecraft.getInstance().font;
            for (int index = 0; index < lines.size(); index++) {
                graphics.drawString(font, lines.get(index), x, y + index * 9, 0xffffff, true);
            }
            super.drawChildren(graphics, x, y, mouseX, mouseY, guiLeft, guiTop, partialTicks);
        }
    }

    private static final class PanelTextButton extends GuiClickable {
        private final GuiStringOutline label;

        private PanelTextButton(int x, int y, String text, Runnable onClick) {
            super(x, y, 9, 9, onClick);
            label = new GuiStringOutline(1, 0, text);
            addChild(label);
        }

        @Override
        protected void onFocus() {
            label.setColor(0xffffcc);
        }

        @Override
        protected void onBlur() {
            label.setColor(0xffffff);
        }
    }
}
