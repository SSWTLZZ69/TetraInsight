package io.github.createdelight.tetrainsight.client;

import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.gui.GuiTextures;

public final class HoloMaterialImpactButtonGui extends GuiClickable {
    private final GuiTexture icon;
    private final GuiStringOutline label;
    private boolean enabled;
    private boolean active;

    public HoloMaterialImpactButtonGui(Runnable onClick) {
        super(0, 0, 0, 9, onClick);
        enabled = true;

        icon = new GuiTexture(0, 0, 9, 9, 224, 0, GuiTextures.workbench);
        addChild(icon);

        label = new GuiStringOutline(
                12,
                0,
                I18n.get("tetra_insight.holo.material_impact.button")
        );
        addChild(label);
        setWidth(12 + label.getWidth());
        updateStyling();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateStyling();
    }

    public void setActive(boolean active) {
        this.active = active;
        updateStyling();
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        return enabled && super.onMouseClick(mouseX, mouseY, button);
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
        if (!enabled && hasFocus()) {
            return List.of(Component.translatable(
                    "tetra_insight.holo.material_impact.unavailable"
            ));
        }
        return null;
    }

    private void updateStyling() {
        int color;
        if (!enabled) {
            color = 0x404040;
        } else if (hasFocus()) {
            color = 0xffffcc;
        } else if (active) {
            color = 0xffffff;
        } else {
            color = 0x7f7f7f;
        }
        icon.setColor(color);
        label.setColor(color);
    }
}
