package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloSortPageControls;
import io.github.createdelight.tetrainsight.client.HoloStatsComparisonAccess;
import io.github.createdelight.tetrainsight.client.HoloStatsLayoutAccess;
import io.github.createdelight.tetrainsight.client.ImprovementComparisonMode;
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
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.gui.stats.bar.GuiStatBase;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloStatsGui;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps Tetra's stat layout, but reserves the final cell of the third row for
 * page controls. Individual screens can request a roomier grid without
 * changing every native Tetra stats panel.
 */
@Mixin(value = HoloStatsGui.class, remap = false)
public abstract class HoloStatsGuiMixin extends GuiElement
        implements HoloStatsComparisonAccess, HoloStatsLayoutAccess {

    @Unique
    private static final int tetraInsight$PAGER_Y = 36;

    @Unique
    private int tetraInsight$columns = 5;

    @Unique
    private int tetraInsight$columnSpacing = 68;

    @Shadow
    @Final
    private GuiElement barGroup;

    @Unique
    private List<GuiStatBase> tetraInsight$allBars = List.of();

    @Unique
    private HoloSortPageControls tetraInsight$pageControls;

    @Unique
    private GuiStringOutline tetraInsight$comparisonLabel;

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
        tetraInsight$pageControls.setX(tetraInsight$pagerX());
        tetraInsight$pageControls.setY(tetraInsight$PAGER_Y);
        tetraInsight$pageControls.update(48, 1, 1);
        addChild(tetraInsight$pageControls);

        tetraInsight$comparisonLabel = new GuiStringOutline(0, -10, "");
        tetraInsight$comparisonLabel.setColor(0x7f7f7f);
        tetraInsight$comparisonLabel.setVisible(false);
        addChild(tetraInsight$comparisonLabel);
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
                tetraInsight$pageSize()).currentPage();
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
                tetraInsight$pageSize());
        tetraInsight$currentPage = window.currentPage();

        barGroup.clearChildren();
        for (int index = window.startIndex(); index < window.endIndex(); index++) {
            GuiStatBase bar = tetraInsight$allBars.get(index);
            int localIndex = index - window.startIndex();
            bar.setX(localIndex % tetraInsight$columns * tetraInsight$columnSpacing);
            bar.setY(localIndex / tetraInsight$columns * 17);
            barGroup.addChild(bar);
        }

        tetraInsight$pageControls.update(
                48, tetraInsight$currentPage + 1, window.totalPages());
        tetraInsight$pageControls.setX(tetraInsight$pagerX());
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

    @Override
    @Unique
    public void tetraInsight$setGridLayout(int columns, int columnSpacing) {
        tetraInsight$columns = Math.max(1, columns);
        tetraInsight$columnSpacing = Math.max(1, columnSpacing);
        tetraInsight$currentPage = 0;
        tetraInsight$applyPage(false);
    }

    @Unique
    private int tetraInsight$pageSize() {
        return tetraInsight$columns * 3 - 1;
    }

    @Unique
    private int tetraInsight$pagerX() {
        return (tetraInsight$columns - 1) * tetraInsight$columnSpacing;
    }

    @Override
    @Unique
    public void tetraInsight$setComparisonMode(ImprovementComparisonMode mode) {
        String translation = switch (mode) {
            case BASE_TO_SELECTED ->
                    "tetra_insight.holo.improvement.compare.base_selected";
            case BASE_TO_PREVIEW ->
                    "tetra_insight.holo.improvement.compare.base_preview";
            case SELECTED_TO_PREVIEW ->
                    "tetra_insight.holo.improvement.compare.selected_preview";
            case NONE -> "";
        };
        tetraInsight$comparisonLabel.setString(translation.isEmpty()
                ? ""
                : net.minecraft.client.resources.language.I18n.get(translation));
        tetraInsight$comparisonLabel.setX(0);
        tetraInsight$comparisonLabel.setVisible(!translation.isEmpty());
    }
}
