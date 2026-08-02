package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.TetraInsight;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiStringSmall;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        this.selected = entries.contains(selected) ? selected : null;
        if (isCollapsible() && !expanded) {
            applyLayout();
        }
    }

    public void reapplyLayout() {
        applyLayout();
    }

    private boolean isCollapsible() {
        return entryCount > MaterialGroupWindow.COLLAPSE_THRESHOLD;
    }

    private void requestToggle() {
        onToggle.run();
    }

    private void applyLayout() {
        if (!isCollapsible()) {
            return;
        }

        int selectedIndex = selected == null ? -1 : entries.indexOf(selected);
        if (selectedIndex >= entryCount) {
            selectedIndex = -1;
        }
        MaterialGroupWindow window = MaterialGroupWindow.of(
                entryCount, selectedIndex, expanded, compactCount);
        for (int index = 0; index < items.size(); index++) {
            GuiElement item = items.get(index);
            item.setVisible(false);
            if (index < entryCount
                    && !window.visibleIndices().contains(index)) {
                restoreHiddenItem(index, item);
            }
        }

        int contentWidth = 0;
        for (int slot = 0; slot < window.visibleIndices().size(); slot++) {
            int entryIndex = window.visibleIndices().get(slot);
            if (entryIndex < 0 || entryIndex >= entryCount
                    || slot >= nativeX.length) {
                continue;
            }
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
            int controlSlot = Math.min(compactCount, entryCount - 1);
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
