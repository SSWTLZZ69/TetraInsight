package io.github.createdelight.tetrainsight.client;

public interface HoloMaterialGroupFoldAccess {
    void tetraInsight$configureFold(Runnable onToggle);

    boolean tetraInsight$isExpanded();

    void tetraInsight$setExpanded(boolean expanded);

    /**
     * Applies an optional entry filter and ordering to this group. Both
     * arguments may be {@code null} to restore the native presentation.
     *
     * @return the number of group entries remaining after filtering
     */
    default int tetraInsight$applyView(
            java.util.function.Predicate<se.mickelus.tetra.module.data.MaterialData> filter,
            java.util.Comparator<se.mickelus.tetra.module.data.MaterialData> sorter) {
        return -1;
    }
}
