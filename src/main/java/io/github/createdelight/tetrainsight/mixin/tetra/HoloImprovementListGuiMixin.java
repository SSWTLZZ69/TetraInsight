package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloDisplaySchematicAccess;
import io.github.createdelight.tetrainsight.client.HoloImprovementGuiExtension;
import io.github.createdelight.tetrainsight.client.TetraInsightConfig;
import io.github.createdelight.tetrainsight.client.HoloImprovementBackButtonGui;
import io.github.createdelight.tetrainsight.client.HoloImprovementOverviewEntryGui;
import io.github.createdelight.tetrainsight.client.HoloSortPageControls;
import io.github.createdelight.tetrainsight.client.ImprovementDisplayEntry;
import io.github.createdelight.tetrainsight.client.ImprovementChainEntry;
import io.github.createdelight.tetrainsight.client.PaginationWindow;
import io.github.createdelight.tetrainsight.integration.tetra.MaterialGlyphTintResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;
import se.mickelus.mutil.gui.impl.GuiHorizontalScrollable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloToggleVisibilityButtonGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.OutcomeStack;
import se.mickelus.tetra.module.schematic.ConfigSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.requirement.AndRequirement;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;
import se.mickelus.tetra.module.schematic.requirement.HasImprovementRequirement;
import se.mickelus.tetra.module.schematic.requirement.NotRequirement;
import se.mickelus.tetra.module.schematic.requirement.OrRequirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(value = HoloImprovementListGui.class, remap = false)
public abstract class HoloImprovementListGuiMixin
        implements io.github.createdelight.tetrainsight.client.HoloImprovementOverviewAccess {
    @Unique
    private static final int tetraInsight$PAGE_SIZE = 15;

    @Unique
    private static final int tetraInsight$GROUP_COUNT = 5;

    @Unique
    private static final int tetraInsight$GROUP_PITCH = 17;

    @Unique
    private static final int tetraInsight$OVERVIEW_TOP = 6;

    @Unique
    private static final int tetraInsight$TOOLBAR_GAP = 6;

    @Unique
    private static final int tetraInsight$DETAIL_HEADER_HEIGHT = 14;

    @Unique
    private HoloToggleVisibilityButtonGui tetraInsight$visibilityToggle;

    @Unique
    private HoloSortPageControls tetraInsight$pageControls;

    @Unique
    private HoloImprovementBackButtonGui tetraInsight$backButton;

    @Unique
    private boolean tetraInsight$showApplicable = true;

    @Unique
    private ItemStack tetraInsight$itemStack = ItemStack.EMPTY;

    @Unique
    private String tetraInsight$slot = "";

    @Unique
    private UpgradeSchematic[] tetraInsight$allSchematics = new UpgradeSchematic[0];

    @Unique
    private List<OutcomeStack> tetraInsight$selectedOutcomes = List.of();

    @Unique
    private int tetraInsight$currentPage;

    @Unique
    private boolean tetraInsight$renderDirty;

    @Unique
    private List<ImprovementDisplayEntry> tetraInsight$displayEntries = List.of();

    @Unique
    private boolean tetraInsight$displayEntriesDirty = true;

    @Unique
    private final List<HoloImprovementOverviewEntryGui> tetraInsight$overviewEntries =
            new ArrayList<>();

    @Unique
    private GuiHorizontalLayoutGroup[] tetraInsight$groups;

    @Unique
    private ImprovementDisplayEntry tetraInsight$detailEntry;

    @Unique
    private KeyframeAnimation tetraInsight$transitionAnimation;

    @Unique
    private boolean tetraInsight$transitioning;

    @Shadow
    @Final
    private List<HoloImprovementGui> improvements;

    @Shadow
    @Final
    private GuiHorizontalScrollable container;

    @Shadow
    @Final
    private GuiHorizontalLayoutGroup[] groups;

    @Shadow
    @Final
    private Consumer<OutcomePreview> onVariantHover;

    @Shadow
    @Final
    private Consumer<OutcomePreview> onVariantBlur;

    @Shadow
    @Final
    private Consumer<OutcomeStack> onVariantSelect;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addVisibilityToggle(int x, int y, int width, int height,
            Consumer<OutcomePreview> onVariantHover,
            Consumer<OutcomePreview> onVariantBlur,
            Consumer<OutcomeStack> onVariantSelect,
            CallbackInfo ci) {
        tetraInsight$visibilityToggle = new HoloToggleVisibilityButtonGui(
                0, -16, this::tetraInsight$toggleVisibility);
        tetraInsight$visibilityToggle.update(true);
        tetraInsight$visibilityToggle.setVisible(false);
        ((GuiElement) (Object) this).addChild(tetraInsight$visibilityToggle);
        tetraInsight$pageControls = new HoloSortPageControls(
                () -> tetraInsight$changePage(-1),
                () -> tetraInsight$changePage(1));
        tetraInsight$pageControls.setY(-16);
        tetraInsight$updatePageControls(1, 1);
        ((GuiElement) (Object) this).addChild(tetraInsight$pageControls);
        tetraInsight$backButton = new HoloImprovementBackButtonGui(
                this::tetraInsight$closeDetail);
        ((GuiElement) (Object) this).addChild(tetraInsight$backButton);

        tetraInsight$groups = Arrays.copyOf(groups, tetraInsight$GROUP_COUNT);
        for (int index = 0; index < tetraInsight$GROUP_COUNT; index++) {
            if (tetraInsight$groups[index] == null) {
                tetraInsight$groups[index] = new GuiHorizontalLayoutGroup(
                        0, 0, 32, 8);
                container.addChild(tetraInsight$groups[index]);
            }
            tetraInsight$groups[index].setY(index * tetraInsight$GROUP_PITCH);
        }
    }

    @Inject(method = "updateSchematics", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$groupImprovementChains(ItemStack itemStack, String slot,
            UpgradeSchematic[] schematics, CallbackInfo ci) {
        if (!TetraInsightConfig.improvementOverview.get()) {
            tetraInsight$displayEntries = List.of();
            tetraInsight$overviewEntries.clear();
            tetraInsight$detailEntry = null;
            tetraInsight$visibilityToggle.setVisible(false);
            tetraInsight$pageControls.setVisible(false);
            tetraInsight$backButton.setVisible(false);
            return;
        }
        tetraInsight$itemStack = itemStack.copy();
        tetraInsight$slot = slot;
        tetraInsight$allSchematics = Arrays.copyOf(schematics, schematics.length);
        tetraInsight$detailEntry = null;
        tetraInsight$visibilityToggle.setVisible(
                Arrays.stream(schematics).anyMatch(UpgradeSchematic::isHoning));
        tetraInsight$currentPage = 0;
        tetraInsight$renderDirty = true;
        tetraInsight$displayEntriesDirty = true;
        tetraInsight$clearRows();
        tetraInsight$updatePageControls(1, 1);
        if (((GuiElement) (Object) this).isVisible()) {
            tetraInsight$renderSchematics();
        }
        ci.cancel();
    }

    @Inject(method = "show", at = @At("HEAD"), remap = false)
    private void tetraInsight$renderBeforeShow(CallbackInfo ci) {
        if (tetraInsight$renderDirty) {
            tetraInsight$renderSchematics();
        }
    }

    @Inject(method = "updateSelection", at = @At("HEAD"), remap = false)
    private void tetraInsight$rememberSelection(ItemStack itemStack,
            List<OutcomeStack> selectedOutcomes, CallbackInfo ci) {
        tetraInsight$itemStack = itemStack.copy();
        tetraInsight$selectedOutcomes = List.copyOf(selectedOutcomes);
    }

    @Inject(method = "updateSelection", at = @At("RETURN"), remap = false)
    private void tetraInsight$relayoutAfterSelection(ItemStack itemStack,
            List<OutcomeStack> selectedOutcomes, CallbackInfo ci) {
        tetraInsight$overviewEntries.forEach(entry ->
                entry.updateSelection(tetraInsight$selectedOutcomes));
        tetraInsight$refreshImprovementLayout();
    }

    @Unique
    private void tetraInsight$toggleVisibility() {
        tetraInsight$transitionSwap(0, () -> {
            tetraInsight$showApplicable = !tetraInsight$showApplicable;
            tetraInsight$visibilityToggle.update(tetraInsight$showApplicable);
            tetraInsight$currentPage = 0;
            tetraInsight$displayEntriesDirty = true;
            tetraInsight$renderSchematics();
        });
    }

    @Unique
    private void tetraInsight$renderSchematics() {
        long started = System.nanoTime();
        if (tetraInsight$detailEntry != null) {
            tetraInsight$renderDetail();
            return;
        }

        container.setY(tetraInsight$OVERVIEW_TOP);

        if (tetraInsight$displayEntriesDirty) {
            tetraInsight$rebuildDisplayEntries();
        }

        tetraInsight$backButton.setVisible(false);
        tetraInsight$visibilityToggle.setVisible(
                Arrays.stream(tetraInsight$allSchematics)
                        .anyMatch(UpgradeSchematic::isHoning));
        PaginationWindow window = PaginationWindow.of(
                tetraInsight$displayEntries.size(), tetraInsight$currentPage,
                tetraInsight$PAGE_SIZE);
        tetraInsight$currentPage = window.currentPage();
        tetraInsight$clearRows();
        for (int index = window.startIndex(); index < window.endIndex(); index++) {
            ImprovementDisplayEntry displayEntry = tetraInsight$displayEntries.get(index);
            GuiHorizontalLayoutGroup targetGroup = tetraInsight$shortestRow();
            boolean available = displayEntry.isChain()
                    ? displayEntry.chain().stream().anyMatch(ImprovementChainEntry::available)
                    : !(displayEntry.schematic() instanceof HoloDisplaySchematicAccess display)
                            || display.tetraInsight$isAvailable();
            HoloImprovementOverviewEntryGui overview =
                    new HoloImprovementOverviewEntryGui(
                            0, 0, displayEntry, available,
                            tetraInsight$conditionLines(displayEntry, available),
                            () -> tetraInsight$openDetail(displayEntry),
                            onVariantSelect);
            overview.updateSelection(tetraInsight$selectedOutcomes);
            tetraInsight$overviewEntries.add(overview);
            targetGroup.addChild(overview);
            targetGroup.forceLayout();
        }

        for (GuiHorizontalLayoutGroup group : tetraInsight$groups) {
            group.forceLayout();
        }
        container.markDirty();
        tetraInsight$updatePageControls(
                tetraInsight$currentPage + 1, window.totalPages());
        tetraInsight$renderDirty = false;
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        if (elapsedMillis >= 10) {
            io.github.createdelight.tetrainsight.TetraInsight.LOGGER.info(
                    "Rendered Tetra improvement page {}/{} in {} ms: groups={}, visible={}",
                    tetraInsight$currentPage + 1, window.totalPages(), elapsedMillis,
                    tetraInsight$displayEntries.size(), tetraInsight$overviewEntries.size());
        }
    }

    @Unique
    private void tetraInsight$rebuildDisplayEntries() {
        UpgradeSchematic[] schematics = tetraInsight$showApplicable
                ? tetraInsight$allSchematics
                : Arrays.stream(tetraInsight$allSchematics)
                        .filter(schematic -> !(schematic instanceof HoloDisplaySchematicAccess display)
                                || display.tetraInsight$isAvailable())
                        .toArray(UpgradeSchematic[]::new);

        Map<UpgradeSchematic, OutcomePreview[]> previewsBySchematic = new IdentityHashMap<>();
        Map<UpgradeSchematic, List<ImprovementChainEntry>> entriesBySchematic =
                new IdentityHashMap<>();
        Map<String, List<ImprovementChainEntry>> entriesByKey = new LinkedHashMap<>();
        for (UpgradeSchematic schematic : schematics) {
            OutcomePreview[] previews = Arrays.stream(schematic.getPreviews(
                            tetraInsight$itemStack, tetraInsight$slot))
                    .filter(preview -> tetraInsight$shouldDisplayPreview(
                            schematic, preview))
                    .toArray(OutcomePreview[]::new);
            previewsBySchematic.put(schematic, previews);
            boolean available = !(schematic instanceof HoloDisplaySchematicAccess display)
                    || display.tetraInsight$isAvailable();
            for (OutcomePreview preview : previews) {
                if (preview.variantKey == null || preview.level <= 0) {
                    continue;
                }
                ImprovementChainEntry entry = new ImprovementChainEntry(
                        schematic, preview, available);
                entriesBySchematic.computeIfAbsent(
                                schematic, ignored -> new ArrayList<>())
                        .add(entry);
                entriesByKey.computeIfAbsent(
                                preview.variantKey, ignored -> new ArrayList<>())
                        .add(entry);
            }
        }

        Set<String> chainKeys = new HashSet<>();
        entriesByKey.forEach((key, entries) -> {
            long levels = entries.stream()
                    .mapToInt(entry -> entry.preview().level)
                    .distinct()
                    .count();
            boolean honingChain = entries.stream()
                    .anyMatch(entry -> entry.schematic().isHoning());
            boolean dependencyChain = entries.stream()
                    .map(ImprovementChainEntry::schematic)
                    .distinct()
                    .anyMatch(schematic -> tetraInsight$dependsOnImprovement(
                            schematic, key));
            if (levels > 1 && (honingChain || dependencyChain)) {
                chainKeys.add(key);
            }
        });

        Map<UpgradeSchematic, String> chainKeyBySchematic = new IdentityHashMap<>();
        entriesBySchematic.forEach((schematic, entries) -> entries.stream()
                .map(entry -> entry.preview().variantKey)
                .filter(chainKeys::contains)
                .findFirst()
                .ifPresent(key -> chainKeyBySchematic.put(schematic, key)));

        Set<String> renderedChains = new HashSet<>();
        List<ImprovementDisplayEntry> displayEntries = new ArrayList<>();
        for (UpgradeSchematic schematic : schematics) {
            OutcomePreview[] schematicPreviews = previewsBySchematic.getOrDefault(
                    schematic, new OutcomePreview[0]);
            if (schematicPreviews.length == 0
                    && MaterialGlyphTintResolver.requiresUsableMaterial(schematic)) {
                continue;
            }
            String improvementKey = chainKeyBySchematic.get(schematic);
            if (improvementKey == null) {
                displayEntries.add(ImprovementDisplayEntry.single(
                        schematic, schematicPreviews));
                continue;
            }

            if (!renderedChains.add(improvementKey)) {
                continue;
            }

            List<ImprovementChainEntry> chain = entriesByKey.get(improvementKey).stream()
                    .sorted(Comparator
                            .comparingInt((ImprovementChainEntry value) -> value.preview().level)
                            .thenComparing(value -> value.schematic().isHoning())
                            .thenComparing(value -> value.schematic().getKey()))
                    .toList();
            displayEntries.add(ImprovementDisplayEntry.chain(improvementKey, chain));
        }
        tetraInsight$displayEntries = List.copyOf(displayEntries);
        tetraInsight$displayEntriesDirty = false;
    }

    @Unique
    private static boolean tetraInsight$shouldDisplayPreview(
            UpgradeSchematic schematic, OutcomePreview preview) {
        if (preview == null) {
            return false;
        }
        if ("book_enchant".equals(schematic.getKey())
                && TetraInsightConfig.enchantmentImprovements.get()) {
            return true;
        }
        return MaterialGlyphTintResolver.shouldDisplay(schematic, preview);
    }

    @Unique
    private static boolean tetraInsight$dependsOnImprovement(
            UpgradeSchematic schematic, String improvementKey) {
        UpgradeSchematic delegate = schematic instanceof HoloDisplaySchematicAccess display
                ? display.tetraInsight$delegate()
                : schematic;
        if (!(delegate instanceof ConfigSchematic)) {
            return false;
        }
        CraftingRequirement requirement = ((ConfigSchematicAccessor) delegate)
                .tetraInsight$getDefinition().requirement;
        return tetraInsight$containsPositiveImprovementRequirement(
                requirement, improvementKey, false);
    }

    @Unique
    private static boolean tetraInsight$containsPositiveImprovementRequirement(
            CraftingRequirement requirement, String improvementKey, boolean negated) {
        if (requirement == null) {
            return false;
        }
        if (requirement instanceof NotRequirement) {
            return tetraInsight$containsPositiveImprovementRequirement(
                    ((NotRequirementAccessor) requirement).tetraInsight$getRequirement(),
                    improvementKey, !negated);
        }
        if (requirement instanceof AndRequirement) {
            return Arrays.stream(((AndRequirementAccessor) requirement)
                            .tetraInsight$getRequirements())
                    .anyMatch(child -> tetraInsight$containsPositiveImprovementRequirement(
                            child, improvementKey, negated));
        }
        if (requirement instanceof OrRequirement) {
            return Arrays.stream(((OrRequirementAccessor) requirement)
                            .tetraInsight$getRequirements())
                    .anyMatch(child -> tetraInsight$containsPositiveImprovementRequirement(
                            child, improvementKey, negated));
        }
        return !negated
                && requirement instanceof HasImprovementRequirement
                && java.util.Objects.equals(
                        ((HasImprovementRequirementAccessor) requirement)
                                .tetraInsight$getImprovement(),
                        improvementKey);
    }

    @Override
    @Unique
    public void tetraInsight$openDetailForMaterial(String materialKey,
            ItemStack materialStack) {
        if ((materialKey == null || materialKey.isBlank())
                && (materialStack == null || materialStack.isEmpty())) {
            return;
        }
        if (tetraInsight$displayEntriesDirty) {
            tetraInsight$rebuildDisplayEntries();
        }
        for (int index = 0; index < tetraInsight$displayEntries.size(); index++) {
            ImprovementDisplayEntry entry = tetraInsight$displayEntries.get(index);
            if (!tetraInsight$entryUsesMaterial(entry, materialKey, materialStack)) {
                continue;
            }
            tetraInsight$currentPage = index / tetraInsight$PAGE_SIZE;
            tetraInsight$openDetail(entry);
            return;
        }
    }

    @Unique
    private static boolean tetraInsight$entryUsesMaterial(
            ImprovementDisplayEntry entry, String materialKey,
            ItemStack materialStack) {
        if (entry.isChain()) {
            return entry.chain().stream().anyMatch(chainEntry ->
                    tetraInsight$previewUsesMaterial(chainEntry.schematic(),
                            chainEntry.preview(), materialKey, materialStack));
        }
        return Arrays.stream(entry.previews()).anyMatch(preview ->
                tetraInsight$previewUsesMaterial(entry.schematic(), preview,
                        materialKey, materialStack));
    }

    @Unique
    private static boolean tetraInsight$previewUsesMaterial(
            UpgradeSchematic schematic, OutcomePreview preview,
            String materialKey, ItemStack materialStack) {
        if (materialKey != null && !materialKey.isBlank()) {
            return MaterialGlyphTintResolver
                    .resolve(schematic.getKey(), preview)
                    .map(snapshot -> snapshot.materialKey().equals(materialKey))
                    .orElse(false);
        }
        if (materialStack != null && !materialStack.isEmpty()
                && preview.materials != null) {
            return Arrays.stream(preview.materials)
                    .filter(java.util.Objects::nonNull)
                    .filter(material -> !material.isEmpty())
                    .anyMatch(material -> material.getItem()
                            == materialStack.getItem());
        }
        return false;
    }

    @Unique
    private void tetraInsight$openDetail(ImprovementDisplayEntry entry) {
        tetraInsight$transitionSwap(1, () -> {
            tetraInsight$detailEntry = entry;
            tetraInsight$renderSchematics();
        });
    }

    @Unique
    private void tetraInsight$closeDetail() {
        tetraInsight$transitionSwap(-1, () -> {
            tetraInsight$detailEntry = null;
            tetraInsight$renderSchematics();
        });
    }

    @Unique
    private void tetraInsight$renderDetail() {
        tetraInsight$clearRows();
        tetraInsight$visibilityToggle.setVisible(false);
        tetraInsight$pageControls.setVisible(false);
        tetraInsight$backButton.setY(0);
        tetraInsight$backButton.setVisible(true);
        container.setY(tetraInsight$DETAIL_HEADER_HEIGHT);

        HoloImprovementGui improvement = tetraInsight$addImprovement(
                tetraInsight$groups[0], tetraInsight$detailEntry.schematic(),
                tetraInsight$itemStack, tetraInsight$slot);
        if (tetraInsight$detailEntry.isChain()) {
            ((HoloImprovementGuiExtension) improvement).tetraInsight$setImprovementChain(
                    tetraInsight$detailEntry.improvementKey(),
                    tetraInsight$detailEntry.chain(), tetraInsight$itemStack);
        }
        improvement.updateSelection(tetraInsight$itemStack, tetraInsight$selectedOutcomes);
        tetraInsight$refreshImprovementLayout();
        tetraInsight$renderDirty = false;
    }

    @Unique
    private boolean tetraInsight$changePage(int delta) {
        int nextPage = PaginationWindow.of(
                tetraInsight$displayEntries.size(), tetraInsight$currentPage + delta,
                tetraInsight$PAGE_SIZE).currentPage();
        if (nextPage == tetraInsight$currentPage) {
            return false;
        }
        int direction = Integer.compare(nextPage, tetraInsight$currentPage);
        tetraInsight$transitionSwap(direction, () -> {
            tetraInsight$currentPage = nextPage;
            tetraInsight$renderSchematics();
        });
        return true;
    }

    @Unique
    private void tetraInsight$transitionSwap(int direction, Runnable swap) {
        if (tetraInsight$transitioning) {
            return;
        }
        tetraInsight$transitioning = true;
        tetraInsight$resetContainerTransform();

        int exitOffset = direction == 0 ? 0 : -direction * 2;
        tetraInsight$transitionAnimation = new KeyframeAnimation(45, container)
                .applyTo(
                        new Applier.Opacity(1f, 0f),
                        new Applier.TranslateX(0f, exitOffset))
                .onStop(completed -> {
                    if (!completed) {
                        tetraInsight$transitioning = false;
                        tetraInsight$resetContainerTransform();
                        return;
                    }

                    swap.run();
                    int enterOffset = direction == 0 ? 0 : direction * 2;
                    tetraInsight$transitionAnimation = new KeyframeAnimation(65, container)
                            .applyTo(
                                    new Applier.Opacity(0f, 1f),
                                    new Applier.TranslateX(enterOffset, 0f))
                            .onStop(entered -> {
                                tetraInsight$transitioning = false;
                                tetraInsight$resetContainerTransform();
                            });
                    tetraInsight$transitionAnimation.start();
                });
        tetraInsight$transitionAnimation.start();
    }

    @Unique
    private void tetraInsight$resetContainerTransform() {
        container.setX(0);
        container.setOpacity(1f);
    }

    @Unique
    private void tetraInsight$clearRows() {
        improvements.clear();
        tetraInsight$overviewEntries.clear();
        for (GuiHorizontalLayoutGroup group : tetraInsight$groups) {
            group.clearChildren();
            group.setWidth(0);
        }
    }

    @Unique
    private void tetraInsight$refreshImprovementLayout() {
        for (HoloImprovementGui improvement : improvements) {
            ((HoloImprovementGuiExtension) improvement).tetraInsight$refreshLayoutWidth();
        }
        for (GuiHorizontalLayoutGroup group : tetraInsight$groups) {
            group.forceLayout();
        }
        container.markDirty();
    }

    @Unique
    private List<Component> tetraInsight$conditionLines(
            ImprovementDisplayEntry entry, boolean available) {
        List<Component> lines = new ArrayList<>();
        if (entry.isChain()) {
            List<ItemStack> materials = new ArrayList<>();
            for (ImprovementChainEntry chainEntry : entry.chain()) {
                if (chainEntry.preview().materials != null) {
                    for (ItemStack material : chainEntry.preview().materials) {
                        if (material != null && !material.isEmpty()
                                && materials.stream().noneMatch(existing ->
                                        ItemStack.isSameItemSameTags(
                                                existing, material))) {
                            materials.add(material);
                        }
                    }
                }
            }
            tetraInsight$appendMaterialLines(lines, materials);
            tetraInsight$appendRequirementLines(lines,
                    entry.chain().get(0).schematic(), available);
        } else {
            List<ItemStack> materials = new ArrayList<>();
            int experienceCost = 0;
            for (OutcomePreview preview : entry.previews()) {
                if (preview.materials != null) {
                    for (ItemStack material : preview.materials) {
                        if (material != null && !material.isEmpty()
                                && materials.stream().noneMatch(existing ->
                                        ItemStack.isSameItemSameTags(
                                                existing, material))) {
                            materials.add(material);
                        }
                    }
                }
                try {
                    experienceCost = Math.max(experienceCost,
                            entry.schematic().getExperienceCost(
                                    tetraInsight$itemStack,
                                    preview.materials != null
                                            ? preview.materials
                                            : new ItemStack[0],
                                    tetraInsight$slot));
                } catch (RuntimeException ignored) {
                }
            }
            tetraInsight$appendMaterialLines(lines, materials);
            if (experienceCost > 0) {
                lines.add(Component.translatable(
                                "tetra_insight.holo.improvement.experience_cost_max",
                                experienceCost)
                        .withStyle(ChatFormatting.GRAY));
            }
            tetraInsight$appendRequirementLines(lines,
                    entry.schematic(), available);
        }
        return List.copyOf(lines);
    }

    @Unique
    private static void tetraInsight$appendMaterialLines(
            List<Component> lines, List<ItemStack> materials) {
        if (materials.isEmpty()) {
            return;
        }
        lines.add(Component.translatable(
                        "tetra_insight.holo.improvement.consumables")
                .withStyle(ChatFormatting.GRAY));
        materials.stream().limit(4).forEach(material -> lines.add(
                Component.literal("  ")
                        .append(material.getHoverName().copy()
                                .withStyle(ChatFormatting.GRAY))
                        .append(material.getCount() > 1
                                ? Component.literal(" ×" + material.getCount())
                                        .withStyle(ChatFormatting.DARK_GRAY)
                                : Component.empty())));
        if (materials.size() > 4) {
            lines.add(Component.translatable(
                            "tetra_insight.holo.improvement.more_consumables",
                            materials.size() - 4)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Unique
    private static void tetraInsight$appendRequirementLines(
            List<Component> lines, UpgradeSchematic schematic,
            boolean available) {
        if (available) {
            return;
        }
        List<Component> requirements = schematic.getRequirementDescription();
        if (requirements != null && !requirements.isEmpty()) {
            lines.add(Component.translatable(
                            "tetra_insight.holo.improvement.requires")
                    .withStyle(ChatFormatting.GRAY));
            requirements.forEach(requirement ->
                    lines.add(Component.literal("  ")
                            .append(requirement.copy()
                                    .withStyle(ChatFormatting.DARK_GRAY))));
        }
    }

    @Unique
    private void tetraInsight$updatePageControls(int currentPage, int totalPages) {
        int pageStart = 0;
        if (tetraInsight$visibilityToggle.isVisible()) {
            int toggleContentExtent = tetraInsight$visibilityToggle.getChildren().stream()
                    .mapToInt(child -> child.getX() + child.getWidth())
                    .max()
                    .orElse(tetraInsight$visibilityToggle.getWidth());
            pageStart = tetraInsight$visibilityToggle.getX()
                    + Math.max(tetraInsight$visibilityToggle.getWidth(), toggleContentExtent)
                    + tetraInsight$TOOLBAR_GAP;
        }
        tetraInsight$pageControls.setX(pageStart);
        tetraInsight$pageControls.update(
                Math.max(40, container.getWidth() - pageStart),
                currentPage, totalPages);
    }

    @Unique
    private GuiHorizontalLayoutGroup tetraInsight$shortestRow() {
        return Arrays.stream(tetraInsight$groups)
                .min(Comparator.comparingInt(GuiHorizontalLayoutGroup::getWidth))
                .orElse(tetraInsight$groups[0]);
    }

    @Unique
    private HoloImprovementGui tetraInsight$addImprovement(GuiHorizontalLayoutGroup group,
            UpgradeSchematic schematic, ItemStack itemStack, String slot) {
        HoloImprovementGui improvement = new HoloImprovementGui(
                0, 0, schematic, itemStack, slot,
                onVariantHover, onVariantBlur, onVariantSelect);
        improvements.add(improvement);
        group.addChild(improvement);
        group.forceLayout();
        return improvement;
    }
}
