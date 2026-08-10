package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloImprovementButtonAccess;
import net.minecraft.client.resources.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementButton;

@Mixin(value = HoloImprovementButton.class, remap = false)
public abstract class HoloImprovementButtonMixin implements HoloImprovementButtonAccess {
    @Unique
    private String tetraInsight$lastLabel = "";

    @Shadow
    GuiString label;

    @Shadow
    boolean hasImprovements;

    @Inject(method = "updateCount", at = @At("RETURN"), remap = false)
    private void tetraInsight$makeActionExplicit(int count, CallbackInfo ci) {
        if (count > 0) {
            tetraInsight$updateLabel(I18n.get("tetra_insight.holo.improvement.open", count));
        } else {
            tetraInsight$updateLabel(I18n.get("tetra_insight.holo.improvement.none"));
        }
    }

    @Override
    public void tetraInsight$showSelectionPrompt() {
        hasImprovements = false;
        label.setColor(0x7f7f7f);
        tetraInsight$updateLabel(I18n.get("tetra_insight.holo.improvement.select_module"));
    }

    @Override
    public String tetraInsight$labelText() {
        return tetraInsight$lastLabel;
    }

    @Unique
    private void tetraInsight$updateLabel(String value) {
        tetraInsight$lastLabel = value;
        label.setString(value);
        ((HoloImprovementButton) (Object) this).setWidth(label.getWidth() + 16);
    }
}
