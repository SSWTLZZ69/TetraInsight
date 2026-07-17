package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;

public record MaterialTranslationInsight(
        String ownerKey,
        TranslationProvenance provenance,
        List<MaterialTranslationEntry> entries
) {
    public MaterialTranslationInsight {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
