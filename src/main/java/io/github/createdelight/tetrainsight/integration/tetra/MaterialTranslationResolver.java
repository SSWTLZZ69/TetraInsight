package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialDefinitionSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialInputChannel;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialOutputKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationInsight;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MaterialTranslationResolver {
    private MaterialTranslationResolver() {
    }

    public static MaterialTranslationInsight resolve(MaterialSchematicSnapshot schematic,
            List<MaterialDefinitionSnapshot> definitions, MaterialDisplayLevelCalibrator calibrator) {
        List<MaterialTranslationEntry> actualEntries = definitions.stream()
                .map(MaterialDefinitionSnapshot::generatedTranslation)
                .flatMap(translation -> translation.entries().stream())
                .toList();

        if (schematic.hasAuthorTranslation()) {
            List<MaterialTranslationEntry> entries = schematic.displayTranslation().entries().stream()
                    .map(authorEntry -> enrichAuthorEntry(authorEntry, actualEntries))
                    .toList();
            return new MaterialTranslationInsight(schematic.schematicKey(), TranslationProvenance.AUTHOR, entries);
        }

        Map<EntryKey, List<MaterialTranslationEntry>> grouped = new LinkedHashMap<>();
        actualEntries.forEach(entry -> grouped.computeIfAbsent(EntryKey.from(entry), ignored -> new ArrayList<>()).add(entry));

        List<MaterialTranslationEntry> generated = grouped.values().stream()
                .map(MaterialTranslationResolver::chooseConsistentCoefficient)
                .filter(java.util.Objects::nonNull)
                .map(entry -> new MaterialTranslationEntry(
                        entry.input(), entry.outputKind(), entry.outputId(), entry.operation(),
                        entry.actualCoefficient(), calibrator.displayLevel(entry)))
                .filter(entry -> entry.generatedDisplayLevel() != 0)
                .toList();

        TranslationProvenance provenance;
        if (!generated.isEmpty()) {
            provenance = TranslationProvenance.GENERATED_FROM_EXTRACT;
        } else if (!definitions.isEmpty() && actualEntries.isEmpty()) {
            provenance = TranslationProvenance.NO_MATERIAL_SCALING;
        } else {
            provenance = TranslationProvenance.UNAVAILABLE;
        }
        return new MaterialTranslationInsight(schematic.schematicKey(), provenance, generated);
    }

    static boolean sameOutput(MaterialTranslationEntry left, MaterialTranslationEntry right) {
        return left.input() == right.input()
                && left.outputKind() == right.outputKind()
                && left.outputId().equals(right.outputId())
                && left.operation().equals(right.operation());
    }

    private static MaterialTranslationEntry enrichAuthorEntry(MaterialTranslationEntry authorEntry,
            List<MaterialTranslationEntry> actualEntries) {
        MaterialTranslationEntry actual = chooseConsistentCoefficient(actualEntries.stream()
                .filter(entry -> sameOutput(authorEntry, entry))
                .toList());
        return new MaterialTranslationEntry(
                authorEntry.input(), authorEntry.outputKind(), authorEntry.outputId(), authorEntry.operation(),
                actual != null ? actual.actualCoefficient() : Double.NaN,
                authorEntry.generatedDisplayLevel());
    }

    private static MaterialTranslationEntry chooseConsistentCoefficient(List<MaterialTranslationEntry> entries) {
        if (entries.isEmpty()) {
            return null;
        }

        boolean positive = entries.stream().anyMatch(entry -> entry.actualCoefficient() > 0);
        boolean negative = entries.stream().anyMatch(entry -> entry.actualCoefficient() < 0);
        if (positive && negative) {
            return null;
        }

        return entries.stream()
                .filter(entry -> Double.isFinite(entry.actualCoefficient()) && entry.actualCoefficient() != 0)
                .max(Comparator.comparingDouble(entry -> Math.abs(entry.actualCoefficient())))
                .orElse(null);
    }

    private record EntryKey(
            MaterialInputChannel input,
            MaterialOutputKind outputKind,
            String outputId,
            String operation
    ) {
        private static EntryKey from(MaterialTranslationEntry entry) {
            return new EntryKey(entry.input(), entry.outputKind(), entry.outputId(), entry.operation());
        }
    }
}
