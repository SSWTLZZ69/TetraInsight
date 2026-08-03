package io.github.createdelight.tetrainsight.integration.tetra.model;

public record MaterialImprovementUsageSnapshot(
        String name,
        MaterialUsageGlyphSnapshot glyph,
        MaterialUsageNavigationSnapshot navigation,
        MaterialStatPreviewSnapshot statPreview
) {
}
