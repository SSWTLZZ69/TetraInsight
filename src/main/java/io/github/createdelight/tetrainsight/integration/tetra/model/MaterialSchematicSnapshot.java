package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record MaterialSchematicSnapshot(
        String schematicKey,
        boolean hasAuthorTranslation,
        int materialOutcomeCount,
        List<ResourceLocation> materialSelectors,
        Set<String> improvementPrefixes,
        Set<String> moduleVariantPrefixes,
        List<MaterialSlotSnapshot> materialSlots,
        MaterialTranslationInsight displayTranslation
) {
    public MaterialSchematicSnapshot {
        materialSelectors = List.copyOf(materialSelectors);
        improvementPrefixes = Set.copyOf(improvementPrefixes);
        moduleVariantPrefixes = Set.copyOf(moduleVariantPrefixes);
        materialSlots = List.copyOf(materialSlots);
    }

    public MaterialSchematicSnapshot withDisplayTranslation(MaterialTranslationInsight translation) {
        return new MaterialSchematicSnapshot(schematicKey, hasAuthorTranslation, materialOutcomeCount,
                materialSelectors, improvementPrefixes, moduleVariantPrefixes, materialSlots, translation);
    }

    public int candidateCount() {
        return materialSlots.stream()
                .mapToInt(slot -> slot.candidates().size())
                .sum();
    }

}
