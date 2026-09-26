package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.TetraInsight;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiStringSmall;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Shared folding state for Tetra's native material groups and compatible
 * replacement group widgets supplied by other client-side addons.
 */
public final class HoloGroupFoldController<T> {
    private static final Set<String> LOGGED_IMPLEMENTATIONS =
            ConcurrentHashMap.newKeySet();

    private final GuiElement owner;
    private final GuiElement entriesContainer;
    private final String implementation;
    private final String category;
    private final List<T> entries;
    private final List<GuiElement> items;
    private final KeyframeAnimation[] itemAnimations;
    private final int compactCount;
    private final int entryCount;
    private final int[] nativeX;
    private final int[] nativeY;
    private final int nativeWidth;
    private final GuiStringSmall categoryLabel;
    private final HoloVariantGroupFoldButtonGui expandButton;
    private final HoloVariantGroupFoldButtonGui collapseButton;

    private Runnable onToggle = () -> {};
    private T selected;
    private boolean expanded;
    private Predicate<T> viewFilter;
    private Comparator<T> viewSorter;

    public HoloGroupFoldController(
            GuiElement owner,
            GuiElement entriesContainer,
            String implementation,
            String category,
            List<T> entries,
            KeyframeAnimation[] itemAnimations,
            int compactCount) {
        this.owner = owner;
        this.entriesContainer = entriesContainer;
        this.implementation = implementation;
        this.category = category;
        this.entries = List.copyOf(entries);
        this.items = List.copyOf(entriesContainer.getChildren());
        this.itemAnimations = itemAnimations != null
                ? itemAnimations
                : new KeyframeAnimation[0];
        this.compactCount = Math.max(1, compactCount);
        this.entryCount = Math.min(this.entries.size(), this.items.size());
        this.nativeX = new int[this.items.size()];
        this.nativeY = new int[this.items.size()];
        for (int index = 0; index < this.items.size(); index++) {
            this.nativeX[index] = this.items.get(index).getX();
            this.nativeY[index] = this.items.get(index).getY();
        }
        this.nativeWidth = owner.getWidth();
        this.categoryLabel = owner.getChildren(GuiStringSmall.class)
                .stream()
                .findFirst()
                .orElse(null);

        if (isCollapsible()) {
            int controlSlot = Math.min(compactCount, entryCount - 1);
            this.expandButton = HoloVariantGroupFoldButtonGui.expandSlot(
                    items.get(controlSlot), this::requestToggle);
            entriesContainer.addChild(this.expandButton);
            this.collapseButton = HoloVariantGroupFoldButtonGui.collapseLink(
                    this::requestToggle);
            owner.addChild(this.collapseButton);
            applyLayout();
        } else {
            this.expandButton = null;
            this.collapseButton = null;
        }
    }

    public void configureFold(Runnable onToggle) {
        this.onToggle = onToggle != null ? onToggle : () -> {};
        if (isCollapsible()
                && LOGGED_IMPLEMENTATIONS.add(implementation)) {
            TetraInsight.LOGGER.info(
                    "Enabled collapsible holo material groups: implementation={}, compact={}, sampleCategory={}, entries={}, items={}, visible={}",
                    implementation, compactCount, category,
                    entries.size(), items.size(), countVisibleItems());
        }
    }

    /**
     * Applies an optional filter and ordering to the entries shown by this
     * group. Pass {@code null} for both to restore the native presentation.
     *
     * @return the number of entries remaining after filtering
     */
    public int applyView(Predicate<T> filter, Comparator<T> sorter) {
        viewFilter = filter;
        viewSorter = sorter;
        List<Integer> order = displayOrder();
        if (filter != null && expanded
                && order.size() <= MaterialGroupWindow.COLLAPSE_THRESHOLD) {
            expanded = false;
        }
        applyLayout();
        return order.size();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (!isCollapsible() || this.expanded == expanded) {
            return;
        }
        this.expanded = expanded;
        applyLayout();
    }

    public void updateSelection(T selected) {
        this.selected = selected != null && entries.contains(selected)
                ? selected
                : null;
        applyLayout();
    }

    public void reapplyLayout() {
        applyLayout();
    }

    private boolean foldingEnabled() {
        return TetraInsightConfig.materialGroupFolding.get();
    }

    private boolean isCollapsible() {
        return foldingEnabled()
                && entryCount > MaterialGroupWindow.COLLAPSE_THRESHOLD;
    }

    /**
     * The entry indices currently addressable by this group, filtered and
     * sorted. Positions in this list are mapped onto the native item slots.
     */
    private List<Integer> displayOrder() {
        List<Integer> order = IntStream.range(0, entryCount)
                .filter(index -> viewFilter == null
                        || viewFilter.test(entries.get(index)))
                .boxed()
                .collect(java.util.stream.Collectors
                        .toCollection(java.util.ArrayList::new));
        if (viewSorter != null) {
            order.sort(Comparator.comparing(entries::get, viewSorter));
        }
        return order;
    }

    private void requestToggle() {
        onToggle.run();
    }

    private void applyLayout() {
        List<Integer> order = displayOrder();
        int displayCount = order.size();
        boolean collapsible = foldingEnabled()
                && displayCount > MaterialGroupWindow.COLLAPSE_THRESHOLD;

        int selectedEntry = selected == null ? -1 : entries.indexOf(selected);
        if (selectedEntry >= entryCount) {
            selectedEntry = -1;
        }
        int selectedPosition = selectedEntry >= 0
                ? order.indexOf(selectedEntry)
                : -1;

        MaterialGroupWindow window = MaterialGroupWindow.of(
                displayCount,
                selectedPosition,
                expanded || !collapsible,
                compactCount);

        for (int index = 0; index < items.size(); index++) {
            items.get(index).setVisible(false);
            if (index < entryCount && !order.contains(index)) {
                restoreHiddenItem(index, items.get(index));
            }
        }

        int contentWidth = 0;
        for (int slot = 0; slot < window.visibleIndices().size(); slot++) {
            int position = window.visibleIndices().get(slot);
            if (position < 0 || position >= displayCount
                    || slot >= nativeX.length) {
                continue;
            }
            int entryIndex = order.get(position);
            GuiElement item = items.get(entryIndex);
            if (entryIndex != slot) {
                stopRelocatedAnimation(entryIndex, item);
            }
            item.setX(nativeX[slot]);
            item.setY(nativeY[slot]);
            item.setVisible(true);
            contentWidth = Math.max(
                    contentWidth, item.getX() + item.getWidth());
        }

        boolean controlsUsable = expandButton != null && collapseButton != null;
        if (!collapsible || !controlsUsable) {
            if (controlsUsable) {
                expandButton.setVisible(false);
                collapseButton.setVisible(false);
            }
            int width = Math.max(nativeWidth, contentWidth);
            entriesContainer.setWidth(width);
            owner.setWidth(width);
            return;
        }

        int labelWidth = categoryLabel != null
                ? categoryLabel.getWidth()
                : 28;
        int headerWidth = labelWidth;
        if (expanded) {
            expandButton.setVisible(false);
            collapseButton.placeCollapseLink(labelWidth + 3, 0);
            headerWidth = labelWidth + 3 + collapseButton.getWidth();
        } else {
            collapseButton.setVisible(false);
            int controlSlot = Math.min(compactCount, displayCount - 1);
            expandButton.placeExpandSlot(
                    nativeX[controlSlot], nativeY[controlSlot],
                    window.hiddenCount());
            contentWidth = Math.max(contentWidth,
                    expandButton.getX() + expandButton.getWidth());
        }

        int width = expanded
                ? Math.max(Math.max(nativeWidth, contentWidth), headerWidth)
                : Math.max(contentWidth, headerWidth);
        entriesContainer.setWidth(width);
        owner.setWidth(width);
    }

    private void stopRelocatedAnimation(int entryIndex, GuiElement item) {
        if (entryIndex < itemAnimations.length
                && itemAnimations[entryIndex] != null
                && itemAnimations[entryIndex].isActive()) {
            itemAnimations[entryIndex].stop();
        }
        item.setOpacity(1.0f);
    }

    private void restoreHiddenItem(int entryIndex, GuiElement item) {
        stopRelocatedAnimation(entryIndex, item);
        item.setX(nativeX[entryIndex]);
        item.setY(nativeY[entryIndex]);
    }

    private long countVisibleItems() {
        return items.stream()
                .filter(GuiElement::isVisible)
                .count();
    }
}
