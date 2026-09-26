package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.mixin.tetra.BasicStatSorterAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterAttributeAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectEfficiencyAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectLevelAccessor;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialCandidateSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialOutputKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.data.StatSorterStore;
import se.mickelus.tetra.gui.stats.getter.IStatFormat;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.getter.StatFormat;
import se.mickelus.tetra.gui.stats.getter.StatGetterAttribute;
import se.mickelus.tetra.gui.stats.getter.StatGetterAttributeAddition;
import se.mickelus.tetra.gui.stats.getter.StatGetterAttributeMultiply;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectEfficiency;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectLevel;
import se.mickelus.tetra.gui.stats.sorting.BasicStatSorter;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.StatSorters;
import se.mickelus.tetra.module.Priority;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the effective sorter list for a schematic popover, split into two
 * visually separated groups:
 *
 * <ul>
 *   <li><b>Module-provided</b> (top, {@link Priority#HIGHER}): every attribute
 *   and effect dimension declared by the schematic's linked {@code extract}
 *   entries. These are the module's stated scaling contract, so they replace
 *   any stock sorter with the same semantics — stock attribute sorters are
 *   invisible to {@code shouldShow} on pure-multiplier stats (e.g. curio
 *   modules that only multiply {@code generic.attack_speed}).</li>
 *   <li><b>Material-provided</b> ({@link Priority#HIGH}): attributes and
 *   effect dimensions carried by the schematic's candidate materials, kept
 *   only when the value varies across candidates. A flat union over hundreds
 *   of materials would flood the popover; restricting to differentiating
 *   stats keeps the section small and every entry meaningful.</li>
 * </ul>
 *
 * <p>Tetra's own sorters keep {@link Priority#BASE} and always sit below both
 * groups, except the ones semantically replaced by generated module sorters.
 */
public final class ContextualSorterFactory {
    private ContextualSorterFactory() {
    }

    /**
     * Per-update context: generated sorters grouped by origin, plus the
     * semantic keys/names they claim for suppressing replaced base sorters.
     */
    public record MergeContext(
            List<IStatSorter> moduleSorters,
            List<IStatSorter> materialSorters,
            Set<String> claimedSemanticKeys,
            Set<String> claimedNames) {

        public boolean suppresses(IStatSorter sorter) {
            String key = existingSemanticKey(sorter);
            return (key != null && claimedSemanticKeys.contains(key))
                    || claimedNames.contains(sorter.getName());
        }

        /**
         * Full popover contribution: generated sorters in display order, with
         * section headers interleaved. Actual position is decided by Tetra's
         * priority comparator; headers sit one priority step above their group.
         */
        public List<IStatSorter> popoverEntries() {
            List<IStatSorter> entries = new ArrayList<>(
                    moduleSorters.size() + materialSorters.size() + 2);
            if (!moduleSorters.isEmpty()) {
                entries.add(new SortSectionHeader(
                        I18n.get("tetra_insight.holo.sort.group_module"),
                        Priority.HIGHEST));
                entries.addAll(moduleSorters);
            }
            if (!materialSorters.isEmpty()) {
                entries.add(new SortSectionHeader(
                        I18n.get("tetra_insight.holo.sort.group_material"),
                        Priority.HIGH));
                entries.addAll(materialSorters);
            }
            return entries;
        }

        public int generatedCount() {
            return moduleSorters.size() + materialSorters.size();
        }
    }

    /**
     * Computes generated sorters for this update. Called once per
     * HoloSortButton.update and consulted by the three base-source redirects.
     */
    public static MergeContext compute(List<MaterialTranslationEntry> actualMaterialScaling,
            MaterialSchematicSnapshot schematic) {
        Set<String> extractAttributes = new TreeSet<>();
        Set<String> extractLevelEffects = new TreeSet<>();
        Set<String> extractEfficiencyEffects = new TreeSet<>();
        for (MaterialTranslationEntry entry : actualMaterialScaling) {
            switch (entry.outputKind()) {
                case ATTRIBUTE -> extractAttributes.add(entry.outputId());
                case EFFECT_LEVEL -> extractLevelEffects.add(entry.outputId());
                case EFFECT_EFFICIENCY -> extractEfficiencyEffects.add(entry.outputId());
                default -> {
                }
            }
        }

        Map<String, Set<String>> varyingAttributes = new HashMap<>();
        Map<String, Set<Float>> varyingEffectLevels = new HashMap<>();
        Map<String, Set<Float>> varyingEffectEfficiencies = new HashMap<>();
        if (schematic != null) {
            Set<MaterialCandidateSnapshot> candidates = new LinkedHashSet<>();
            schematic.materialSlots().forEach(slot -> candidates.addAll(slot.candidates()));
            for (MaterialCandidateSnapshot candidate : candidates) {
                for (var attribute : candidate.attributes()) {
                    varyingAttributes.computeIfAbsent(attribute.attributeId(),
                                    key -> new LinkedHashSet<>())
                            .add(attribute.operation() + ":" + attribute.amount());
                }
                for (var effect : candidate.effects()) {
                    varyingEffectLevels.computeIfAbsent(effect.effectId(), key -> new TreeSet<>())
                            .add(effect.level());
                    varyingEffectEfficiencies.computeIfAbsent(effect.effectId(), key -> new TreeSet<>())
                            .add(effect.efficiency());
                }
            }
        }

        List<IStatSorter> moduleSorters = new ArrayList<>();
        List<IStatSorter> materialSorters = new ArrayList<>();
        Set<String> claimedSemanticKeys = new LinkedHashSet<>();
        Set<String> claimedNames = new LinkedHashSet<>();

        // material attributes only earn a sorter when a real choice exists
        Set<String> materialAttributes = new TreeSet<>();
        for (var entry : varyingAttributes.entrySet()) {
            if (entry.getValue().size() > 1 && !extractAttributes.contains(entry.getKey())) {
                materialAttributes.add(entry.getKey());
            }
        }
        for (String attributeId : extractAttributes) {
            addAttributeSorter(attributeId, Priority.HIGHER, moduleSorters,
                    claimedSemanticKeys, claimedNames);
        }
        for (String attributeId : materialAttributes) {
            addAttributeSorter(attributeId, Priority.HIGH, materialSorters,
                    claimedSemanticKeys, claimedNames);
        }

        Set<String> moduleEffectKeys = new TreeSet<>(extractLevelEffects);
        moduleEffectKeys.addAll(extractEfficiencyEffects);
        for (String effectKey : moduleEffectKeys) {
            addEffectSorters(effectKey, ItemEffect.get(effectKey),
                    extractLevelEffects.contains(effectKey),
                    extractEfficiencyEffects.contains(effectKey),
                    Priority.HIGHER, moduleSorters, claimedSemanticKeys, claimedNames);
        }
        Set<String> materialEffectKeys = new TreeSet<>();
        varyingEffectLevels.keySet().stream()
                .filter(key -> varyingEffectLevels.get(key).size() > 1
                        || varyingEffectEfficiencies.getOrDefault(key, Set.of()).size() > 1)
                .filter(key -> !extractLevelEffects.contains(key)
                        && !extractEfficiencyEffects.contains(key))
                .forEach(materialEffectKeys::add);
        for (String effectKey : materialEffectKeys) {
            addEffectSorters(effectKey, ItemEffect.get(effectKey),
                    varyingEffectLevels.getOrDefault(effectKey, Set.of()).size() > 1,
                    varyingEffectEfficiencies.getOrDefault(effectKey, Set.of()).size() > 1,
                    Priority.HIGH, materialSorters, claimedSemanticKeys, claimedNames);
        }

        return new MergeContext(List.copyOf(moduleSorters), List.copyOf(materialSorters),
                claimedSemanticKeys, claimedNames);
    }

    /**
     * Filters one base sorter source (static list, derived list or datapack
     * store): drops sorters that generated entries replace. Pass {@code null}
     * when this button has no contextual context yet.
     */
    public static List<IStatSorter> filterBase(List<IStatSorter> base, MergeContext context) {
        if (context == null) {
            return base;
        }
        List<IStatSorter> filtered = new ArrayList<>(base.size());
        for (IStatSorter sorter : base) {
            if (!context.suppresses(sorter)) {
                filtered.add(sorter);
            }
        }
        return filtered;
    }

    /**
     * Single-shot merge used by callers that already hold a computed context:
     * the derived redirect returns the generated entries plus the filtered
     * base list; Tetra's own comparator applies the section ordering.
     */
    public static List<IStatSorter> mergeWithDerived(List<IStatSorter> originalDerived,
            MergeContext context) {
        if (context == null || context.generatedCount() == 0) {
            return originalDerived;
        }
        List<IStatSorter> result = new ArrayList<>(
                originalDerived.size() + context.generatedCount() + 2);
        result.addAll(context.popoverEntries());
        result.addAll(filterBase(originalDerived, context));
        return List.copyOf(result);
    }

    private static void addAttributeSorter(String attributeId, Priority priority,
            List<IStatSorter> output, Set<String> claimedSemanticKeys, Set<String> claimedNames) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
        if (id == null || attribute == null) {
            return;
        }
        String key = "attribute:" + id;
        String baseName = translatedOrHumanized(attribute.getDescriptionId(), id.getPath());

        // split by operation: a plain "generic.x" entry contributes additively,
        // "**generic.x" contributes multiplicatively. Each generated sorter is
        // shown only when that operation is actually present on a preview, so
        // pure-multiplier stats (curio modules) surface as a ×factor row
        // instead of the unreadable final-attribute value.
        ContextualStatSorter addSorter = new ContextualStatSorter(
                key + ":add", baseName,
                new StatGetterAttributeAddition(attribute, false, false),
                StatFormat.twoDecimal, priority);
        ContextualStatSorter multiplySorter = new ContextualStatSorter(
                key + ":multiply", baseName + " ×",
                new StatGetterAttributeMultiply(attribute, false),
                MULTIPLIER_FORMAT, priority);
        output.add(addSorter);
        output.add(multiplySorter);
        // claim the plain attribute key so stock sorters for this attribute
        // (which merge both ops and hide on zero-base stacks) are replaced
        claimedSemanticKeys.add(key);
        claimedNames.add(baseName);
    }

    private static final IStatFormat MULTIPLIER_FORMAT = new StatFormat("×%.02f");

    private static void addEffectSorters(String effectKey, ItemEffect effect,
            boolean includeLevel, boolean includeEfficiency, Priority priority,
            List<IStatSorter> output, Set<String> claimedSemanticKeys, Set<String> claimedNames) {
        if (effect == null) {
            return;
        }
        String baseKey = "tetra.stats." + effectKey;
        if (includeLevel) {
            ContextualStatSorter sorter = new ContextualStatSorter(
                    "effect_level:" + effectKey,
                    translatedOrHumanized(I18n.exists(baseKey + ".level")
                            ? baseKey + ".level" : baseKey, effectKey),
                    new StatGetterEffectLevel(effect), StatFormat.noDecimal, priority);
            output.add(sorter);
            claimedSemanticKeys.add(sorter.semanticKey());
            claimedNames.add(sorter.getName());
        }
        if (includeEfficiency) {
            String efficiencyName = I18n.exists(baseKey + ".efficiency")
                    ? I18n.get(baseKey + ".efficiency")
                    : translatedOrHumanized(baseKey, effectKey) + " "
                            + I18n.get("tetra.stats.efficiency_suffix");
            ContextualStatSorter sorter = new ContextualStatSorter(
                    "effect_efficiency:" + effectKey, efficiencyName,
                    new StatGetterEffectEfficiency(effect), StatFormat.oneDecimal, priority);
            output.add(sorter);
            claimedSemanticKeys.add(sorter.semanticKey());
            claimedNames.add(sorter.getName());
        }
    }

    private static String semanticKey(IStatSorter sorter) {
        if (sorter instanceof ContextualStatSorter contextual) {
            return contextual.semanticKey();
        }
        if (sorter instanceof SortSectionHeader) {
            return null;
        }
        if (!(sorter instanceof BasicStatSorter) || !(sorter instanceof BasicStatSorterAccessor accessor)) {
            return null;
        }

        IStatGetter getter = accessor.tetraInsight$getGetter();
        if (getter instanceof StatGetterAttribute
                && getter instanceof StatGetterAttributeAccessor attributeAccessor) {
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attributeAccessor.tetraInsight$getAttribute());
            return id != null ? "attribute:" + id : null;
        }
        if (getter instanceof StatGetterEffectLevel
                && getter instanceof StatGetterEffectLevelAccessor effectAccessor) {
            return "effect_level:" + effectAccessor.tetraInsight$getEffect().getKey();
        }
        if (getter instanceof StatGetterEffectEfficiency
                && getter instanceof StatGetterEffectEfficiencyAccessor effectAccessor) {
            return "effect_efficiency:" + effectAccessor.tetraInsight$getEffect().getKey();
        }
        return null;
    }

    /** Diagnostics hook: exposes the semantic key resolution for existing sorters. */
    public static String existingSemanticKey(IStatSorter sorter) {
        return semanticKey(sorter);
    }

    private static String translatedOrHumanized(String key, String fallback) {
        String translated = I18n.get(key);
        if (!translated.equals(key)) {
            return translated;
        }
        String[] parts = fallback.split("[./_]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return builder.length() == 0 ? key : builder.toString();
    }

    /**
     * Diagnostics: semantic keys of the sorters the extract data would claim.
     */
    public static List<String> contextualPreviewNames(List<MaterialTranslationEntry> actualMaterialScaling) {
        List<String> names = new ArrayList<>();
        for (MaterialTranslationEntry entry : actualMaterialScaling) {
            switch (entry.outputKind()) {
                case ATTRIBUTE -> names.add("attribute:" + entry.outputId());
                case EFFECT_LEVEL, EFFECT_EFFICIENCY -> names.add("effect:" + entry.outputId());
                default -> {
                }
            }
        }
        return names;
    }
}
