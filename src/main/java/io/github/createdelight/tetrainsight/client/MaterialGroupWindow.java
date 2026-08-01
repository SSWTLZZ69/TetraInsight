package io.github.createdelight.tetrainsight.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects the material entries shown by a collapsed Tetra material category.
 * Seven entries leave the eighth native 2x4 cell available for the expand
 * control. A selected entry is pinned into the compact window when needed.
 */
public record MaterialGroupWindow(List<Integer> visibleIndices, int hiddenCount) {
    public static final int COLLAPSE_THRESHOLD = 8;
    public static final int COMPACT_MATERIAL_COUNT = 7;

    public MaterialGroupWindow {
        visibleIndices = List.copyOf(visibleIndices);
    }

    public static MaterialGroupWindow of(
            int totalCount, int selectedIndex, boolean expanded) {
        return of(totalCount, selectedIndex, expanded, COMPACT_MATERIAL_COUNT);
    }

    public static MaterialGroupWindow of(
            int totalCount, int selectedIndex, boolean expanded,
            int compactCount) {
        int safeTotal = Math.max(0, totalCount);
        int safeCompactCount = Math.max(1, compactCount);
        int collapseThreshold = Math.max(COLLAPSE_THRESHOLD, safeCompactCount);
        if (expanded || safeTotal <= collapseThreshold) {
            return new MaterialGroupWindow(
                    java.util.stream.IntStream.range(0, safeTotal)
                            .boxed()
                            .toList(),
                    0);
        }

        List<Integer> visible = new ArrayList<>(safeCompactCount);
        int leadingCount = selectedIndex >= safeCompactCount
                && selectedIndex < safeTotal
                ? safeCompactCount - 1
                : safeCompactCount;
        for (int index = 0; index < leadingCount; index++) {
            visible.add(index);
        }
        if (leadingCount < safeCompactCount) {
            visible.add(selectedIndex);
        }
        return new MaterialGroupWindow(visible, safeTotal - visible.size());
    }
}
