package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record MaterialCandidateSnapshot(
        String materialKey,
        String category,
        int slotIndex,
        int requiredQuantity,
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
        List<MaterialAttributeSnapshot> attributes,
        List<MaterialEffectSnapshot> effects,
        List<MaterialToolSnapshot> requiredTools,
        List<String> features,
        Map<String, Integer> improvements,
        List<ResourceLocation> matchedSelectors
) {
    public MaterialCandidateSnapshot {
        sourceItems = List.copyOf(sourceItems);
        attributes = List.copyOf(attributes);
        effects = List.copyOf(effects);
        requiredTools = List.copyOf(requiredTools);
        features = List.copyOf(features);
        improvements = Map.copyOf(improvements);
        matchedSelectors = List.copyOf(matchedSelectors);
    }
}
