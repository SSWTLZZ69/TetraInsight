package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;
import java.util.stream.Stream;

public record TetraProbeSnapshot(
        List<MaterialDefinitionSnapshot> materialImprovements,
        List<MaterialDefinitionSnapshot> materialVariants,
        List<MaterialSchematicSnapshot> materialSchematics
) {
    public static final TetraProbeSnapshot EMPTY = new TetraProbeSnapshot(List.of(), List.of(), List.of());

    public TetraProbeSnapshot {
        materialImprovements = List.copyOf(materialImprovements);
        materialVariants = List.copyOf(materialVariants);
        materialSchematics = List.copyOf(materialSchematics);
    }

    public long missingTranslationCount() {
        return materialSchematics.stream()
                .filter(schematic -> !schematic.hasAuthorTranslation())
                .count();
    }

    public List<MaterialDefinitionSnapshot> linkedDefinitions(MaterialSchematicSnapshot schematic) {
        return Stream.concat(materialImprovements.stream(), materialVariants.stream())
                .filter(definition -> switch (definition.sourceKind()) {
                    case IMPROVEMENT -> schematic.improvementPrefixes().contains(definition.ownerKey());
                    case MODULE_VARIANT -> schematic.moduleVariantPrefixes().contains(definition.ownerKey());
                })
                .toList();
    }

    public long linkedMaterialSchematicCount() {
        return materialSchematics.stream()
                .filter(schematic -> !linkedDefinitions(schematic).isEmpty())
                .count();
    }

    public long missingTranslationWithLinkedDefinitionCount() {
        return materialSchematics.stream()
                .filter(schematic -> !schematic.hasAuthorTranslation())
                .filter(schematic -> !linkedDefinitions(schematic).isEmpty())
                .count();
    }

    public int totalMaterialCandidateCount() {
        return materialSchematics.stream()
                .mapToInt(MaterialSchematicSnapshot::candidateCount)
                .sum();
    }

    public long globalHiddenMaterialCandidateCount() {
        return materialSchematics.stream()
                .flatMap(schematic -> schematic.materialSlots().stream())
                .flatMap(slot -> slot.candidates().stream())
                .filter(MaterialCandidateSnapshot::hiddenInGlobalMaterialBrowser)
                .count();
    }

    public long displayTranslationCount(TranslationProvenance provenance) {
        return materialSchematics.stream()
                .filter(schematic -> schematic.displayTranslation().provenance() == provenance)
                .count();
    }

    public boolean isEmpty() {
        return materialImprovements.isEmpty() && materialVariants.isEmpty() && materialSchematics.isEmpty();
    }
}
