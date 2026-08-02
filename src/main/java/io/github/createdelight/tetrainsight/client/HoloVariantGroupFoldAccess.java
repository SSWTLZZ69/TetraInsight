package io.github.createdelight.tetrainsight.client;

public interface HoloVariantGroupFoldAccess {
    void tetraInsight$configureFold(Runnable onToggle);

    boolean tetraInsight$isExpanded();

    void tetraInsight$setExpanded(boolean expanded);
}
