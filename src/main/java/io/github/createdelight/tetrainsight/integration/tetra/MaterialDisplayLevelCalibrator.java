package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialDefinitionSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialInputChannel;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialOutputKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import io.github.createdelight.tetrainsight.integration.tetra.model.TetraProbeSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MaterialDisplayLevelCalibrator {
    private final Map<EntryKey, Double> exactUnits;
    private final Map<FamilyKey, Double> familyUnits;
    private final Map<MaterialOutputKind, Double> kindUnits;
    private final int maximumDisplayLevel;

    private MaterialDisplayLevelCalibrator(Map<EntryKey, Double> exactUnits,
            Map<FamilyKey, Double> familyUnits, Map<MaterialOutputKind, Double> kindUnits,
            int maximumDisplayLevel) {
        this.exactUnits = Map.copyOf(exactUnits);
        this.familyUnits = Map.copyOf(familyUnits);
        this.kindUnits = Map.copyOf(kindUnits);
        this.maximumDisplayLevel = maximumDisplayLevel;
    }

    public static MaterialDisplayLevelCalibrator from(TetraProbeSnapshot snapshot) {
        Map<EntryKey, List<Double>> exactSamples = new HashMap<>();
        Map<FamilyKey, List<Double>> familySamples = new HashMap<>();
        Map<MaterialOutputKind, List<Double>> kindSamples = new HashMap<>();
        int maximumLevel = 1;

        for (MaterialSchematicSnapshot schematic : snapshot.materialSchematics()) {
            if (!schematic.hasAuthorTranslation()) {
                continue;
            }

            List<MaterialTranslationEntry> actualEntries = snapshot.linkedDefinitions(schematic).stream()
                    .map(MaterialDefinitionSnapshot::generatedTranslation)
                    .flatMap(translation -> translation.entries().stream())
                    .toList();

            for (MaterialTranslationEntry authorEntry : schematic.displayTranslation().entries()) {
                maximumLevel = Math.max(maximumLevel, Math.abs(authorEntry.generatedDisplayLevel()));
                actualEntries.stream()
                        .filter(actual -> MaterialTranslationResolver.sameOutput(authorEntry, actual))
                        .filter(actual -> Math.signum(actual.actualCoefficient())
                                == Math.signum(authorEntry.generatedDisplayLevel()))
                        .max(Comparator.comparingDouble(actual -> Math.abs(actual.actualCoefficient())))
                        .ifPresent(actual -> {
                            double unit = Math.abs(actual.actualCoefficient())
                                    / Math.abs(authorEntry.generatedDisplayLevel());
                            if (Double.isFinite(unit) && unit > 0) {
                                exactSamples.computeIfAbsent(EntryKey.from(actual), ignored -> new ArrayList<>()).add(unit);
                                familySamples.computeIfAbsent(FamilyKey.from(actual), ignored -> new ArrayList<>()).add(unit);
                                kindSamples.computeIfAbsent(actual.outputKind(), ignored -> new ArrayList<>()).add(unit);
                            }
                        });
            }
        }

        return new MaterialDisplayLevelCalibrator(
                medians(exactSamples), medians(familySamples), medians(kindSamples), maximumLevel);
    }

    public int displayLevel(MaterialTranslationEntry entry) {
        double coefficient = entry.actualCoefficient();
        if (!Double.isFinite(coefficient) || coefficient == 0) {
            return 0;
        }

        double unit = exactUnits.getOrDefault(EntryKey.from(entry),
                familyUnits.getOrDefault(FamilyKey.from(entry),
                        kindUnits.getOrDefault(entry.outputKind(), Math.abs(coefficient))));
        int magnitude = (int) Math.round(Math.abs(coefficient) / unit);
        magnitude = Math.max(1, Math.min(maximumDisplayLevel, magnitude));
        return coefficient > 0 ? magnitude : -magnitude;
    }

    private static <K> Map<K, Double> medians(Map<K, List<Double>> samples) {
        Map<K, Double> result = new HashMap<>();
        samples.forEach((key, values) -> {
            List<Double> sorted = values.stream().sorted().toList();
            int middle = sorted.size() / 2;
            double median = sorted.size() % 2 == 0
                    ? (sorted.get(middle - 1) + sorted.get(middle)) / 2
                    : sorted.get(middle);
            result.put(key, median);
        });
        return result;
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

    private record FamilyKey(
            MaterialInputChannel input,
            MaterialOutputKind outputKind,
            String operation
    ) {
        private static FamilyKey from(MaterialTranslationEntry entry) {
            return new FamilyKey(entry.input(), entry.outputKind(), entry.operation());
        }
    }
}
