package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialAxis;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialAxisBand;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialItemSource;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.MaterialData;

public final class MaterialInsightIndex {
    private static final Object LOCK = new Object();

    private static Map<String, MaterialProfileSnapshot> profilesByKey = Map.of();
    private static Map<String, List<MaterialProfileSnapshot>> profilesByItem = Map.of();
    private static Map<String, List<MaterialUsageSnapshot>> usagesByMaterial = Map.of();

    private MaterialInsightIndex() {
    }

    public static void rebuild(List<MaterialSchematicSnapshot> schematics) {
        LinkedHashMap<String, MaterialProfileSnapshot> nextProfiles = new LinkedHashMap<>();
        DataManager.instance.materialData.getData().values().stream()
                .filter(Objects::nonNull)
                .filter(material -> material.key != null && material.material != null)
                .map(MaterialInsightIndex::snapshot)
                .forEach(profile -> nextProfiles.putIfAbsent(profile.materialKey(), profile));

        LinkedHashMap<String, LinkedHashMap<String, MaterialUsageSnapshot>> usageBuilders =
                new LinkedHashMap<>();
        for (MaterialSchematicSnapshot schematic : schematics) {
            schematic.materialSlots().forEach(slot -> slot.candidates().forEach(candidate -> {
                MaterialUsageSnapshot usage = new MaterialUsageSnapshot(
                        schematic.schematicKey(),
                        slot.slotIndex(),
                        candidate.requiredQuantity());
                usageBuilders.computeIfAbsent(candidate.materialKey(), ignored -> new LinkedHashMap<>())
                        .putIfAbsent(usage.schematicKey() + "|" + usage.slotIndex(), usage);
            }));
        }

        LinkedHashMap<String, List<MaterialProfileSnapshot>> itemBuilders = new LinkedHashMap<>();
        nextProfiles.values().forEach(profile -> profile.sourceItems().forEach(source ->
                itemBuilders.computeIfAbsent(source.itemId(), ignored -> new ArrayList<>()).add(profile)));

        LinkedHashMap<String, List<MaterialUsageSnapshot>> nextUsages = new LinkedHashMap<>();
        usageBuilders.forEach((key, values) -> nextUsages.put(key, values.values().stream()
                .sorted(Comparator.comparing(MaterialUsageSnapshot::schematicKey)
                        .thenComparingInt(MaterialUsageSnapshot::slotIndex))
                .toList()));

        LinkedHashMap<String, List<MaterialProfileSnapshot>> nextProfilesByItem = new LinkedHashMap<>();
        itemBuilders.forEach((itemId, profiles) -> nextProfilesByItem.put(itemId, profiles.stream()
                .distinct()
                .sorted(Comparator.comparing(MaterialProfileSnapshot::materialKey))
                .toList()));

        synchronized (LOCK) {
            profilesByKey = Map.copyOf(nextProfiles);
            profilesByItem = Map.copyOf(nextProfilesByItem);
            usagesByMaterial = Map.copyOf(nextUsages);
        }
        MaterialUsageHierarchyResolver.clear();
    }

    public static Optional<MaterialProfileSnapshot> findProfile(String materialKey) {
        synchronized (LOCK) {
            return Optional.ofNullable(profilesByKey.get(materialKey));
        }
    }

    public static List<MaterialProfileSnapshot> findProfiles(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return List.of();
        }
        String stackNbt = stack.hasTag() ? stack.getTag().toString() : "";
        synchronized (LOCK) {
            return profilesByItem.getOrDefault(itemId.toString(), List.of()).stream()
                    .filter(profile -> matchesSource(profile, itemId.toString(), stackNbt))
                    .toList();
        }
    }

    public static List<MaterialUsageSnapshot> findUsages(String materialKey) {
        synchronized (LOCK) {
            return usagesByMaterial.getOrDefault(materialKey, List.of());
        }
    }

    public static int usageCount(String materialKey) {
        return findUsages(materialKey).size();
    }

    public static MaterialAxisBand band(MaterialProfileSnapshot profile, MaterialAxis axis) {
        Float value = axisValue(profile, axis);
        if (value == null || !Float.isFinite(value)) {
            return MaterialAxisBand.UNAVAILABLE;
        }

        List<Float> peers;
        synchronized (LOCK) {
            peers = profilesByKey.values().stream()
                    .filter(other -> Objects.equals(other.category(), profile.category()))
                    .map(other -> axisValue(other, axis))
                    .filter(Objects::nonNull)
                    .filter(Float::isFinite)
                    .sorted()
                    .toList();
        }
        if (peers.size() < 3 || Objects.equals(peers.get(0), peers.get(peers.size() - 1))) {
            return MaterialAxisBand.MEDIUM;
        }

        long lower = peers.stream().filter(peer -> peer < value).count();
        double percentile = (double) lower / (peers.size() - 1);
        if (percentile < 0.34D) {
            return MaterialAxisBand.LOW;
        }
        if (percentile > 0.66D) {
            return MaterialAxisBand.HIGH;
        }
        return MaterialAxisBand.MEDIUM;
    }

    private static MaterialProfileSnapshot snapshot(MaterialData material) {
        List<MaterialItemSource> sourceItems = new ArrayList<>();
        Set<String> seenSources = new LinkedHashSet<>();
        for (ItemStack stack : material.material.getApplicableItemStacks()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId == null) {
                continue;
            }
            String nbt = stack.hasTag() ? stack.getTag().toString() : "";
            String identity = itemId + "|" + nbt;
            if (seenSources.add(identity)) {
                sourceItems.add(new MaterialItemSource(itemId.toString(), stack.getCount(), nbt));
            }
        }

        return new MaterialProfileSnapshot(
                material.key,
                Objects.requireNonNullElse(material.category, "misc"),
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
                sourceItems,
                material.attributes != null ? material.attributes.entries().size() : 0,
                material.effects != null ? material.effects.getValues().size() : 0,
                material.aspects != null ? material.aspects.getValues().size() : 0,
                material.features != null ? material.features.length : 0,
                material.improvements != null ? material.improvements.size() : 0
        );
    }

    private static boolean matchesSource(
            MaterialProfileSnapshot profile,
            String itemId,
            String stackNbt
    ) {
        return profile.sourceItems().stream().anyMatch(source ->
                source.itemId().equals(itemId)
                        && (source.nbt().isEmpty() || source.nbt().equals(stackNbt)));
    }

    private static Float axisValue(MaterialProfileSnapshot profile, MaterialAxis axis) {
        return switch (axis) {
            case PRIMARY -> profile.primary();
            case SECONDARY -> profile.secondary();
            case TERTIARY -> profile.tertiary();
        };
    }
}
