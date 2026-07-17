package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloSortPageControls;
import io.github.createdelight.tetrainsight.client.PaginationWindow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.gui.stats.bar.GuiStatBase;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloStatsGui;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps Tetra's five-column stat layout, but reserves the final cell of the
 * third row for page controls. This prevents a fourth stat row from colliding
 * with the improvement list below it.
 */
@Mixin(value = HoloStatsGui.class, remap = false)
public abstract class HoloStatsGuiMixin extends GuiElement {
    @Unique
    private static final int tetraInsight$PAGE_SIZE = 14;

    @Unique
    private static final int tetraInsight$PAGER_X = 272;

    @Unique
    private static final int tetraInsight$PAGER_Y = 36;

    @Shadow
    @Final
    private GuiElement barGroup;

    @Unique
    private List<GuiStatBase> tetraInsight$allBars = List.of();

    @Unique
    private HoloSortPageControls tetraInsight$pageControls;

    @Unique
    private int tetraInsight$currentPage;

    @Unique
    private KeyframeAnimation tetraInsight$pageAnimation;

    protected HoloStatsGuiMixin(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addPageControls(int x, int y, CallbackInfo ci) {
        tetraInsight$pageControls = new HoloSortPageControls(
                () -> tetraInsight$changePage(-1),
                () -> tetraInsight$changePage(1));
        tetraInsight$pageControls.setX(tetraInsight$PAGER_X);
        tetraInsight$pageControls.setY(tetraInsight$PAGER_Y);
        tetraInsight$pageControls.update(48, 1, 1);
        addChild(tetraInsight$pageControls);
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$paginateBars(ItemStack currentStack,
            ItemStack previewStack, String slot, String improvement,
            Player player, CallbackInfo ci) {
        List<GuiStatBase> visibleBars = new ArrayList<>();
        for (GuiElement child : barGroup.getChildren()) {
            if (child instanceof GuiStatBase bar) {
                visibleBars.add(bar);
            }
        }
        tetraInsight$allBars = List.copyOf(visibleBars);
        tetraInsight$applyPage(false);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (tetraInsight$pageControls.isVisible() && hasFocus() && amount != 0) {
            return tetraInsight$changePage(amount < 0 ? 1 : -1);
        }
        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    @Unique
    private boolean tetraInsight$changePage(int delta) {
        int nextPage = PaginationWindow.of(
                tetraInsight$allBars.size(), tetraInsight$currentPage + delta,
                tetraInsight$PAGE_SIZE).currentPage();
        if (nextPage == tetraInsight$currentPage) {
            return false;
        }
        tetraInsight$currentPage = nextPage;
        tetraInsight$applyPage(true);
        return true;
    }

    @Unique
    private void tetraInsight$applyPage(boolean animate) {
        PaginationWindow window = PaginationWindow.of(
                tetraInsight$allBars.size(), tetraInsight$currentPage,
                tetraInsight$PAGE_SIZE);
        tetraInsight$currentPage = window.currentPage();

        barGroup.clearChildren();
        for (int index = window.startIndex(); index < window.endIndex(); index++) {
            GuiStatBase bar = tetraInsight$allBars.get(index);
            int localIndex = index - window.startIndex();
            bar.setX(localIndex % 5 * 68);
            bar.setY(localIndex / 5 * 17);
            barGroup.addChild(bar);
        }

        tetraInsight$pageControls.update(
                48, tetraInsight$currentPage + 1, window.totalPages());
        tetraInsight$pageControls.setX(tetraInsight$PAGER_X);
        tetraInsight$pageControls.setY(tetraInsight$PAGER_Y);

        if (animate) {
            if (tetraInsight$pageAnimation != null) {
                tetraInsight$pageAnimation.stop();
            }
            tetraInsight$pageAnimation = new KeyframeAnimation(55, barGroup)
                    .applyTo(new Applier.Opacity(0.65f, 1f));
            tetraInsight$pageAnimation.start();
        } else {
            barGroup.setOpacity(1f);
        }
    }
}
