package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.gui.stats.sorting.IStatSorter;

import java.util.List;

/**
 * Exposes the fully merged sorter list (static + derived + contextual) that a
 * HoloSortButton produced during its last update, before Tetra's per-preview
 * shouldShow filter trims it for the popover. Diagnostics only.
 */
public interface HoloSortMergedListAccess {
    List<IStatSorter> tetraInsight$getMergedSorters();
}
