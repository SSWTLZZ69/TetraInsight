package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record MaterialDefinitionSnapshot(
        String ownerKey,
        MaterialSourceKind sourceKind,
        List<ResourceLocation> materialSelectors,
        MaterialTranslationInsight generatedTranslation
) {
    public MaterialDefinitionSnapshot {
        materialSelectors = List.copyOf(materialSelectors);
    }
}
