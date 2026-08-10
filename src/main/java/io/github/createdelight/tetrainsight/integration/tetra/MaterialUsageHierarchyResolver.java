package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialImprovementUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialItemSource;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialItemUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialModuleUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialStatPreviewSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageGlyphSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageNavigationSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageTreeSnapshot;
import io.github.createdelight.tetrainsight.mixin.tetra.AndRequirementAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.ConfigSchematicAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.NotRequirementAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.OrRequirementAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.resources.language.I18n;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.ModularBladedItem;
import se.mickelus.tetra.items.modular.impl.ModularDoubleHeadedItem;
import se.mickelus.tetra.items.modular.impl.ModularSingleHeadedItem;
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;
import se.mickelus.tetra.items.modular.impl.shield.ModularShieldItem;
import se.mickelus.tetra.items.modular.impl.toolbelt.ModularToolbeltItem;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.ConfigSchematic;
import se.mickelus.tetra.module.schematic.CraftingContext;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.SchematicType;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.requirement.AndRequirement;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;
import se.mickelus.tetra.module.schematic.requirement.ModuleRequirement;
import se.mickelus.tetra.module.schematic.requirement.NotRequirement;
import se.mickelus.tetra.module.schematic.requirement.OrRequirement;

public final class MaterialUsageHierarchyResolver {
    private static final Object LOCK = new Object();
    private static Map<String, MaterialUsageTreeSnapshot> cache = Map.of();
    private static Map<String, MaterialUsageTreeSnapshot> specialCache = Map.of();

    private MaterialUsageHierarchyResolver() {
    }

    public static void clear() {
        synchronized (LOCK) {
            cache = Map.of();
            specialCache = Map.of();
        }
    }

    public static MaterialUsageTreeSnapshot resolve(MaterialProfileSnapshot profile) {
        synchronized (LOCK) {
            MaterialUsageTreeSnapshot cached = cache.get(profile.materialKey());
            if (cached != null) {
                return cached;
            }
        }

        Set<String> usageKeys = MaterialInsightIndex.findUsages(profile.materialKey()).stream()
                .map(MaterialUsageSnapshot::schematicKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        MaterialUsageTreeSnapshot resolved = build(
                usageKeys,
                profile.glyphTint(),
                (schematic, preview) -> matchesProfile(
                        schematic, preview, profile));
        synchronized (LOCK) {
            LinkedHashMap<String, MaterialUsageTreeSnapshot> next = new LinkedHashMap<>(cache);
            next.put(profile.materialKey(), resolved);
            cache = Map.copyOf(next);
        }
        return resolved;
    }

    public static MaterialUsageTreeSnapshot resolveSpecial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new MaterialUsageTreeSnapshot(List.of());
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String cacheKey = (itemId != null ? itemId.toString() : stack.getDescriptionId())
                + "|" + (stack.hasTag() ? stack.getTag() : "");
        synchronized (LOCK) {
            MaterialUsageTreeSnapshot cached = specialCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        Set<String> usageKeys = new LinkedHashSet<>(
                TetraDataProbe.findSpecialMaterialSchematicKeys(stack));
        MaterialUsageTreeSnapshot resolved = build(
                usageKeys,
                null,
                (schematic, preview) -> matchesStack(preview, stack));
        synchronized (LOCK) {
            LinkedHashMap<String, MaterialUsageTreeSnapshot> next =
                    new LinkedHashMap<>(specialCache);
            next.put(cacheKey, resolved);
            specialCache = Map.copyOf(next);
        }
        return resolved;
    }

    private static MaterialUsageTreeSnapshot build(
            Set<String> usageKeys,
            Integer glyphTint,
            BiPredicate<UpgradeSchematic, OutcomePreview> materialMatcher
    ) {
        if (usageKeys.isEmpty()) {
            return new MaterialUsageTreeSnapshot(List.of());
        }

        Map<String, UpgradeSchematic> targets = resolveTargets(usageKeys);
        List<MaterialItemUsageSnapshot> items = ForgeRegistries.ITEMS.getValues().stream()
                .filter(IModularItem.class::isInstance)
                .map(item -> buildItem(
                        (IModularItem) item,
                        glyphTint,
                        materialMatcher,
                        usageKeys,
                        targets))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MaterialItemUsageSnapshot::name))
                .toList();
        return new MaterialUsageTreeSnapshot(items);
    }

    private static MaterialItemUsageSnapshot buildItem(
            IModularItem modularItem,
            Integer glyphTint,
            BiPredicate<UpgradeSchematic, OutcomePreview> materialMatcher,
            Set<String> usageKeys,
            Map<String, UpgradeSchematic> targets
    ) {
        ItemStack baseStack = modularItem.getDefaultStack();
        if (baseStack == null || baseStack.isEmpty()) {
            return null;
        }

        List<MaterialModuleUsageSnapshot> modules = new ArrayList<>();
        for (String slot : slots(modularItem, baseStack)) {
            LinkedHashMap<String, UpgradeSchematic> roots = new LinkedHashMap<>();
            Arrays.stream(SchematicRegistry.getSchematics(baseStack, slot))
                    .forEach(schematic -> roots.putIfAbsent(schematic.getKey(), schematic));
            targets.values().stream()
                    .filter(schematic -> schematic.isRelevant(baseStack))
                    .filter(schematic -> schematic.isApplicableForSlot(slot, baseStack))
                    .forEach(schematic -> roots.putIfAbsent(schematic.getKey(), schematic));

            for (UpgradeSchematic root : roots.values()) {
                List<OutcomePreview> modulePreviews = previews(root, baseStack, slot).stream()
                        .filter(MaterialUsageHierarchyResolver::createsModule)
                        .toList();
                if (modulePreviews.isEmpty()) {
                    continue;
                }

                String rootCanonicalKey = canonicalKey(root.getKey());
                boolean direct = usageKeys.contains(rootCanonicalKey);
                OutcomePreview directPreview = direct
                        ? modulePreviews.stream()
                                .filter(preview -> materialMatcher.test(root, preview))
                                .findFirst()
                                .orElse(null)
                        : null;
                List<MaterialImprovementUsageSnapshot> improvements = new ArrayList<>();
                for (UpgradeSchematic target : targets.values()) {
                    if (Objects.equals(canonicalKey(target.getKey()), rootCanonicalKey)
                            || target.getType() != SchematicType.improvement) {
                        continue;
                    }
                    findApplicableImprovementPreview(
                            target, modulePreviews, slot, materialMatcher).ifPresent(resolved ->
                            improvements.add(new MaterialImprovementUsageSnapshot(
                                    schematicName(target),
                                    MaterialUsageGlyphSnapshot.from(
                                            target.getGlyph(), glyphTint),
                                    MaterialUsageNavigationSnapshot.improvement(
                                            resolved.parent().itemStack,
                                            slot,
                                            target.getKey(),
                                            root.getKey()),
                                    new MaterialStatPreviewSnapshot(
                                            resolved.parent().itemStack,
                                            resolved.preview().itemStack))));
                }

                if (!direct && improvements.isEmpty()) {
                    continue;
                }
                improvements.sort(Comparator.comparing(MaterialImprovementUsageSnapshot::name));
                modules.add(new MaterialModuleUsageSnapshot(
                        slotName(slot),
                        schematicName(root),
                        MaterialUsageGlyphSnapshot.from(
                                root.getGlyph(), direct ? glyphTint : null),
                        MaterialUsageNavigationSnapshot.schematic(
                                baseStack, slot, root.getKey()),
                        directPreview != null
                                ? new MaterialStatPreviewSnapshot(
                                        baseStack, directPreview.itemStack)
                                : null,
                        direct,
                        improvements));
            }
        }

        if (modules.isEmpty()) {
            return null;
        }
        modules.sort(Comparator.comparing(MaterialModuleUsageSnapshot::slotName)
                .thenComparing(MaterialModuleUsageSnapshot::name));
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(baseStack.getItem());
        return new MaterialItemUsageSnapshot(
                itemId != null ? itemId.toString() : baseStack.getDescriptionId(),
                representativeStack(baseStack),
                toolTypeName(modularItem, baseStack),
                modules);
    }

    private static ItemStack representativeStack(ItemStack baseStack) {
        ItemStack replacement = DataManager.instance.replacementData.getData().values().stream()
                .flatMap(Arrays::stream)
                .map(definition -> definition.itemStack)
                .filter(Objects::nonNull)
                .filter(stack -> !stack.isEmpty() && stack.getItem() == baseStack.getItem())
                .findFirst()
                .map(ItemStack::copy)
                .orElse(null);
        if (replacement != null) {
            return replacement;
        }

        IModularItem item = (IModularItem) baseStack.getItem();
        ItemStack assembled = baseStack.copy();
        for (String slot : slots(item, assembled)) {
            ItemStack current = assembled;
            OutcomePreview selected = Arrays.stream(SchematicRegistry.getSchematics(current, slot))
                    .flatMap(schematic -> previews(schematic, current, slot).stream())
                    .filter(MaterialUsageHierarchyResolver::createsModule)
                    .findFirst()
                    .orElse(null);
            if (selected != null) {
                assembled = selected.itemStack.copy();
            }
        }
        return assembled;
    }

    private static String slotName(String slot) {
        String translationKey = "tetra.slot." + slot;
        return I18n.exists(translationKey) ? I18n.get(translationKey) : schematicName(slot);
    }

    private static java.util.Optional<ImprovementPreview> findApplicableImprovementPreview(
            UpgradeSchematic target,
            List<OutcomePreview> parents,
            String slot,
            BiPredicate<UpgradeSchematic, OutcomePreview> materialMatcher
    ) {
        for (OutcomePreview parent : parents) {
            ItemStack stack = parent.itemStack;
            if (stack == null || stack.isEmpty()
                    || !target.isRelevant(stack)
                    || !target.isApplicableForSlot(slot, stack)
                    || !matchesModuleOwnership(target, stack, slot)) {
                continue;
            }
            OutcomePreview preview = previews(target, stack, slot).stream()
                    .filter(candidate -> materialMatcher.test(target, candidate))
                    .findFirst()
                    .orElse(null);
            if (preview != null) {
                return java.util.Optional.of(new ImprovementPreview(parent, preview));
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Matches Tetra's improvement-to-module ownership check without requiring
     * every non-structural requirement (such as empowered state) to be present
     * on the synthetic parent preview.
     */
    private static boolean matchesModuleOwnership(
            UpgradeSchematic schematic,
            ItemStack stack,
            String slot
    ) {
        if (!(schematic instanceof ConfigSchematic config)) {
            return true;
        }

        CraftingRequirement requirement = ((ConfigSchematicAccessor) config)
                .tetraInsight$getDefinition().requirement;
        if (requirement == null) {
            return true;
        }

        CraftingContext context = new CraftingContext(
                null,
                null,
                null,
                null,
                stack,
                slot,
                new ResourceLocation[0]);
        boolean[] moduleState = new boolean[2];
        collectModuleRequirements(requirement, context, false, moduleState);
        return !moduleState[0] || moduleState[1];
    }

    private static void collectModuleRequirements(
            CraftingRequirement requirement,
            CraftingContext context,
            boolean negated,
            boolean[] moduleState
    ) {
        if (requirement == null) {
            return;
        }
        if (requirement instanceof NotRequirement) {
            collectModuleRequirements(
                    ((NotRequirementAccessor) requirement).tetraInsight$getRequirement(),
                    context,
                    !negated,
                    moduleState);
            return;
        }
        if (requirement instanceof AndRequirement) {
            for (CraftingRequirement child : ((AndRequirementAccessor) requirement)
                    .tetraInsight$getRequirements()) {
                collectModuleRequirements(child, context, negated, moduleState);
            }
            return;
        }
        if (requirement instanceof OrRequirement) {
            for (CraftingRequirement child : ((OrRequirementAccessor) requirement)
                    .tetraInsight$getRequirements()) {
                collectModuleRequirements(child, context, negated, moduleState);
            }
            return;
        }
        if (negated || !(requirement instanceof ModuleRequirement)) {
            return;
        }
        moduleState[0] = true;
        moduleState[1] |= requirement.test(context);
    }

    private static Map<String, UpgradeSchematic> resolveTargets(Set<String> usageKeys) {
        LinkedHashMap<String, UpgradeSchematic> result = new LinkedHashMap<>();
        Collection<UpgradeSchematic> all = SchematicRegistry.getAllSchematics();
        for (String usageKey : usageKeys) {
            UpgradeSchematic exact = SchematicRegistry.getSchematic(usageKey);
            if (exact != null) {
                result.putIfAbsent(exact.getKey(), exact);
                continue;
            }
            all.stream()
                    .filter(schematic -> usageKey.equals(canonicalKey(schematic.getKey())))
                    .forEach(schematic -> result.putIfAbsent(schematic.getKey(), schematic));
        }
        return Map.copyOf(result);
    }

    private static Set<String> slots(IModularItem item, ItemStack stack) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        addSlots(result, item.getRequiredModules(stack));
        addSlots(result, item.getMajorModuleKeys(stack));
        addSlots(result, item.getMinorModuleKeys(stack));
        return result;
    }

    private static void addSlots(Set<String> target, String[] slots) {
        if (slots != null) {
            Arrays.stream(slots).filter(Objects::nonNull).forEach(target::add);
        }
    }

    private static List<OutcomePreview> previews(
            UpgradeSchematic schematic,
            ItemStack stack,
            String slot
    ) {
        OutcomePreview[] previews = schematic.getPreviews(stack, slot);
        if (previews == null) {
            return List.of();
        }
        return Arrays.stream(previews)
                .filter(Objects::nonNull)
                .filter(preview -> preview.itemStack != null && !preview.itemStack.isEmpty())
                .toList();
    }

    private static boolean createsModule(OutcomePreview preview) {
        return preview.moduleKey != null && !preview.moduleKey.isBlank();
    }

    private static boolean matchesProfile(
            UpgradeSchematic schematic,
            OutcomePreview preview,
            MaterialProfileSnapshot profile
    ) {
        var resolved = MaterialGlyphTintResolver.resolve(schematic.getKey(), preview);
        if (resolved.isPresent()) {
            return resolved.get().materialKey().equals(profile.materialKey());
        }
        if (preview.materials == null) {
            return false;
        }
        for (ItemStack material : preview.materials) {
            if (material == null || material.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(material.getItem());
            if (itemId == null) {
                continue;
            }
            String nbt = material.hasTag() ? material.getTag().toString() : "";
            for (MaterialItemSource source : profile.sourceItems()) {
                if (source.itemId().equals(itemId.toString())
                        && (source.nbt().isEmpty() || source.nbt().equals(nbt))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesStack(OutcomePreview preview, ItemStack stack) {
        if (preview.materials == null) {
            return false;
        }
        return Arrays.stream(preview.materials)
                .filter(Objects::nonNull)
                .filter(material -> !material.isEmpty())
                .anyMatch(material -> material.getItem() == stack.getItem()
                        && (!material.hasTag()
                                || Objects.equals(material.getTag(), stack.getTag())));
    }

    private static String toolTypeName(IModularItem item, ItemStack stack) {
        String tetraKey = null;
        if (item instanceof ModularSingleHeadedItem) {
            tetraKey = "tetra.holo.craft.modular_single";
        } else if (item instanceof ModularDoubleHeadedItem) {
            tetraKey = "tetra.holo.craft.modular_double";
        } else if (item instanceof ModularBladedItem) {
            tetraKey = "tetra.holo.craft.modular_sword";
        } else if (item instanceof ModularBowItem) {
            tetraKey = "tetra.holo.craft.modular_bow";
        } else if (item instanceof ModularCrossbowItemImpl) {
            tetraKey = "tetra.holo.craft.modular_crossbow";
        } else if (item instanceof ModularShieldItem) {
            tetraKey = "tetra.holo.craft.modular_shield";
        } else if (item instanceof ModularToolbeltItem) {
            tetraKey = "tetra.holo.craft.modular_toolbelt";
        }
        if (tetraKey != null && I18n.exists(tetraKey)) {
            return I18n.get(tetraKey);
        }

        String simpleName = item.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        String armorKey = simpleName.contains("chest")
                ? "tetra.holo.craft.chest"
                : simpleName.contains("head")
                        ? "tetra.holo.craft.head"
                        : simpleName.contains("legs")
                                ? "tetra.holo.craft.legs"
                                : simpleName.contains("feet")
                                        ? "tetra.holo.craft.feet"
                                        : null;
        if (armorKey != null && I18n.exists(armorKey)) {
            return I18n.get(armorKey);
        }
        return stack.getItem().getDescription().getString();
    }

    private record ImprovementPreview(OutcomePreview parent, OutcomePreview preview) {
    }

    private static String canonicalKey(String schematicKey) {
        return TetraDataProbe.canonicalSchematicKey(schematicKey);
    }

    private static String schematicName(UpgradeSchematic schematic) {
        if (schematic.getName() != null && !schematic.getName().isBlank()) {
            return schematic.getName();
        }
        String value = schematic.getKey();
        int separator = value.lastIndexOf('/');
        if (separator >= 0 && separator + 1 < value.length()) {
            value = value.substring(separator + 1);
        }
        return value.replace('_', ' ');
    }

    private static String schematicName(String key) {
        String value = key;
        int separator = value.lastIndexOf('/');
        if (separator >= 0 && separator + 1 < value.length()) {
            value = value.substring(separator + 1);
        }
        return value.replace('_', ' ');
    }
}
