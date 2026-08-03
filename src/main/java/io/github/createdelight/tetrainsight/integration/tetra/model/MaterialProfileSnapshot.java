package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;

public record MaterialProfileSnapshot(
        String materialKey,
        String category,
        Float primary,
        Float secondary,
        Float tertiary,
        float durability,
        float integrityGain,
        float integrityCost,
        int magicCapacity,
        int toolLevel,
        float toolEfficiency,
        int glyphTint,
        boolean hiddenInGlobalMaterialBrowser,
        List<MaterialItemSource> sourceItems,
        int attributeCount,
        int effectCount,
        int aspectCount,
        int featureCount,
        int improvementCount
) {
    public MaterialProfileSnapshot {
        sourceItems = List.copyOf(sourceItems);
    }
}
