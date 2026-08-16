package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialDefinitionSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.FixedConsumableOutcomeSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.FixedConsumableSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSourceKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import io.github.createdelight.tetrainsight.integration.tetra.model.TetraProbeSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;
import io.github.createdelight.tetrainsight.mixin.tetra.OutcomeMaterialAccessor;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.module.data.MaterialImprovementData;
import se.mickelus.tetra.module.data.MaterialVariantData;
import se.mickelus.tetra.module.schematic.MaterialOutcomeDefinition;
import se.mickelus.tetra.module.schematic.OutcomeDefinition;
import se.mickelus.tetra.module.schematic.SchematicDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class TetraDataProbe {
    private static final Object LOCK = new Object();

    private static final List<MaterialDefinitionSnapshot> improvementBuilder = new ArrayList<>();
    private static final List<MaterialDefinitionSnapshot> variantBuilder = new ArrayList<>();
    private static final List<MaterialSchematicSnapshot> schematicBuilder = new ArrayList<>();
    private static final List<FixedConsumableSchematicSnapshot> fixedConsumableBuilder = new ArrayList<>();
    private static final List<SpecialMaterialMatcher> specialMaterialBuilder = new ArrayList<>();
    private static final Map<String, String> schematicAliasBuilder = new LinkedHashMap<>();

    private static List<MaterialDefinitionSnapshot> materialImprovements = List.of();
    private static List<MaterialDefinitionSnapshot> materialVariants = List.of();
    private static List<MaterialSchematicSnapshot> materialSchematics = List.of();
    private static List<FixedConsumableSchematicSnapshot> fixedConsumableSchematics = List.of();
    private static CandidateRuleIndex<Item, SpecialMaterialMatcher> specialMaterialMatcherIndex =
            CandidateRuleIndex.empty();
    private static Map<String, MaterialSchematicSnapshot> materialSchematicsByKey = Map.of();
    private static Map<String, FixedConsumableSchematicSnapshot> fixedConsumablesByKey = Map.of();

    private TetraDataProbe() {
    }

    public static void beginImprovementReload() {
        synchronized (LOCK) {
            improvementBuilder.clear();
        }
    }

    public static void captureImprovement(MaterialImprovementData data) {
        synchronized (LOCK) {
            improvementBuilder.add(new MaterialDefinitionSnapshot(
                    safeKey(data.key),
                    MaterialSourceKind.IMPROVEMENT,
                    Arrays.asList(data.materials.clone()),
                    MaterialTranslationGenerator.fromExtract(safeKey(data.key), data.extract)
            ));
        }
    }

    public static void finishImprovementReload() {
        synchronized (LOCK) {
            materialImprovements = List.copyOf(improvementBuilder);
            TetraInsight.LOGGER.info("Captured {} raw Tetra material improvement definitions", materialImprovements.size());
        }
    }

    public static void beginVariantReload() {
        synchronized (LOCK) {
            variantBuilder.clear();
        }
    }

    public static void captureVariant(MaterialVariantData data) {
        synchronized (LOCK) {
            variantBuilder.add(new MaterialDefinitionSnapshot(
                    safeKey(data.key),
                    MaterialSourceKind.MODULE_VARIANT,
                    Arrays.asList(data.materials.clone()),
                    MaterialTranslationGenerator.fromExtract(safeKey(data.key), data.extract)
            ));
        }
    }

    public static void finishVariantReload() {
        synchronized (LOCK) {
            materialVariants = List.copyOf(variantBuilder);
            TetraInsight.LOGGER.info("Captured {} raw Tetra material module variants", materialVariants.size());
        }
    }

    public static void beginSchematicReload() {
        synchronized (LOCK) {
            schematicBuilder.clear();
            fixedConsumableBuilder.clear();
            specialMaterialBuilder.clear();
            schematicAliasBuilder.clear();
        }
    }

    public static void captureSchematic(SchematicDefinition definition) {
        captureFixedConsumables(definition);

        List<MaterialOutcomeDefinition> materialOutcomes = Arrays.stream(definition.outcomes)
                .filter(MaterialOutcomeDefinition.class::isInstance)
                .map(MaterialOutcomeDefinition.class::cast)
                .toList();

        if (materialOutcomes.isEmpty()) {
            return;
        }

        List<ResourceLocation> selectors = materialOutcomes.stream()
                .flatMap(outcome -> Arrays.stream(outcome.materials))
                .distinct()
                .toList();

        Set<String> improvementPrefixes = new LinkedHashSet<>();
        Set<String> moduleVariantPrefixes = new LinkedHashSet<>();
        for (OutcomeDefinition outcome : materialOutcomes) {
            improvementPrefixes.addAll(outcome.improvements.keySet());
            if (outcome.moduleVariant != null) {
                moduleVariantPrefixes.add(outcome.moduleVariant);
            }
        }

        String schematicKey = safeKey(definition.key);
        synchronized (LOCK) {
            schematicBuilder.add(new MaterialSchematicSnapshot(
                    schematicKey,
                    definition.translation != null,
                    materialOutcomes.size(),
                    selectors,
                    improvementPrefixes,
                    moduleVariantPrefixes,
                    MaterialCandidateEnumerator.enumerate(materialOutcomes),
                    definition.translation != null
                            ? MaterialTranslationGenerator.fromAuthorTranslation(schematicKey, definition.translation)
                            : new io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationInsight(
                                    schematicKey, TranslationProvenance.UNAVAILABLE, List.of())
            ));
            if (definition.keySuffixes != null) {
                Arrays.stream(definition.keySuffixes)
                        .filter(Objects::nonNull)
                        .map(suffix -> schematicKey + suffix)
                        .filter(alias -> !alias.equals(schematicKey))
                        .forEach(alias -> schematicAliasBuilder.putIfAbsent(alias, schematicKey));
            }
        }
    }

    public static void finishSchematicReload() {
        synchronized (LOCK) {
            fixedConsumableSchematics = List.copyOf(fixedConsumableBuilder);
            specialMaterialMatcherIndex = CandidateRuleIndex.build(
                    specialMaterialBuilder,
                    SpecialMaterialMatcher::candidateItems);
            List<MaterialSchematicSnapshot> capturedSchematics = List.copyOf(schematicBuilder);
            TetraProbeSnapshot capturedSnapshot = new TetraProbeSnapshot(
                    materialImprovements, materialVariants, capturedSchematics);
            MaterialDisplayLevelCalibrator calibrator = MaterialDisplayLevelCalibrator.from(capturedSnapshot);
            materialSchematics = capturedSchematics.stream()
                    .map(schematic -> schematic.withDisplayTranslation(MaterialTranslationResolver.resolve(
                            schematic,
                            capturedSnapshot.linkedDefinitions(schematic),
                            calibrator)))
                    .toList();
            LinkedHashMap<String, MaterialSchematicSnapshot> materialIndex = new LinkedHashMap<>();
            materialSchematics.forEach(schematic ->
                    materialIndex.putIfAbsent(schematic.schematicKey(), schematic));
            schematicAliasBuilder.forEach((alias, schematicKey) -> {
                MaterialSchematicSnapshot schematic = materialIndex.get(schematicKey);
                if (schematic != null) {
                    materialIndex.putIfAbsent(alias, schematic);
                }
            });
            materialSchematicsByKey = Map.copyOf(materialIndex);
            MaterialInsightIndex.rebuild(materialSchematics);
            LinkedHashMap<String, FixedConsumableSchematicSnapshot> fixedIndex = new LinkedHashMap<>();
            for (FixedConsumableSchematicSnapshot snapshot : fixedConsumableSchematics) {
                fixedIndex.putIfAbsent(snapshot.schematicKey(), snapshot);
                snapshot.aliases().forEach(alias -> fixedIndex.putIfAbsent(alias, snapshot));
            }
            fixedConsumablesByKey = Map.copyOf(fixedIndex);

            long missing = materialSchematics.stream().filter(snapshot -> !snapshot.hasAuthorTranslation()).count();
            TetraInsight.LOGGER.info("Captured {} raw Tetra material schematics ({} missing translation)",
                    materialSchematics.size(), missing);
            TetraInsight.LOGGER.info("Indexed {} expanded material schematic aliases",
                    schematicAliasBuilder.size());
            TetraInsight.LOGGER.info("Captured {} fixed-consumable schematics",
                    fixedConsumableSchematics.size());
            TetraInsight.LOGGER.info(
                    "Indexed {} special-material predicates across {} candidate items ({} fallback predicates)",
                    specialMaterialMatcherIndex.ruleCount(),
                    specialMaterialMatcherIndex.candidateCount(),
                    specialMaterialMatcherIndex.fallbackRuleCount());
            fixedConsumableSchematics.stream()
                    .filter(snapshot -> snapshot.schematicKey().startsWith("toolbelt/")
                            || snapshot.schematicKey().equals("bow/riser/adjustable_strength"))
                    .forEach(snapshot -> TetraInsight.LOGGER.debug(
                            "Fixed-consumable sample {} aliases={} outcomes={} materialCounts={}",
                            snapshot.schematicKey(), snapshot.aliases(), snapshot.outcomes().size(),
                            snapshot.outcomes().stream()
                                    .map(outcome -> outcome.materials().size())
                                    .toList()));

            TetraProbeSnapshot snapshot = new TetraProbeSnapshot(materialImprovements, materialVariants, materialSchematics);
            TetraInsight.LOGGER.info("Linked {} material schematics to raw extract definitions; {} missing translations have linked definitions",
                    snapshot.linkedMaterialSchematicCount(), snapshot.missingTranslationWithLinkedDefinitionCount());
            long emptyCandidateSets = materialSchematics.stream()
                    .filter(schematic -> schematic.candidateCount() == 0)
                    .count();
            int indexedSlots = materialSchematics.stream()
                    .mapToInt(schematic -> schematic.materialSlots().size())
                    .sum();
            TetraInsight.LOGGER.info(
                    "Indexed {} logical material candidates across {} schematic slots ({} global-browser-hidden candidate entries, {} schematics empty)",
                    snapshot.totalMaterialCandidateCount(), indexedSlots,
                    snapshot.globalHiddenMaterialCandidateCount(), emptyCandidateSets);
            TetraInsight.LOGGER.info(
                    "Resolved material display translations: {} author, {} generated from extract, {} without material scaling, {} unavailable",
                    snapshot.displayTranslationCount(TranslationProvenance.AUTHOR),
                    snapshot.displayTranslationCount(TranslationProvenance.GENERATED_FROM_EXTRACT),
                    snapshot.displayTranslationCount(TranslationProvenance.NO_MATERIAL_SCALING),
                    snapshot.displayTranslationCount(TranslationProvenance.UNAVAILABLE));
            long authorEntryCount = materialSchematics.stream()
                    .filter(MaterialSchematicSnapshot::hasAuthorTranslation)
                    .flatMap(schematic -> schematic.displayTranslation().entries().stream())
                    .count();
            long authorEntriesWithActualCoefficient = materialSchematics.stream()
                    .filter(MaterialSchematicSnapshot::hasAuthorTranslation)
                    .flatMap(schematic -> schematic.displayTranslation().entries().stream())
                    .filter(entry -> Double.isFinite(entry.actualCoefficient()))
                    .count();
            java.util.Map<Integer, Long> generatedLevelDistribution = materialSchematics.stream()
                    .filter(schematic -> schematic.displayTranslation().provenance()
                            == TranslationProvenance.GENERATED_FROM_EXTRACT)
                    .flatMap(schematic -> schematic.displayTranslation().entries().stream())
                    .collect(java.util.stream.Collectors.groupingBy(
                            entry -> entry.generatedDisplayLevel(),
                            java.util.TreeMap::new,
                            java.util.stream.Collectors.counting()));
            TetraInsight.LOGGER.info(
                    "Display translation detail: {}/{} author entries linked to actual coefficients; generated level distribution {}",
                    authorEntriesWithActualCoefficient, authorEntryCount, generatedLevelDistribution);
            if (snapshot.displayTranslationCount(TranslationProvenance.NO_MATERIAL_SCALING) > 0) {
                TetraInsight.LOGGER.info("Material schematics with no material-scaled outputs: {}",
                        materialSchematics.stream()
                                .filter(schematic -> schematic.displayTranslation().provenance()
                                        == TranslationProvenance.NO_MATERIAL_SCALING)
                                .map(MaterialSchematicSnapshot::schematicKey)
                                .toList());
            }
            if (snapshot.displayTranslationCount(TranslationProvenance.UNAVAILABLE) > 0) {
                TetraInsight.LOGGER.warn("Material schematics with unavailable display translations: {}",
                        materialSchematics.stream()
                                .filter(schematic -> schematic.displayTranslation().provenance()
                                        == TranslationProvenance.UNAVAILABLE)
                                .map(schematic -> schematic.schematicKey() + " linked="
                                        + snapshot.linkedDefinitions(schematic).stream()
                                                .map(definition -> definition.ownerKey() + "("
                                                        + definition.generatedTranslation().entries().size() + ")")
                                                .toList())
                                .toList());
            }
            if (emptyCandidateSets > 0) {
                TetraInsight.LOGGER.warn("Material schematics with no visible candidates: {}",
                        materialSchematics.stream()
                                .filter(schematic -> schematic.candidateCount() == 0)
                                .map(schematic -> schematic.schematicKey() + schematic.materialSelectors())
                                .toList());
            }

            materialSchematics.stream()
                    .filter(schematic -> schematic.schematicKey().contains("wrap_handle")
                            || schematic.schematicKey().contains("wrap_hilt"))
                    .findFirst()
                    .ifPresent(schematic -> TetraInsight.LOGGER.info(
                            "Probe sample {} resolved to {} raw definitions and {} generated display entries",
                            schematic.schematicKey(),
                            snapshot.linkedDefinitions(schematic).size(),
                            snapshot.linkedDefinitions(schematic).stream()
                                    .mapToInt(definition -> definition.generatedTranslation().entries().size())
                                    .sum()));

            materialSchematics.stream()
                    .filter(schematic -> schematic.schematicKey().contains("wrap_handle")
                            || schematic.schematicKey().contains("wrap_hilt"))
                    .findFirst()
                    .ifPresent(schematic -> TetraInsight.LOGGER.info(
                            "Candidate sample {} contains {} material slots and {} logical material candidates; glyph tints {}",
                            schematic.schematicKey(), schematic.materialSlots().size(), schematic.candidateCount(),
                            schematic.materialSlots().stream()
                                    .flatMap(slot -> slot.candidates().stream())
                                    .map(candidate -> candidate.materialKey() + "=#"
                                            + String.format("%06X", candidate.glyphTint() & 0xffffff))
                                    .toList()));
        }
    }

    public static TetraProbeSnapshot snapshot() {
        synchronized (LOCK) {
            return new TetraProbeSnapshot(materialImprovements, materialVariants, materialSchematics);
        }
    }

    public static Optional<MaterialSchematicSnapshot> findSchematic(String schematicKey) {
        synchronized (LOCK) {
            return Optional.ofNullable(materialSchematicsByKey.get(schematicKey));
        }
    }

    public static List<MaterialTranslationEntry> findActualMaterialScaling(String schematicKey) {
        synchronized (LOCK) {
            MaterialSchematicSnapshot schematic = materialSchematicsByKey.get(schematicKey);
            if (schematic == null) {
                return List.of();
            }
            TetraProbeSnapshot current = new TetraProbeSnapshot(
                    materialImprovements, materialVariants, materialSchematics);
            return current.linkedDefinitions(schematic).stream()
                    .flatMap(definition -> definition.generatedTranslation().entries().stream())
                    .distinct()
                    .toList();
        }
    }

    public static Optional<FixedConsumableSchematicSnapshot> findFixedConsumableSchematic(String schematicKey) {
        synchronized (LOCK) {
            return Optional.ofNullable(fixedConsumablesByKey.get(schematicKey));
        }
    }

    public static List<String> findSpecialMaterialSchematicKeys(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        List<SpecialMaterialMatcher> candidates;
        synchronized (LOCK) {
            candidates = specialMaterialMatcherIndex.candidates(stack.getItem());
        }
        return candidates.stream()
                .filter(matcher -> matcher.predicate().matches(stack))
                .map(SpecialMaterialMatcher::schematicKey)
                .distinct()
                .sorted()
                .toList();
    }

    public static String canonicalSchematicKey(String schematicKey) {
        synchronized (LOCK) {
            MaterialSchematicSnapshot material = materialSchematicsByKey.get(schematicKey);
            if (material != null) {
                return material.schematicKey();
            }
            FixedConsumableSchematicSnapshot fixed = fixedConsumablesByKey.get(schematicKey);
            return fixed != null ? fixed.schematicKey() : schematicKey;
        }
    }

    private static void captureFixedConsumables(SchematicDefinition definition) {
        List<OutcomeDefinition> fixedOutcomes = Arrays.stream(definition.outcomes)
                .filter(outcome -> !(outcome instanceof MaterialOutcomeDefinition))
                .filter(outcome -> !outcome.hidden)
                .filter(outcome -> outcome.material != null && outcome.material.isValid())
                .toList();
        List<SpecialMaterialMatcher> specialMatchers = new ArrayList<>();
        List<FixedConsumableOutcomeSnapshot> outcomes = fixedOutcomes.stream()
                .map(outcome -> {
                    List<ItemStack> materials = Arrays.stream(
                                    outcome.material.getApplicableItemStacks())
                            .filter(stack -> !stack.isEmpty())
                            .map(ItemStack::copy)
                            .toList();
                    ItemPredicate predicate = outcome.material.getPredicate();
                    if (predicate != null) {
                        Set<Item> candidateItems = materials.stream()
                                .map(ItemStack::getItem)
                                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                        specialMatchers.add(new SpecialMaterialMatcher(
                                safeKey(definition.key), predicate, candidateItems));
                    }
                    net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag =
                            ((OutcomeMaterialAccessor) (Object) outcome.material)
                                    .tetraInsight$getTagLocation();
                    return new FixedConsumableOutcomeSnapshot(
                            materials,
                            tag != null ? tag.location() : null,
                            Math.max(1, outcome.material.count));
                })
                .filter(outcome -> !outcome.materials().isEmpty()
                        || outcome.materialTag() != null)
                .toList();
        if (outcomes.isEmpty()) {
            return;
        }

        String[] keySuffixes = definition.keySuffixes != null
                ? definition.keySuffixes
                : new String[0];
        Set<String> aliases = Arrays.stream(keySuffixes)
                .map(suffix -> safeKey(definition.key) + suffix)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        synchronized (LOCK) {
            fixedConsumableBuilder.add(new FixedConsumableSchematicSnapshot(
                    safeKey(definition.key), aliases, outcomes));
            specialMaterialBuilder.addAll(specialMatchers);
        }
    }

    private record SpecialMaterialMatcher(
            String schematicKey,
            ItemPredicate predicate,
            Set<Item> candidateItems
    ) {
        private SpecialMaterialMatcher {
            candidateItems = Set.copyOf(candidateItems);
        }
    }

    private static String safeKey(String key) {
        return Objects.requireNonNullElse(key, "<unknown>");
    }
}
