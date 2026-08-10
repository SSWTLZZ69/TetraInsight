package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.client.HoloDisplaySchematic;
import io.github.createdelight.tetrainsight.client.HoloDisplaySchematicAccess;
import io.github.createdelight.tetrainsight.client.HoloHoningOutcomeStack;
import io.github.createdelight.tetrainsight.client.HoloHoningTargetAccess;
import io.github.createdelight.tetrainsight.client.HoloImprovementCountAccess;
import io.github.createdelight.tetrainsight.client.HoloStatsComparisonAccess;
import io.github.createdelight.tetrainsight.client.ImprovementComparisonMode;
import io.github.createdelight.tetrainsight.client.ImprovementPreviewContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloStatsGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloVariantDetailGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.OutcomeStack;
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
    @Final
    private HoloStatsGui stats;

    @Shadow
    private OutcomePreview hoveredImprovement;

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

    @Unique
    private static final String tetraInsight$BOOK_ENCHANT = "book_enchant";

    @Inject(method = "updateVariant", at = @At("HEAD"), remap = false)
    private void tetraInsight$resetImprovementCount(OutcomePreview variant, OutcomePreview hovered,
            String slot, CallbackInfo ci) {
        tetraInsight$improvementCount = 0;
        tetraInsight$displaySchematics = new UpgradeSchematic[0];
        tetraInsight$displayStack = ItemStack.EMPTY;
        tetraInsight$displaySlot = "";
        tetraInsight$selectedHoning.clear();
        hoveredImprovement = null;
        ImprovementPreviewContext.clear();
        ((HoloStatsComparisonAccess) stats).tetraInsight$setComparisonMode(
                ImprovementComparisonMode.NONE);
    }

    @Inject(method = "hideImprovements", at = @At("HEAD"), remap = false)
    private void tetraInsight$clearTransientImprovementState(CallbackInfo ci) {
        tetraInsight$selectedHoning.clear();
        hoveredImprovement = null;
        ImprovementPreviewContext.clear();
        ((HoloStatsComparisonAccess) stats).tetraInsight$setComparisonMode(
                ImprovementComparisonMode.NONE);
    }

    @Inject(method = "onImprovementSelect", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$selectCombinedPreview(OutcomeStack selected, CallbackInfo ci) {
        if (selected instanceof HoloHoningOutcomeStack honing) {
            int selectedIndex = tetraInsight$findHoningSelection(honing);
            if (selectedIndex >= 0) {
                tetraInsight$selectedHoning.remove(selectedIndex);
            } else {
                tetraInsight$selectedHoning.removeIf(selectedHoning ->
                        tetraInsight$sameSelectionFamily(selectedHoning, honing));
                tetraInsight$selectedHoning.add(honing);
                tetraInsight$selectedHoning.sort(Comparator
                        .comparingInt((HoloHoningOutcomeStack outcome) ->
                                outcome.preview().level)
                        .thenComparing(outcome -> outcome.schematic().getKey()));
            }
        } else {
            int selectedIndex = tetraInsight$findOrdinarySelection(selected);
            if (selectedIndex >= 0) {
                selectedOutcomes.remove(selectedIndex);
            } else {
                selectedOutcomes.removeIf(existing ->
                        tetraInsight$sameSelectionFamily(existing, selected));
                selectedOutcomes.add(selected);
            }
        }

        tetraInsight$rebuildCurrentOutcome();
        tetraInsight$refreshCombinedSelection();
        ci.cancel();
    }

    @Unique
    private void tetraInsight$rebuildCurrentOutcome() {
        List<OutcomeStack> selections = new ArrayList<>(selectedOutcomes);
        selections.addAll(tetraInsight$selectedHoning);
        currentOutcome = tetraInsight$composeOutcome(selections);
    }

    @Unique
    private OutcomePreview tetraInsight$composeOutcome(
            List<? extends OutcomeStack> selections) {
        OutcomePreview outcome = variantOutcome.clone();
        List<OutcomeStack> nonEnchantments = selections.stream()
                .filter(selection -> !tetraInsight$isBookEnchant(selection))
                .map(selection -> (OutcomeStack) selection)
                .toList();
        List<OutcomeStack> enchantments = selections.stream()
                .filter(HoloVariantDetailGuiMixin::tetraInsight$isBookEnchant)
                .map(selection -> (OutcomeStack) selection)
                .toList();
        outcome = tetraInsight$applySelections(nonEnchantments, outcome);
        return tetraInsight$applySelections(enchantments, outcome);
    }

    @Unique
    private OutcomePreview tetraInsight$applySelections(
            List<? extends OutcomeStack> selections, OutcomePreview base) {
        OutcomePreview outcome = base;
        List<OutcomeStack> pending = new ArrayList<>(selections);
        boolean progressed;
        do {
            progressed = false;
            for (int index = 0; index < pending.size(); index++) {
                OutcomePreview resolved = tetraInsight$resolveOutcome(
                        pending.get(index), outcome);
                if (resolved == null) {
                    continue;
                }
                outcome = resolved;
                pending.remove(index--);
                progressed = true;
            }
        } while (progressed && !pending.isEmpty());

        if (!pending.isEmpty()) {
            TetraInsight.LOGGER.debug(
                    "Could not recompose {} improvement selections on the current preview",
                    pending.size());
        }
        return outcome;
    }

    @Unique
    private static boolean tetraInsight$isBookEnchant(OutcomeStack selection) {
        OutcomeStackAccessor access = (OutcomeStackAccessor) selection;
        return tetraInsight$BOOK_ENCHANT.equals(
                tetraInsight$unwrapDisplaySchematic(
                        access.tetraInsight$getSchematic()).getKey());
    }

    @Unique
    private OutcomePreview tetraInsight$resolveOutcome(
            OutcomeStack selected, OutcomePreview base) {
        OutcomeStackAccessor access = (OutcomeStackAccessor) selected;
        OutcomePreview requested = access.tetraInsight$getPreview();
        UpgradeSchematic schematic = tetraInsight$unwrapDisplaySchematic(
                access.tetraInsight$getSchematic());
        for (OutcomePreview candidate : schematic.getPreviews(base.itemStack, slot)) {
            if (tetraInsight$matchesSelection(candidate, requested)) {
                return candidate;
            }
        }
        return null;
    }

    @Unique
    private static UpgradeSchematic tetraInsight$unwrapDisplaySchematic(
            UpgradeSchematic schematic) {
        if (schematic instanceof HoloDisplaySchematicAccess display) {
            return display.tetraInsight$delegate();
        }
        return schematic;
    }

    @Unique
    private void tetraInsight$refreshCombinedSelection() {
        List<OutcomeStack> displaySelection = new ArrayList<>(selectedOutcomes);
        displaySelection.addAll(tetraInsight$selectedHoning);
        improvements.updateSelection(currentOutcome.itemStack, displaySelection);
        tetraInsight$showSelectedComparison();
    }

    @Unique
    private int tetraInsight$findOrdinarySelection(OutcomeStack candidate) {
        for (int index = 0; index < selectedOutcomes.size(); index++) {
            if (tetraInsight$sameSelection(selectedOutcomes.get(index), candidate)) {
                return index;
            }
        }
        return -1;
    }

    @Unique
    private int tetraInsight$findHoningSelection(HoloHoningOutcomeStack candidate) {
        for (int index = 0; index < tetraInsight$selectedHoning.size(); index++) {
            HoloHoningOutcomeStack selected = tetraInsight$selectedHoning.get(index);
            if (tetraInsight$sameSelection(selected, candidate)) {
                return index;
            }
        }
        return -1;
    }

    @Inject(method = "onImprovementHover", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void tetraInsight$previewOnCombinedOutcome(
            OutcomePreview hovered, CallbackInfo ci) {
        UpgradeSchematic schematic = ImprovementPreviewContext.find(hovered);
        if (schematic == null) {
            schematic = tetraInsight$findPreviewSchematic(hovered);
        }
        if (schematic == null) {
            return;
        }

        OutcomeStack hoverSelection = new OutcomeStack(schematic, hovered);
        List<OutcomeStack> previewSelections = new ArrayList<>(selectedOutcomes);
        previewSelections.addAll(tetraInsight$selectedHoning);
        previewSelections.removeIf(selected ->
                tetraInsight$sameSelectionFamily(selected, hoverSelection));
        previewSelections.add(hoverSelection);
        OutcomePreview resolved = tetraInsight$composeOutcome(previewSelections);
        if (resolved == null) {
            return;
        }

        stats.update(
                currentOutcome.itemStack,
                resolved.itemStack,
                null,
                null,
                Minecraft.getInstance().player);
        ((HoloStatsComparisonAccess) stats).tetraInsight$setComparisonMode(
                tetraInsight$hasSelections()
                        ? ImprovementComparisonMode.SELECTED_TO_PREVIEW
                        : ImprovementComparisonMode.BASE_TO_PREVIEW);
        hoveredImprovement = hovered;
        ci.cancel();
    }

    @Inject(method = "onImprovementBlur", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void tetraInsight$restoreSelectedComparison(
            OutcomePreview hovered, CallbackInfo ci) {
        if (hoveredImprovement == null
                || !tetraInsight$matchesSelection(hovered, hoveredImprovement)) {
            return;
        }
        tetraInsight$showSelectedComparison();
        hoveredImprovement = null;
        ci.cancel();
    }

    @Unique
    private UpgradeSchematic tetraInsight$findPreviewSchematic(OutcomePreview preview) {
        for (UpgradeSchematic schematic : tetraInsight$displaySchematics) {
            for (OutcomePreview candidate : schematic.getPreviews(
                    variantOutcome.itemStack, slot)) {
                if (tetraInsight$matchesSelection(candidate, preview)) {
                    return schematic;
                }
            }
        }
        return null;
    }

    @Unique
    private void tetraInsight$showSelectedComparison() {
        boolean hasSelections = tetraInsight$hasSelections();
        ItemStack base = variantOutcome != null
                ? variantOutcome.itemStack
                : currentOutcome.itemStack;
        stats.update(
                base,
                currentOutcome.itemStack,
                null,
                null,
                Minecraft.getInstance().player);
        ((HoloStatsComparisonAccess) stats).tetraInsight$setComparisonMode(
                hasSelections
                        ? ImprovementComparisonMode.BASE_TO_SELECTED
                        : ImprovementComparisonMode.NONE);
    }

    @Unique
    private boolean tetraInsight$hasSelections() {
        return !selectedOutcomes.isEmpty() || !tetraInsight$selectedHoning.isEmpty();
    }

    @Unique
    private static boolean tetraInsight$sameSelection(
            OutcomeStack left, OutcomeStack right) {
        OutcomeStackAccessor leftAccess = (OutcomeStackAccessor) left;
        OutcomeStackAccessor rightAccess = (OutcomeStackAccessor) right;
        return leftAccess.tetraInsight$getSchematic().getKey().equals(
                        rightAccess.tetraInsight$getSchematic().getKey())
                && tetraInsight$matchesSelection(
                        leftAccess.tetraInsight$getPreview(),
                        rightAccess.tetraInsight$getPreview());
    }

    @Unique
    private static boolean tetraInsight$sameSelectionFamily(
            OutcomeStack left, OutcomeStack right) {
        OutcomeStackAccessor leftAccess = (OutcomeStackAccessor) left;
        OutcomeStackAccessor rightAccess = (OutcomeStackAccessor) right;
        if (left instanceof HoloHoningOutcomeStack leftChain
                && right instanceof HoloHoningOutcomeStack rightChain
                && !leftChain.chainKey().isBlank()
                && leftChain.chainKey().equals(rightChain.chainKey())) {
            return true;
        }
        if (left instanceof HoloHoningOutcomeStack leftChain
                && leftChain.chainKey().equals(
                        rightAccess.tetraInsight$getPreview().variantKey)) {
            return true;
        }
        if (right instanceof HoloHoningOutcomeStack rightChain
                && rightChain.chainKey().equals(
                        leftAccess.tetraInsight$getPreview().variantKey)) {
            return true;
        }
        return leftAccess.tetraInsight$getSchematic().getKey().equals(
                        rightAccess.tetraInsight$getSchematic().getKey())
                && java.util.Objects.equals(
                        leftAccess.tetraInsight$getPreview().variantKey,
                        rightAccess.tetraInsight$getPreview().variantKey);
    }

    @Unique
    private static boolean tetraInsight$matchesSelection(
            OutcomePreview candidate, OutcomePreview requested) {
        if (!java.util.Objects.equals(candidate.variantKey, requested.variantKey)
                || candidate.level != requested.level) {
            return false;
        }
        ItemStack[] candidateMaterials = candidate.materials != null
                ? candidate.materials
                : new ItemStack[0];
        ItemStack[] requestedMaterials = requested.materials != null
                ? requested.materials
                : new ItemStack[0];
        if (candidateMaterials.length != requestedMaterials.length) {
            return false;
        }
        for (int index = 0; index < candidateMaterials.length; index++) {
            if (!ItemStack.isSameItemSameTags(
                        candidateMaterials[index], requestedMaterials[index])
                    || candidateMaterials[index].getCount()
                            != requestedMaterials[index].getCount()) {
                return false;
            }
        }
        return true;
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

        Set<String> ordinaryImprovementKeys = new HashSet<>();
        ordinary.values().forEach(schematic -> tetraInsight$collectImprovementKeys(
                previewsBySchematic.get(schematic), ordinaryImprovementKeys));
        boolean addedLinkedImprovement;
        do {
            addedLinkedImprovement = false;
            for (UpgradeSchematic schematic : relevantCandidates) {
                if (schematic.isHoning()
                        || schematic.getType() != SchematicType.improvement
                        || ordinary.containsKey(schematic.getKey())
                        || !moduleOwnershipBySchematic.computeIfAbsent(
                                schematic, value -> tetraInsight$matchesModuleOwnership(
                                        value, candidateContext))) {
                    continue;
                }
                Set<String> dependencies = new HashSet<>();
                tetraInsight$collectStructuralRequirements(
                        tetraInsight$getRequirement(schematic), candidateContext,
                        false, dependencies, new boolean[2]);
                if (dependencies.stream().noneMatch(ordinaryImprovementKeys::contains)) {
                    continue;
                }
                ordinary.put(schematic.getKey(), schematic);
                tetraInsight$collectImprovementKeys(
                        previewsBySchematic.get(schematic), ordinaryImprovementKeys);
                addedLinkedImprovement = true;
            }
        } while (addedLinkedImprovement);

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
                    target = "Lse/mickelus/tetra/items/modular/impl/holo/gui/craft/schematic/HoloImprovementButton;updateCount(I)V"),
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
