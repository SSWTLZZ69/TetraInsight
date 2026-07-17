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
import se.mickelus.tetra.blocks.workbench.gui.WorkbenchStatsGui;
import se.mickelus.tetra.gui.stats.bar.GuiStatBase;

import java.util.ArrayList;
import java.util.List;

/** Prevents the workbench's three-row stat layout from growing off-screen. */
@Mixin(value = WorkbenchStatsGui.class, remap = false)
public abstract class WorkbenchStatsGuiMixin extends GuiElement {
    @Unique
    private static final int tetraInsight$PAGE_SIZE = 18;

    @Shadow
    @Final
    private GuiElement barGroup;

    @Shadow
    public abstract void realignBars();

    @Unique
    private List<GuiStatBase> tetraInsight$allBars = List.of();

    @Unique
    private HoloSortPageControls tetraInsight$pageControls;

    @Unique
    private int tetraInsight$currentPage;

    @Unique
    private KeyframeAnimation tetraInsight$pageAnimation;

    protected WorkbenchStatsGuiMixin(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addPageControls(int x, int y, CallbackInfo ci) {
        tetraInsight$pageControls = new HoloSortPageControls(
                () -> tetraInsight$changePage(-1),
                () -> tetraInsight$changePage(1));
        tetraInsight$pageControls.setX(80);
        tetraInsight$pageControls.setY(-12);
        tetraInsight$pageControls.update(40, 1, 1);
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
            barGroup.addChild(tetraInsight$allBars.get(index));
        }
        realignBars();

        tetraInsight$pageControls.update(
                40, tetraInsight$currentPage + 1, window.totalPages());
        tetraInsight$pageControls.setX(80);
        tetraInsight$pageControls.setY(-12);

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
