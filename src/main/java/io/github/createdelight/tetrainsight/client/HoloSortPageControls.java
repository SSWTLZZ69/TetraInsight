package io.github.createdelight.tetrainsight.client;

import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiStringOutline;

public final class HoloSortPageControls extends GuiElement {
    private final PageButton previous;
    private final PageButton next;
    private final GuiStringOutline pageLabel;

    public HoloSortPageControls(Runnable onPrevious, Runnable onNext) {
        super(6, 0, 40, 10);
        previous = new PageButton(0, 0, "<", onPrevious);
        next = new PageButton(32, 0, ">", onNext);
        pageLabel = new GuiStringOutline(0, 0, "1 / 1");
        addChild(previous);
        addChild(pageLabel);
        addChild(next);
        setVisible(false);
    }

    public void update(int availableWidth, int currentPage, int totalPages) {
        setWidth(Math.max(24, availableWidth));
        pageLabel.setString(currentPage + " / " + totalPages);
        pageLabel.setX((getWidth() - pageLabel.getWidth()) / 2);
        next.setX(getWidth() - next.getWidth());
        previous.setVisible(currentPage > 1);
        next.setVisible(currentPage < totalPages);
        setVisible(totalPages > 1);
    }

    private static final class PageButton extends GuiClickable {
        private final GuiStringOutline label;

        private PageButton(int x, int y, String text, Runnable onClick) {
            super(x, y, 8, 10, onClick);
            label = new GuiStringOutline(1, 0, text);
            addChild(label);
        }

        @Override
        protected void onFocus() {
            label.setColor(0xffffcc);
            super.onFocus();
        }

        @Override
        protected void onBlur() {
            label.setColor(0xffffff);
            super.onBlur();
        }
    }
}
