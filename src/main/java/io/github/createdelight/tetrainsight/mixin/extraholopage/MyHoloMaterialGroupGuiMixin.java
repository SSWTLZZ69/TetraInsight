package io.github.createdelight.tetrainsight.mixin.extraholopage;

import io.github.createdelight.tetrainsight.client.HoloGroupFoldController;
import io.github.createdelight.tetrainsight.client.HoloMaterialGroupFoldAccess;
import io.github.createdelight.tetrainsight.client.MaterialGroupWindow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.module.data.MaterialData;

import java.util.List;
import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "net.yiran.extraholopage.util.MyHoloMaterialGroupGui",
        remap = false)
public abstract class MyHoloMaterialGroupGuiMixin
        implements HoloMaterialGroupFoldAccess {
    @Shadow
    @Final
    private GuiElement materialsContainer;

    @Shadow
    @Final
    private KeyframeAnimation[] itemAnimations;

    private HoloGroupFoldController<MaterialData> tetraInsight$fold;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
    private void tetraInsight$addFoldControl(
            int x,
            int y,
            @Coerce Object guiSorter,
            String category,
            List<MaterialData> materials,
            int animationOffset,
            Consumer<MaterialData> onHover,
            Consumer<MaterialData> onBlur,
            Consumer<MaterialData> onSelect,
            CallbackInfo ci) {
        tetraInsight$fold = new HoloGroupFoldController<>(
                (GuiElement) (Object) this,
                materialsContainer,
                "net.yiran.extraholopage.util.MyHoloMaterialGroupGui",
                category,
                materials,
                itemAnimations,
                MaterialGroupWindow.COMPACT_MATERIAL_COUNT);
    }

    @Inject(method = "animateIn", at = @At("RETURN"), remap = false,
            require = 0)
    private void tetraInsight$restoreFoldAfterAnimation(CallbackInfo ci) {
        if (tetraInsight$fold != null) {
            tetraInsight$fold.reapplyLayout();
        }
    }

    @Inject(method = "updateSelection", at = @At("RETURN"), remap = false,
            require = 0)
    private void tetraInsight$pinSelectedMaterial(
            MaterialData selected, CallbackInfo ci) {
        if (tetraInsight$fold != null) {
            tetraInsight$fold.updateSelection(selected);
        }
    }

    @Override
    public void tetraInsight$configureFold(Runnable onToggle) {
        if (tetraInsight$fold != null) {
            tetraInsight$fold.configureFold(onToggle);
        }
    }

    @Override
    public boolean tetraInsight$isExpanded() {
        return tetraInsight$fold != null && tetraInsight$fold.isExpanded();
    }

    @Override
    public void tetraInsight$setExpanded(boolean expanded) {
        if (tetraInsight$fold != null) {
            tetraInsight$fold.setExpanded(expanded);
        }
    }
}
