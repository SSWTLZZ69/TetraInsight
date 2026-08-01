package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloGroupFoldController;
import io.github.createdelight.tetrainsight.client.MaterialGroupWindow;
import io.github.createdelight.tetrainsight.client.HoloVariantGroupFoldAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloVariantGroupGui;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.List;
import java.util.function.Consumer;

@Mixin(value = HoloVariantGroupGui.class, remap = false)
public abstract class HoloVariantGroupGuiMixin implements HoloVariantGroupFoldAccess {
    @Shadow
    @Final
    private GuiElement variantsContainer;

    @Shadow
    @Final
    private KeyframeAnimation[] itemAnimations;

    private HoloGroupFoldController<OutcomePreview> tetraInsight$fold;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addFoldControl(int x, int y, String category,
            List<OutcomePreview> outcomes, int animationOffset,
            IStatSorter sorter, Player player,
            Consumer<OutcomePreview> onHover,
            Consumer<OutcomePreview> onBlur,
            Consumer<OutcomePreview> onSelect,
            CallbackInfo ci) {
        tetraInsight$fold = new HoloGroupFoldController<>(
                (GuiElement) (Object) this,
                variantsContainer,
                HoloVariantGroupGui.class.getName(),
                category,
                outcomes,
                itemAnimations,
                MaterialGroupWindow.COMPACT_MATERIAL_COUNT);
    }

    @Inject(method = "animateIn", at = @At("RETURN"), remap = false)
    private void tetraInsight$restoreFoldAfterAnimation(CallbackInfo ci) {
        if (tetraInsight$fold != null) {
            tetraInsight$fold.reapplyLayout();
        }
    }

    @Inject(method = "updateSelection", at = @At("RETURN"), remap = false)
    private void tetraInsight$pinSelectedVariant(
            OutcomePreview selected, CallbackInfo ci) {
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
