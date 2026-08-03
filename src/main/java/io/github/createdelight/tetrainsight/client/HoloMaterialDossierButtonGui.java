package io.github.createdelight.tetrainsight.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiStringOutline;

public final class HoloMaterialDossierButtonGui extends GuiClickable {
    private final GuiStringOutline label;
    private boolean active;
    private boolean enabled = true;

    public HoloMaterialDossierButtonGui(int x, int y, Runnable onClick) {
        super(x, y, 52, 11, onClick);
        label = new GuiStringOutline(1, 1, "");
        addChild(label);
        refreshColor();
    }

    public void update(int usageCount) {
        label.setString(Component.translatable(
                "tetra_insight.material.dossier.button",
                usageCount).getString());
        setWidth(Math.max(52, label.getWidth() + 2));
    }

    public void setActive(boolean active) {
        this.active = active;
        refreshColor();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        refreshColor();
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        return enabled && super.onMouseClick(mouseX, mouseY, button);
    }

    @Override
    public List<Component> getTooltipLines() {
        if (!hasFocus()) {
            return null;
        }
        return List.of(Component.translatable(enabled
                        ? "tetra_insight.material.dossier.button.tooltip"
                        : "tetra_insight.material.dossier.unavailable")
                .withStyle(enabled ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void onFocus() {
        refreshColor();
    }

    @Override
    protected void onBlur() {
        refreshColor();
    }

    private void refreshColor() {
        label.setColor(!enabled
                ? 0x404040
                : active || hasFocus() ? 0xffffff : 0xaaaaaa);
    }
}
