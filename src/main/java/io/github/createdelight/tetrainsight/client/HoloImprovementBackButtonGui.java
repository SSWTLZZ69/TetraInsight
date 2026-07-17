package io.github.createdelight.tetrainsight.client;

import net.minecraft.client.resources.language.I18n;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiStringOutline;

/** Tetra-styled text link shown above an improvement detail page. */
public final class HoloImprovementBackButtonGui extends GuiClickable {
    private final GuiStringOutline label;

    public HoloImprovementBackButtonGui(Runnable onClick) {
        super(0, -16, 0, 10, onClick);
        label = new GuiStringOutline(0, 0,
                I18n.get("tetra_insight.holo.improvement.back"));
        addChild(label);
        setWidth(label.getWidth());
        setVisible(false);
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        label.setColor(0xffffcc);
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        label.setColor(0xffffff);
    }
}
