package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialAttributeSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialCandidateSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialEffectSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialItemSource;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSlotSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialToolSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.module.data.MaterialData;
import se.mickelus.tetra.module.schematic.MaterialOutcomeDefinition;
import se.mickelus.tetra.module.schematic.OutcomeMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MaterialCandidateEnumerator {
    private MaterialCandidateEnumerator() {
    }

    public static List<MaterialSlotSnapshot> enumerate(List<MaterialOutcomeDefinition> outcomes) {
        Map<Integer, LinkedHashMap<String, MaterialCandidateSnapshot>> candidatesBySlot = new LinkedHashMap<>();

        for (MaterialOutcomeDefinition outcome : outcomes) {
            LinkedHashMap<String, MaterialCandidateSnapshot> slotCandidates = candidatesBySlot.computeIfAbsent(
                    outcome.materialSlot, ignored -> new LinkedHashMap<>());

            for (ResourceLocation selector : outcome.materials) {
                for (MaterialData material : resolveMaterials(selector)) {
                    if (material == null || material.material == null || material.hiddenOutcomes) {
                        continue;
                    }

                    MaterialCandidateSnapshot candidate = createCandidate(outcome, selector, material);
                    slotCandidates.merge(candidate.materialKey(), candidate, MaterialCandidateEnumerator::mergeCandidate);
                }
            }
        }

        return candidatesBySlot.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MaterialSlotSnapshot(entry.getKey(), entry.getValue().values().stream()
                        .sorted(Comparator.comparing(MaterialCandidateSnapshot::materialKey))
                        .toList()))
                .toList();
    }

    private static Collection<MaterialData> resolveMaterials(ResourceLocation selector) {
        if (selector.getPath().endsWith("/")) {
            return DataManager.instance.materialData.getDataIn(selector);
        }

        return Optional.ofNullable(DataManager.instance.materialData.getData(selector))
                .map(List::of)
                .orElseGet(List::of);
    }

    private static MaterialCandidateSnapshot createCandidate(MaterialOutcomeDefinition outcome,
            ResourceLocation selector, MaterialData material) {
        OutcomeMaterial requiredMaterial = material.material.offsetCount(outcome.countFactor, outcome.countOffset);
        List<MaterialItemSource> sources = Arrays.stream(requiredMaterial.getApplicableItemStacks())
                .map(MaterialCandidateEnumerator::toSource)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(MaterialCandidateEnumerator::sourceIdentity, Function.identity(), (left, right) -> left,
                                LinkedHashMap::new),
                        map -> List.copyOf(map.values())));

        String materialKey = Optional.ofNullable(material.key)
                .orElseGet(() -> sources.stream().findFirst()
                        .map(MaterialItemSource::itemId)
                        .orElse(selector.toString()));

        return new MaterialCandidateSnapshot(
                materialKey,
                material.category,
                outcome.materialSlot,
                requiredMaterial.count,
                material.primary,
                material.secondary,
                material.tertiary,
                material.durability,
                material.integrityGain,
                material.integrityCost,
                material.magicCapacity,
                material.toolLevel,
                material.toolEfficiency,
                material.tints != null ? material.tints.glyph : 0xffffff,
                material.hidden,
                sources,
                snapshotAttributes(material),
                snapshotEffects(material),
                snapshotTools(material),
                material.features != null ? Arrays.asList(material.features.clone()) : List.of(),
                material.improvements != null ? material.improvements : Map.of(),
                List.of(selector)
        );
    }

    private static MaterialItemSource toSource(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        return new MaterialItemSource(itemId.toString(), stack.getCount(), stack.hasTag() ? stack.getTag().toString() : "");
    }

    private static List<MaterialAttributeSnapshot> snapshotAttributes(MaterialData material) {
        if (material.attributes == null) {
            return List.of();
        }

        return material.attributes.entries().stream()
                .map(entry -> {
                    ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
                    AttributeModifier modifier = entry.getValue();
                    return new MaterialAttributeSnapshot(
                            attributeId != null ? attributeId.toString() : entry.getKey().getDescriptionId(),
                            modifier.getOperation().name(),
                            modifier.getAmount());
                })
                .toList();
    }

    private static List<MaterialEffectSnapshot> snapshotEffects(MaterialData material) {
        if (material.effects == null) {
            return List.of();
        }

        Set<ItemEffect> effects = new LinkedHashSet<>();
        effects.addAll(material.effects.levelMap.keySet());
        effects.addAll(material.effects.efficiencyMap.keySet());
        return effects.stream()
                .map(effect -> new MaterialEffectSnapshot(
                        effect.getKey(),
                        material.effects.levelMap.getOrDefault(effect, 0f),
                        material.effects.efficiencyMap.getOrDefault(effect, 0f)))
                .filter(effect -> effect.level() != 0 || effect.efficiency() != 0)
                .toList();
    }

    private static List<MaterialToolSnapshot> snapshotTools(MaterialData material) {
        if (material.requiredTools == null) {
            return List.of();
        }

        Set<ToolAction> tools = new LinkedHashSet<>();
        tools.addAll(material.requiredTools.levelMap.keySet());
        tools.addAll(material.requiredTools.efficiencyMap.keySet());
        return tools.stream()
                .map(tool -> new MaterialToolSnapshot(
                        tool.name(),
                        material.requiredTools.levelMap.getOrDefault(tool, 0f),
                        material.requiredTools.efficiencyMap.getOrDefault(tool, 0f)))
                .toList();
    }

    private static MaterialCandidateSnapshot mergeCandidate(MaterialCandidateSnapshot left,
            MaterialCandidateSnapshot right) {
        return new MaterialCandidateSnapshot(
                left.materialKey(),
                left.category(),
                left.slotIndex(),
                right.requiredQuantity(),
                right.primary(),
                right.secondary(),
                right.tertiary(),
                right.durability(),
                right.integrityGain(),
                right.integrityCost(),
                right.magicCapacity(),
                right.toolLevel(),
                right.toolEfficiency(),
                right.glyphTint(),
                left.hiddenInGlobalMaterialBrowser() && right.hiddenInGlobalMaterialBrowser(),
                mergeDistinct(left.sourceItems(), right.sourceItems(), MaterialCandidateEnumerator::sourceIdentity),
                mergeDistinct(left.attributes(), right.attributes(), MaterialCandidateEnumerator::attributeIdentity),
                mergeDistinct(left.effects(), right.effects(), MaterialEffectSnapshot::effectId),
                mergeDistinct(left.requiredTools(), right.requiredTools(), MaterialToolSnapshot::toolId),
                mergeDistinct(left.features(), right.features(), Function.identity()),
                mergeMaps(left.improvements(), right.improvements()),
                mergeDistinct(left.matchedSelectors(), right.matchedSelectors(), ResourceLocation::toString)
        );
    }

    private static <T> List<T> mergeDistinct(List<T> left, List<T> right, Function<T, String> identity) {
        LinkedHashMap<String, T> result = new LinkedHashMap<>();
        left.forEach(value -> result.put(identity.apply(value), value));
        right.forEach(value -> result.put(identity.apply(value), value));
        return List.copyOf(result.values());
    }

    private static Map<String, Integer> mergeMaps(Map<String, Integer> left, Map<String, Integer> right) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>(left);
        right.forEach((key, value) -> result.merge(key, value, Math::max));
        return Map.copyOf(result);
    }

    private static String sourceIdentity(MaterialItemSource source) {
        return source.itemId() + "|" + source.nbt() + "|" + source.count();
    }

    private static String attributeIdentity(MaterialAttributeSnapshot attribute) {
        return attribute.attributeId() + "|" + attribute.operation() + "|" + attribute.amount();
    }
}
