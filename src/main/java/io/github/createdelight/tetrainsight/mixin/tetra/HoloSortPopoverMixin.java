package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.client.ClearSorterOption;
import io.github.createdelight.tetrainsight.client.HoloSortPageControls;
import io.github.createdelight.tetrainsight.client.HoloSortPaginationAccess;
import io.github.createdelight.tetrainsight.client.PaginationWindow;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.mutil.gui.impl.GuiVerticalLayoutGroup;
import se.mickelus.tetra.gui.ZOffsetGui;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSortPopover;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloFilterButton;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Mixin(value = HoloSortPopover.class, remap = false)
public abstract class HoloSortPopoverMixin extends ZOffsetGui implements HoloSortPaginationAccess {
    @Unique
    private static final int tetraInsight$PAGE_SIZE = 9;

    @Unique
    private static final int tetraInsight$ITEM_HEIGHT = 10;

    @Unique
    private static final int tetraInsight$ITEM_SPACING = 3;

    @Shadow
    @Final
    private GuiVerticalLayoutGroup items;

    @Shadow
    @Final
    private GuiElement backdrop;

    @Shadow
    @Final
    private Consumer<IStatSorter> onSelect;

    @Unique
    private int tetraInsight$currentPage;

    @Unique
    private int tetraInsight$totalPages = 1;

    @Unique
    private int tetraInsight$baseWidth;

    @Unique
    private IStatSorter[] tetraInsight$sorters = new IStatSorter[0];

    @Unique
    private HoloSortPageControls tetraInsight$pageControls;

    @Unique
    private HoloFilterButton tetraInsight$searchField;

    @Unique
    private GuiString tetraInsight$searchHint;

    @Unique
    private GuiStringOutline tetraInsight$noResults;

    @Unique
    private String tetraInsight$query = "";

    @Unique
    private int tetraInsight$lastLoggedSorterCount = -1;

    protected HoloSortPopoverMixin(int x, int y, double z) {
        super(x, y, z);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addPageControls(int x, int y, Consumer<IStatSorter> onSelect,
            CallbackInfo ci) {
        tetraInsight$pageControls = new HoloSortPageControls(
                () -> tetraInsight$changePage(-1),
                () -> tetraInsight$changePage(1));
        tetraInsight$searchField = new HoloFilterButton(6, 6, tetraInsight$setQueryInternal());
        tetraInsight$searchHint = new GuiString(
                17,
                6,
                I18n.get("tetra_insight.holo.sort.search_hint")
        );
        tetraInsight$searchHint.setColor(0x7f7f7f);
        tetraInsight$noResults = new GuiStringOutline(
                6, 19, I18n.get("tetra_insight.holo.sort.no_results"));
        tetraInsight$noResults.setVisible(false);
        items.setY(19);
        addChild(tetraInsight$searchField);
        addChild(tetraInsight$searchHint);
        addChild(tetraInsight$noResults);
        addChild(tetraInsight$pageControls);
        tetraInsight$updateSearchHint();
    }

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void tetraInsight$rememberSorters(IStatSorter[] sorters, CallbackInfo ci) {
        tetraInsight$currentPage = 0;
        IStatSorter[] copiedSorters = new IStatSorter[sorters.length];
        System.arraycopy(sorters, 0, copiedSorters, 0, sorters.length);
        tetraInsight$sorters = copiedSorters;
        tetraInsight$query = "";
        if (tetraInsight$searchField != null) {
            tetraInsight$searchField.reset();
        }
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$paginate(IStatSorter[] sorters, CallbackInfo ci) {
        tetraInsight$baseWidth = getWidth();
        tetraInsight$applyPage();
        if (sorters.length > tetraInsight$PAGE_SIZE && sorters.length != tetraInsight$lastLoggedSorterCount) {
            TetraInsight.LOGGER.info("Paginated Tetra sorter popover: {} sorters across {} pages",
                    sorters.length, tetraInsight$totalPages);
            tetraInsight$lastLoggedSorterCount = sorters.length;
        }
    }

    @Inject(method = "onSelect", at = @At("HEAD"), remap = false)
    private void tetraInsight$clearSearchAfterMouseSelection(IStatSorter sorter, CallbackInfo ci) {
        tetraInsight$resetSearchAfterSelection();
    }

    @Inject(method = "onKeyPress", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$changePageByKey(int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        if ((tetraInsight$isSearchInputFocused() || !tetraInsight$query.isEmpty())
                && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)) {
            cir.setReturnValue(tetraInsight$searchField.onKeyPress(
                    GLFW.GLFW_KEY_BACKSPACE,
                    scanCode,
                    modifiers
            ));
            return;
        }
        if (tetraInsight$isSearchInputFocused()) {
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_PAGE_DOWN
                || keyCode == GLFW.GLFW_KEY_KP_ADD
                || keyCode == GLFW.GLFW_KEY_EQUAL) {
            cir.setReturnValue(tetraInsight$changePage(1));
        } else if (keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_PAGE_UP
                || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT
                || keyCode == GLFW.GLFW_KEY_MINUS) {
            cir.setReturnValue(tetraInsight$changePage(-1));
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (tetraInsight$totalPages > 1 && hasFocus() && amount != 0) {
            return tetraInsight$changePage(amount < 0 ? 1 : -1);
        }
        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        boolean handled = super.onMouseClick(mouseX, mouseY, button);
        tetraInsight$updateSearchHint();
        return handled;
    }

    @Override
    public boolean onKeyRelease(int keyCode, int scanCode, int modifiers) {
        if (tetraInsight$isSearchInputFocused()) {
            return super.onKeyRelease(keyCode, scanCode, modifiers);
        }
        int localIndex = tetraInsight$numericIndex(keyCode);
        if (localIndex >= 0) {
            List<Integer> filteredIndices = tetraInsight$filteredIndices();
            int filteredIndex = tetraInsight$currentPage * tetraInsight$PAGE_SIZE + localIndex;
            if (filteredIndex < filteredIndices.size()) {
                IStatSorter sorter = tetraInsight$sorters[filteredIndices.get(filteredIndex)];
                tetraInsight$resetSearchAfterSelection();
                onSelect.accept(sorter);
                setVisible(false);
                return true;
            }
        }
        return super.onKeyRelease(keyCode, scanCode, modifiers);
    }

    @Unique
    private boolean tetraInsight$changePage(int delta) {
        int nextPage = PaginationWindow.of(
                tetraInsight$filteredIndices().size(),
                tetraInsight$currentPage + delta,
                tetraInsight$PAGE_SIZE).currentPage();
        if (nextPage == tetraInsight$currentPage) {
            return false;
        }
        tetraInsight$currentPage = nextPage;
        tetraInsight$applyPage();
        return true;
    }

    @Unique
    private void tetraInsight$applyPage() {
        tetraInsight$updateSearchHint();
        List<GuiElement> children = items.getChildren();
        List<Integer> filteredIndices = tetraInsight$filteredIndices();
        PaginationWindow window = PaginationWindow.of(
                filteredIndices.size(), tetraInsight$currentPage, tetraInsight$PAGE_SIZE);
        tetraInsight$totalPages = window.totalPages();
        tetraInsight$currentPage = window.currentPage();

        children.forEach(child -> child.setVisible(false));
        int row = 0;
        for (int filteredIndex = window.startIndex(); filteredIndex < window.endIndex(); filteredIndex++) {
            int childIndex = filteredIndices.get(filteredIndex);
            if (childIndex >= children.size()) {
                continue;
            }
            GuiElement child = children.get(childIndex);
            child.setVisible(true);
            child.setY(row * (tetraInsight$ITEM_HEIGHT + tetraInsight$ITEM_SPACING));
            row++;
        }

        int itemsHeight = row == 0
                ? 0
                : row * (tetraInsight$ITEM_HEIGHT + tetraInsight$ITEM_SPACING) - tetraInsight$ITEM_SPACING;
        items.setHeight(itemsHeight);
        tetraInsight$noResults.setVisible(filteredIndices.isEmpty());
        int contentHeight = filteredIndices.isEmpty() ? 10 : itemsHeight;

        int popoverWidth = Math.max(tetraInsight$baseWidth,
                Math.max(tetraInsight$searchField.getWidth() + 12,
                        tetraInsight$noResults.getWidth() + 12));
        setWidth(popoverWidth);
        children.forEach(child -> child.setWidth(popoverWidth - 12));

        boolean hasMultiplePages = tetraInsight$totalPages > 1;
        tetraInsight$pageControls.update(getWidth() - 12,
                tetraInsight$currentPage + 1, tetraInsight$totalPages);
        tetraInsight$pageControls.setY(19 + contentHeight + 2);

        int popoverHeight = 19 + contentHeight + 6 + (hasMultiplePages ? 12 : 0);
        setHeight(popoverHeight);
        backdrop.setHeight(popoverHeight);
        backdrop.setWidth(getWidth());
    }

    @Unique
    private boolean tetraInsight$isSearchInputFocused() {
        return tetraInsight$searchField instanceof HoloFilterButtonAccessor accessor
                && accessor.tetraInsight$isInputFocused();
    }

    @Unique
    private static int tetraInsight$numericIndex(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            return keyCode - GLFW.GLFW_KEY_1;
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return keyCode - GLFW.GLFW_KEY_KP_1;
        }
        return -1;
    }

    @Override
    public int tetraInsight$currentPage() {
        return tetraInsight$currentPage;
    }

    @Override
    public int tetraInsight$totalPages() {
        return tetraInsight$totalPages;
    }

    @Override
    public int tetraInsight$visibleItemCount() {
        return (int) items.getChildren().stream().filter(GuiElement::isVisible).count();
    }

    @Override
    public int tetraInsight$filteredItemCount() {
        return tetraInsight$filteredIndices().size();
    }

    @Override
    public void tetraInsight$setPage(int page) {
        tetraInsight$currentPage = page;
        tetraInsight$applyPage();
    }

    @Override
    public void tetraInsight$setQuery(String query) {
        tetraInsight$query = query == null ? "" : query;
        tetraInsight$currentPage = 0;
        tetraInsight$applyPage();
    }

    @Unique
    private Consumer<String> tetraInsight$setQueryInternal() {
        return query -> {
            tetraInsight$setQuery(query);
            tetraInsight$updateSearchHint();
        };
    }

    @Unique
    private void tetraInsight$updateSearchHint() {
        if (tetraInsight$searchField == null || tetraInsight$searchHint == null) {
            return;
        }
        boolean visible = tetraInsight$query.isEmpty() && !tetraInsight$isSearchInputFocused();
        tetraInsight$searchHint.setVisible(visible);
        if (visible) {
            tetraInsight$searchField.setWidth(11 + tetraInsight$searchHint.getWidth());
        } else if (tetraInsight$query.isEmpty()) {
            tetraInsight$searchField.setWidth(11);
        }
    }

    @Unique
    private List<Integer> tetraInsight$filteredIndices() {
        String normalizedQuery = tetraInsight$query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return IntStream.range(0, tetraInsight$sorters.length).boxed().toList();
        }
        return IntStream.range(0, tetraInsight$sorters.length)
                .filter(index -> tetraInsight$sorters[index] == ClearSorterOption.INSTANCE
                        || tetraInsight$sorters[index].getName()
                                .toLowerCase(Locale.ROOT)
                                .contains(normalizedQuery))
                .boxed()
                .toList();
    }

    @Unique
    private void tetraInsight$resetSearchAfterSelection() {
        tetraInsight$query = "";
        tetraInsight$currentPage = 0;
        tetraInsight$searchField.reset();
        tetraInsight$applyPage();
    }
}
