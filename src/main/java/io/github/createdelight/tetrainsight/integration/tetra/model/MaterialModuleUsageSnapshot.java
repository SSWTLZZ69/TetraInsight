package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;

public record MaterialModuleUsageSnapshot(
        String slotName,
        String name,
        MaterialUsageGlyphSnapshot glyph,
        MaterialUsageNavigationSnapshot navigation,
        MaterialStatPreviewSnapshot statPreview,
        boolean usesMaterialDirectly,
        List<MaterialImprovementUsageSnapshot> improvements
) {
    public MaterialModuleUsageSnapshot {
        improvements = List.copyOf(improvements);
    }
}
