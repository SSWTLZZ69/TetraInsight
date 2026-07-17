package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.mixin.tetra.BasicStatSorterAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterAttributeAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectEfficiencyAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectLevelAccessor;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialOutputKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.data.StatSorterStore;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.getter.StatFormat;
import se.mickelus.tetra.gui.stats.getter.StatGetterAttribute;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectEfficiency;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectLevel;
import se.mickelus.tetra.gui.stats.sorting.BasicStatSorter;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.StatSorters;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class ContextualSorterFactory {
    private ContextualSorterFactory() {
    }

    public static List<IStatSorter> mergeWithDerived(OutcomePreview[] previews, Player player,
            List<IStatSorter> originalDerived,
            List<MaterialTranslationEntry> actualMaterialScaling) {
        if (previews.length == 0 || player == null) {
            return originalDerived;
        }

        Set<String> semanticKeys = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        Stream.concat(
                        Stream.concat(StatSorters.staticSorters.stream(), originalDerived.stream()),
                        Arrays.stream(StatSorterStore.instance.getSorters()))
                .forEach(sorter -> {
                    String semanticKey = semanticKey(sorter);
                    if (semanticKey != null) {
                        semanticKeys.add(semanticKey);
                    }
                    names.add(sorter.getName());
                });

        List<IStatSorter> contextual = new ArrayList<>();
        Set<String> attributeIds = new java.util.TreeSet<>();
        Set<String> effectLevelKeys = new java.util.TreeSet<>();
        Set<String> effectEfficiencyKeys = new java.util.TreeSet<>();
        for (MaterialTranslationEntry entry : actualMaterialScaling) {
            switch (entry.outputKind()) {
                case ATTRIBUTE -> attributeIds.add(entry.outputId());
                case EFFECT_LEVEL -> effectLevelKeys.add(entry.outputId());
                case EFFECT_EFFICIENCY -> effectEfficiencyKeys.add(entry.outputId());
                default -> {
                }
            }
        }

        for (String attributeId : attributeIds) {
            ResourceLocation id = ResourceLocation.tryParse(attributeId);
            Attribute attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
            if (id != null && attribute != null) {
                addAttributeSorter(id, attribute, semanticKeys, names, contextual);
            }
        }

        Set<String> effectKeys = new java.util.TreeSet<>(effectLevelKeys);
        effectKeys.addAll(effectEfficiencyKeys);
        for (String effectKey : effectKeys) {
            addEffectSorters(effectKey, ItemEffect.get(effectKey),
                    effectLevelKeys.contains(effectKey),
                    effectEfficiencyKeys.contains(effectKey),
                    semanticKeys, names, contextual);
        }

        if (contextual.isEmpty()) {
            return originalDerived;
        }
        List<IStatSorter> result = new ArrayList<>(originalDerived.size() + contextual.size());
        result.addAll(originalDerived);
        result.addAll(contextual);
        return List.copyOf(result);
    }

    public static int contextualCount(List<IStatSorter> merged, List<IStatSorter> original) {
        return Math.max(0, merged.size() - original.size());
    }

    private static void addAttributeSorter(ResourceLocation id, Attribute attribute,
            Set<String> semanticKeys, Set<String> names, List<IStatSorter> output) {
        String key = "attribute:" + id;
        if (semanticKeys.contains(key)) {
            return;
        }
        StatGetterAttribute getter = new StatGetterAttribute(attribute);
        ContextualStatSorter sorter = new ContextualStatSorter(
                key, translatedOrHumanized(attribute.getDescriptionId(), id.getPath()),
                getter, StatFormat.twoDecimal);
        addContextual(sorter, semanticKeys, names, output);
    }

    private static void addEffectSorters(String effectKey, ItemEffect effect,
            boolean includeLevel, boolean includeEfficiency,
            Set<String> semanticKeys, Set<String> names, List<IStatSorter> output) {
        String baseKey = "tetra.stats." + effectKey;
        if (includeLevel) {
            ContextualStatSorter levelSorter = new ContextualStatSorter(
                    "effect_level:" + effectKey,
                    translatedOrHumanized(I18n.exists(baseKey + ".level")
                            ? baseKey + ".level" : baseKey, effectKey),
                    new StatGetterEffectLevel(effect), StatFormat.noDecimal);
            addContextual(levelSorter, semanticKeys, names, output);
        }
        if (includeEfficiency) {
            String efficiencyName = I18n.exists(baseKey + ".efficiency")
                    ? I18n.get(baseKey + ".efficiency")
                    : translatedOrHumanized(baseKey, effectKey) + " "
                            + I18n.get("tetra.stats.efficiency_suffix");
            ContextualStatSorter efficiencySorter = new ContextualStatSorter(
                    "effect_efficiency:" + effectKey,
                    efficiencyName,
                    new StatGetterEffectEfficiency(effect), StatFormat.oneDecimal);
            addContextual(efficiencySorter, semanticKeys, names, output);
        }
    }

    private static void addContextual(ContextualStatSorter sorter,
            Set<String> semanticKeys, Set<String> names, List<IStatSorter> output) {
        if (semanticKeys.contains(sorter.semanticKey()) || names.contains(sorter.getName())) {
            return;
        }
        semanticKeys.add(sorter.semanticKey());
        names.add(sorter.getName());
        output.add(sorter);
    }

    private static String semanticKey(IStatSorter sorter) {
        if (sorter instanceof ContextualStatSorter contextual) {
            return contextual.semanticKey();
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

    private static String translatedOrHumanized(String translationKey, String fallback) {
        if (I18n.exists(translationKey)) {
            return I18n.get(translationKey);
        }
        String spaced = fallback.replace('_', ' ').replace('/', ' ');
        if (spaced.isEmpty()) {
            return fallback;
        }
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }
}
