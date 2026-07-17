package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.client.HoloDisplaySchematic;
import io.github.createdelight.tetrainsight.client.HoloHoningOutcomeStack;
import io.github.createdelight.tetrainsight.client.HoloHoningTargetAccess;
import io.github.createdelight.tetrainsight.client.HoloImprovementCountAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloVariantDetailGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.OutcomeStack;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.CraftingContext;
import se.mickelus.tetra.module.schematic.ConfigSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.SchematicType;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.requirement.AndRequirement;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;
import se.mickelus.tetra.module.schematic.requirement.HasImprovementRequirement;
import se.mickelus.tetra.module.schematic.requirement.ModuleRequirement;
import se.mickelus.tetra.module.schematic.requirement.NotRequirement;
import se.mickelus.tetra.module.schematic.requirement.OrRequirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = HoloVariantDetailGui.class, remap = false)
public abstract class HoloVariantDetailGuiMixin
        implements HoloImprovementCountAccess, HoloHoningTargetAccess {
    @Shadow
    @Final
    private HoloImprovementListGui improvements;

    @Shadow
    @Final
    private List<OutcomeStack> selectedOutcomes;

    @Shadow
    private OutcomePreview variantOutcome;

    @Shadow
    private OutcomePreview currentOutcome;

    @Shadow
    private String slot;

    @Shadow
    public abstract void updateStats(OutcomePreview variant, OutcomePreview hovered);

    @Unique
    private int tetraInsight$improvementCount;

    @Unique
    private UpgradeSchematic[] tetraInsight$displaySchematics = new UpgradeSchematic[0];

    @Unique
    private ItemStack tetraInsight$honingTarget = ItemStack.EMPTY;

    @Unique
    private ItemStack tetraInsight$displayStack = ItemStack.EMPTY;

    @Unique
    private String tetraInsight$displaySlot = "";

    @Unique
    private final List<HoloHoningOutcomeStack> tetraInsight$selectedHoning = new ArrayList<>();

    @Inject(method = "updateVariant", at = @At("HEAD"), remap = false)
    private void tetraInsight$resetImprovementCount(OutcomePreview variant, OutcomePreview hovered,
            String slot, CallbackInfo ci) {
        tetraInsight$improvementCount = 0;
        tetraInsight$displaySchematics = new UpgradeSchematic[0];
        tetraInsight$displayStack = ItemStack.EMPTY;
        tetraInsight$displaySlot = "";
        tetraInsight$selectedHoning.clear();
    }

    @Inject(method = "onImprovementSelect", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$selectHoningPreview(OutcomeStack selected, CallbackInfo ci) {
        if (!(selected instanceof HoloHoningOutcomeStack honing)) {
            return;
        }

        int selectedIndex = tetraInsight$findHoningSelection(honing);
        if (selectedIndex >= 0) {
            tetraInsight$selectedHoning.remove(selectedIndex);
        } else {
            tetraInsight$selectedHoning.removeIf(selectedHoning ->
                    java.util.Objects.equals(
                            selectedHoning.preview().variantKey,
                            honing.preview().variantKey));
            tetraInsight$selectedHoning.add(honing);
            tetraInsight$selectedHoning.sort(Comparator
                    .comparingInt((HoloHoningOutcomeStack outcome) -> outcome.preview().level)
                    .thenComparing(outcome -> outcome.schematic().getKey()));
        }

        tetraInsight$rebuildCurrentOutcome();
        tetraInsight$refreshHoningSelection();
        ci.cancel();
    }

    @Inject(method = "onImprovementSelect", at = @At("RETURN"), remap = false)
    private void tetraInsight$reapplyHoningAfterOrdinarySelection(
            OutcomeStack selected, CallbackInfo ci) {
        if (tetraInsight$selectedHoning.isEmpty()) {
            return;
        }

        tetraInsight$applyHoningOutcome();
        tetraInsight$refreshHoningSelection();
    }

    @Unique
    private void tetraInsight$rebuildCurrentOutcome() {
        currentOutcome = variantOutcome.clone();
        for (OutcomeStack selected : selectedOutcomes) {
            OutcomePreview resolved = tetraInsight$resolveOutcome(selected, currentOutcome);
            if (resolved != null) {
                currentOutcome = resolved;
            }
        }
        tetraInsight$applyHoningOutcome();
    }

    @Unique
    private void tetraInsight$applyHoningOutcome() {
        for (HoloHoningOutcomeStack selected : tetraInsight$selectedHoning) {
            OutcomePreview resolved = tetraInsight$resolveOutcome(selected, currentOutcome);
            if (resolved != null) {
                currentOutcome = resolved;
            }
        }
    }

    @Unique
    private OutcomePreview tetraInsight$resolveOutcome(
            OutcomeStack selected, OutcomePreview base) {
        OutcomeStackAccessor access = (OutcomeStackAccessor) selected;
        OutcomePreview requested = access.tetraInsight$getPreview();
        for (OutcomePreview candidate : access.tetraInsight$getSchematic()
                .getPreviews(base.itemStack, slot)) {
            if (candidate.equals(requested)) {
                return candidate;
            }
        }
        return null;
    }

    @Unique
    private void tetraInsight$refreshHoningSelection() {
        List<OutcomeStack> displaySelection = new ArrayList<>(selectedOutcomes);
        displaySelection.addAll(tetraInsight$selectedHoning);
        improvements.updateSelection(currentOutcome.itemStack, displaySelection);
        updateStats(currentOutcome, currentOutcome);
    }

    @Unique
    private int tetraInsight$findHoningSelection(HoloHoningOutcomeStack candidate) {
        for (int index = 0; index < tetraInsight$selectedHoning.size(); index++) {
            HoloHoningOutcomeStack selected = tetraInsight$selectedHoning.get(index);
            if (selected.schematic().getKey().equals(candidate.schematic().getKey())
                    && selected.preview().equals(candidate.preview())) {
                return index;
            }
        }
        return -1;
    }

    @Redirect(
            method = "updateVariant",
            at = @At(
                    value = "INVOKE",
                    target = "Lse/mickelus/tetra/module/SchematicRegistry;getPreviewSchematics(Lse/mickelus/tetra/module/schematic/CraftingContext;Z)[Lse/mickelus/tetra/module/schematic/UpgradeSchematic;"),
            remap = false)
    private UpgradeSchematic[] tetraInsight$buildUnifiedImprovementList(
            CraftingContext candidateContext, boolean remote) {
        long started = System.nanoTime();
        UpgradeSchematic[] previewed = SchematicRegistry.getPreviewSchematics(
                candidateContext, remote);
        tetraInsight$displayStack = candidateContext.targetStack.copy();
        tetraInsight$displaySlot = candidateContext.slot;
        Map<UpgradeSchematic, Boolean> moduleOwnershipBySchematic =
                new IdentityHashMap<>();
        Map<String, UpgradeSchematic> ordinary = new LinkedHashMap<>();
        Arrays.stream(previewed)
                .filter(schematic -> schematic.getType() == SchematicType.improvement)
                .filter(schematic -> !schematic.isHoning())
                .filter(HoloVariantDetailGuiMixin::tetraInsight$hasResolvableMaterials)
                .filter(schematic -> moduleOwnershipBySchematic.computeIfAbsent(
                        schematic, value -> tetraInsight$matchesModuleOwnership(
                                value, candidateContext)))
                .forEach(schematic -> ordinary.putIfAbsent(schematic.getKey(), schematic));

        ItemStack actualTarget = tetraInsight$honingTarget.isEmpty()
                ? candidateContext.targetStack
                : tetraInsight$honingTarget;
        CraftingContext actualContext = new CraftingContext(
                candidateContext.world, candidateContext.pos, candidateContext.blockState,
                candidateContext.player, actualTarget, candidateContext.slot,
                candidateContext.unlocks);

        Map<String, UpgradeSchematic> honingFamily = new LinkedHashMap<>();
        java.util.Set<String> honingImprovementKeys = new java.util.LinkedHashSet<>();
        Map<UpgradeSchematic, OutcomePreview[]> previewsBySchematic = new java.util.IdentityHashMap<>();
        List<UpgradeSchematic> relevantCandidates = new ArrayList<>();
        java.util.Collection<UpgradeSchematic> allSchematics = SchematicRegistry.getAllSchematics();
        for (UpgradeSchematic schematic : allSchematics) {
            if (!schematic.isRelevant(candidateContext.targetStack)
                    || !schematic.isApplicableForSlot(
                            candidateContext.slot, candidateContext.targetStack)) {
                continue;
            }
            OutcomePreview[] previews = schematic.getPreviews(
                    candidateContext.targetStack, candidateContext.slot);
            previewsBySchematic.put(schematic, previews);
            if (!tetraInsight$hasResolvableMaterials(schematic)) {
                continue;
            }
            if (!tetraInsight$isAcceptedByCandidate(previews, candidateContext)) {
                continue;
            }
            relevantCandidates.add(schematic);
        }

        for (UpgradeSchematic schematic : relevantCandidates) {
            if (!schematic.isHoning()
                    && schematic.getType() == SchematicType.improvement
                    && moduleOwnershipBySchematic.computeIfAbsent(
                            schematic, value -> tetraInsight$matchesModuleOwnership(
                                    value, candidateContext))
                    && schematic.canPreview(candidateContext, true)) {
                ordinary.putIfAbsent(schematic.getKey(), schematic);
            }
        }

        for (UpgradeSchematic schematic : tetraInsight$selectHoningCandidates(
                relevantCandidates, previewsBySchematic, actualContext)) {
            honingFamily.putIfAbsent(schematic.getKey(), schematic);
            tetraInsight$collectImprovementKeys(
                    previewsBySchematic.get(schematic), honingImprovementKeys);
        }

        for (UpgradeSchematic schematic : relevantCandidates) {
            if (schematic.isHoning()
                    || schematic.getType() != SchematicType.improvement
                    || !moduleOwnershipBySchematic.computeIfAbsent(
                            schematic, value -> tetraInsight$matchesModuleOwnership(
                                    value, candidateContext))
                    || !tetraInsight$hasAnyImprovementKey(
                            previewsBySchematic.get(schematic), honingImprovementKeys)) {
                continue;
            }
            ordinary.putIfAbsent(schematic.getKey(), schematic);
        }

        List<UpgradeSchematic> display = new ArrayList<>();
        ordinary.values().forEach(schematic -> {
            OutcomePreview[] previews = previewsBySchematic.computeIfAbsent(
                    schematic, value -> value.getPreviews(
                            candidateContext.targetStack, candidateContext.slot));
            display.add(HoloDisplaySchematic.wrap(
                    schematic, schematic.matchesRequirements(candidateContext),
                    candidateContext.targetStack, candidateContext.slot, previews));
        });
        honingFamily.values().forEach(schematic -> {
            OutcomePreview[] previews = previewsBySchematic.computeIfAbsent(
                    schematic, value -> value.getPreviews(
                            candidateContext.targetStack, candidateContext.slot));
            display.add(HoloDisplaySchematic.wrap(
                    schematic, schematic.matchesRequirements(actualContext),
                    candidateContext.targetStack, candidateContext.slot, previews));
        });
        tetraInsight$displaySchematics = display.toArray(UpgradeSchematic[]::new);

        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        if (elapsedMillis >= 10) {
            TetraInsight.LOGGER.info(
                    "Built Tetra improvement snapshot for slot {} in {} ms: registry={}, relevant={}, previews={}, ordinary={}, honing={}, total={}",
                    candidateContext.slot, elapsedMillis,
                    allSchematics.size(),
                    relevantCandidates.size(), previewsBySchematic.size(), ordinary.size(),
                    honingFamily.size(), tetraInsight$displaySchematics.length);
        }
        return tetraInsight$displaySchematics;
    }

    @Redirect(
            method = "updateVariant",
            at = @At(
                    value = "INVOKE",
                    target = "Lse/mickelus/tetra/items/modular/impl/holo/gui/craft/HoloImprovementButton;updateCount(I)V"),
            remap = false)
    private void tetraInsight$rememberImprovementCount(HoloImprovementButton button, int count) {
        tetraInsight$improvementCount = tetraInsight$countDisplayGroups(
                tetraInsight$displaySchematics);
        button.updateCount(tetraInsight$improvementCount);
    }

    @Unique
    private int tetraInsight$countDisplayGroups(UpgradeSchematic[] schematics) {
        java.util.Set<String> honingKeys = new java.util.HashSet<>();
        Map<UpgradeSchematic, String> keysBySchematic = new java.util.IdentityHashMap<>();
        for (UpgradeSchematic schematic : schematics) {
            OutcomePreview[] previews = schematic.getPreviews(
                    tetraInsight$displayStack, tetraInsight$displaySlot);
            if (previews.length == 1 && previews[0].variantKey != null
                    && previews[0].level > 0) {
                keysBySchematic.put(schematic, previews[0].variantKey);
                if (schematic.isHoning()) {
                    honingKeys.add(previews[0].variantKey);
                }
            }
        }

        int count = 0;
        java.util.Set<String> countedKeys = new java.util.HashSet<>();
        for (UpgradeSchematic schematic : schematics) {
            String key = keysBySchematic.get(schematic);
            if (key != null && honingKeys.contains(key)) {
                if (countedKeys.add(key)) {
                    count++;
                }
            } else {
                count++;
            }
        }
        return count;
    }

    @Unique
    private static boolean tetraInsight$isAcceptedByCandidate(
            OutcomePreview[] previews, CraftingContext context) {
        if (context.targetMajorModule == null) {
            return false;
        }
        for (OutcomePreview preview : previews) {
            if (preview.variantKey != null
                    && context.targetMajorModule.acceptsImprovementLevel(
                            preview.variantKey, preview.level)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean tetraInsight$hasResolvableMaterials(UpgradeSchematic schematic) {
        if (schematic.getNumMaterialSlots() <= 0 || !(schematic instanceof ConfigSchematic)) {
            return true;
        }
        return io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe
                .findSchematic(schematic.getKey())
                .map(snapshot -> snapshot.candidateCount() > 0)
                .orElseGet(() -> io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe
                        .findFixedConsumableSchematic(schematic.getKey())
                        .isPresent());
    }

    @Unique
    private static List<UpgradeSchematic> tetraInsight$selectHoningCandidates(
            List<UpgradeSchematic> relevantCandidates,
            Map<UpgradeSchematic, OutcomePreview[]> previewsBySchematic,
            CraftingContext context) {
        List<UpgradeSchematic> honing = relevantCandidates.stream()
                .filter(UpgradeSchematic::isHoning)
                .toList();
        Set<String> ordinaryImprovementOutputs = new HashSet<>();
        relevantCandidates.stream()
                .filter(schematic -> !schematic.isHoning())
                .filter(schematic -> schematic.getType() == SchematicType.improvement)
                .forEach(schematic -> tetraInsight$collectImprovementKeys(
                        previewsBySchematic.get(schematic), ordinaryImprovementOutputs));
        Map<UpgradeSchematic, Set<String>> outputsBySchematic = new IdentityHashMap<>();
        Map<UpgradeSchematic, Set<String>> dependenciesBySchematic = new IdentityHashMap<>();
        Map<UpgradeSchematic, boolean[]> moduleStateBySchematic = new IdentityHashMap<>();
        Set<UpgradeSchematic> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> selectedOutputs = new HashSet<>();

        for (UpgradeSchematic schematic : honing) {
            Set<String> outputs = new HashSet<>();
            tetraInsight$collectImprovementKeys(
                    previewsBySchematic.get(schematic), outputs);
            outputsBySchematic.put(schematic, outputs);

            Set<String> dependencies = new HashSet<>();
            boolean[] moduleState = new boolean[2];
            tetraInsight$collectStructuralRequirements(
                    tetraInsight$getRequirement(schematic), context,
                    false, dependencies, moduleState);
            dependenciesBySchematic.put(schematic, dependencies);
            moduleStateBySchematic.put(schematic, moduleState);

            boolean moduleMismatch = moduleState[0] && !moduleState[1];
            boolean followsOrdinaryImprovement = dependencies.stream()
                    .anyMatch(ordinaryImprovementOutputs::contains);
            boolean structuralRoot = dependencies.isEmpty()
                    || (moduleState[0] && moduleState[1])
                    || followsOrdinaryImprovement
                    || schematic.matchesRequirements(context);
            if (!moduleMismatch && structuralRoot) {
                selected.add(schematic);
                selectedOutputs.addAll(outputs);
            }
        }

        boolean changed;
        do {
            changed = false;
            for (UpgradeSchematic schematic : honing) {
                if (selected.contains(schematic)) {
                    continue;
                }
                boolean[] moduleState = moduleStateBySchematic.get(schematic);
                if (moduleState[0] && !moduleState[1]) {
                    continue;
                }
                boolean linkedByDependency = dependenciesBySchematic.get(schematic).stream()
                        .anyMatch(selectedOutputs::contains);
                boolean linkedByOutput = outputsBySchematic.get(schematic).stream()
                        .anyMatch(selectedOutputs::contains);
                if (linkedByDependency || linkedByOutput) {
                    selected.add(schematic);
                    selectedOutputs.addAll(outputsBySchematic.get(schematic));
                    changed = true;
                }
            }
        } while (changed);

        return honing.stream().filter(selected::contains).toList();
    }

    @Unique
    private static CraftingRequirement tetraInsight$getRequirement(
            UpgradeSchematic schematic) {
        if (schematic instanceof ConfigSchematic) {
            return ((ConfigSchematicAccessor) schematic)
                    .tetraInsight$getDefinition().requirement;
        }
        return null;
    }

    @Unique
    private static boolean tetraInsight$matchesModuleOwnership(
            UpgradeSchematic schematic, CraftingContext context) {
        boolean[] moduleState = new boolean[2];
        tetraInsight$collectStructuralRequirements(
                tetraInsight$getRequirement(schematic), context,
                false, new HashSet<>(), moduleState);
        return !moduleState[0] || moduleState[1];
    }

    @Unique
    private static void tetraInsight$collectStructuralRequirements(
            CraftingRequirement requirement, CraftingContext context,
            boolean negated, Set<String> dependencies, boolean[] moduleState) {
        if (requirement == null) {
            return;
        }
        if (requirement instanceof NotRequirement) {
            tetraInsight$collectStructuralRequirements(
                    ((NotRequirementAccessor) requirement).tetraInsight$getRequirement(),
                    context, !negated, dependencies, moduleState);
            return;
        }
        if (requirement instanceof AndRequirement) {
            for (CraftingRequirement child :
                    ((AndRequirementAccessor) requirement).tetraInsight$getRequirements()) {
                tetraInsight$collectStructuralRequirements(
                        child, context, negated, dependencies, moduleState);
            }
            return;
        }
        if (requirement instanceof OrRequirement) {
            for (CraftingRequirement child :
                    ((OrRequirementAccessor) requirement).tetraInsight$getRequirements()) {
                tetraInsight$collectStructuralRequirements(
                        child, context, negated, dependencies, moduleState);
            }
            return;
        }
        if (negated) {
            return;
        }
        if (requirement instanceof ModuleRequirement) {
            moduleState[0] = true;
            moduleState[1] |= requirement.test(context);
        } else if (requirement instanceof HasImprovementRequirement) {
            String improvement = ((HasImprovementRequirementAccessor) requirement)
                    .tetraInsight$getImprovement();
            if (improvement != null && !improvement.isBlank()) {
                dependencies.add(improvement);
            }
        }
    }

    @Unique
    private static void tetraInsight$collectImprovementKeys(
            OutcomePreview[] previews, java.util.Set<String> output) {
        for (OutcomePreview preview : previews) {
            if (preview.variantKey != null && !preview.variantKey.isBlank()) {
                output.add(preview.variantKey);
            }
        }
    }

    @Unique
    private static boolean tetraInsight$hasAnyImprovementKey(
            OutcomePreview[] previews, java.util.Set<String> keys) {
        for (OutcomePreview preview : previews) {
            if (preview.variantKey != null && keys.contains(preview.variantKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tetraInsight$setHoningTarget(ItemStack targetStack) {
        tetraInsight$honingTarget = targetStack == null
                ? ItemStack.EMPTY
                : targetStack.copy();
    }

    @Override
    public int tetraInsight$improvementCount() {
        return tetraInsight$improvementCount;
    }
}
