package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloDisplaySchematicAccess;
import io.github.createdelight.tetrainsight.client.HoloImprovementGuiExtension;
import io.github.createdelight.tetrainsight.client.HoloSortPageControls;
import io.github.createdelight.tetrainsight.client.ImprovementDisplayEntry;
import io.github.createdelight.tetrainsight.client.ImprovementChainEntry;
import io.github.createdelight.tetrainsight.client.PaginationWindow;
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
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloToggleVisibilityButtonGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.OutcomeStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

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
public abstract class HoloImprovementListGuiMixin {
    @Unique
    private static final int tetraInsight$PAGE_SIZE = 12;

    @Unique
    private static final int tetraInsight$TOOLBAR_GAP = 6;

    @Unique
    private HoloToggleVisibilityButtonGui tetraInsight$visibilityToggle;

    @Unique
    private HoloSortPageControls tetraInsight$pageControls;

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
    }

    @Inject(method = "updateSchematics", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$groupImprovementChains(ItemStack itemStack, String slot,
            UpgradeSchematic[] schematics, CallbackInfo ci) {
        tetraInsight$itemStack = itemStack.copy();
        tetraInsight$slot = slot;
        tetraInsight$allSchematics = Arrays.copyOf(schematics, schematics.length);
        tetraInsight$visibilityToggle.setVisible(
                Arrays.stream(schematics).anyMatch(UpgradeSchematic::isHoning));
        tetraInsight$currentPage = 0;
        tetraInsight$renderDirty = true;
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
        tetraInsight$refreshImprovementLayout();
    }

    @Unique
    private void tetraInsight$toggleVisibility() {
        tetraInsight$showApplicable = !tetraInsight$showApplicable;
        tetraInsight$visibilityToggle.update(tetraInsight$showApplicable);
        tetraInsight$currentPage = 0;
        tetraInsight$renderSchematics();
    }

    @Unique
    private void tetraInsight$renderSchematics() {
        long started = System.nanoTime();
        UpgradeSchematic[] schematics = tetraInsight$showApplicable
                ? tetraInsight$allSchematics
                : Arrays.stream(tetraInsight$allSchematics)
                        .filter(schematic -> !(schematic instanceof HoloDisplaySchematicAccess display)
                                || display.tetraInsight$isAvailable())
                        .toArray(UpgradeSchematic[]::new);

        Map<UpgradeSchematic, ImprovementChainEntry> entriesBySchematic = new IdentityHashMap<>();
        Map<String, List<ImprovementChainEntry>> entriesByKey = new LinkedHashMap<>();
        for (UpgradeSchematic schematic : schematics) {
            OutcomePreview[] previews = schematic.getPreviews(
                    tetraInsight$itemStack, tetraInsight$slot);
            if (previews.length != 1 || previews[0].variantKey == null || previews[0].level <= 0) {
                continue;
            }

            boolean available = !(schematic instanceof HoloDisplaySchematicAccess display)
                    || display.tetraInsight$isAvailable();
            ImprovementChainEntry entry = new ImprovementChainEntry(
                    schematic, previews[0], available);
            entriesBySchematic.put(schematic, entry);
            entriesByKey.computeIfAbsent(previews[0].variantKey, ignored -> new ArrayList<>())
                    .add(entry);
        }

        Set<String> chainKeys = new HashSet<>();
        entriesByKey.forEach((key, entries) -> {
            if (entries.stream().anyMatch(entry -> entry.schematic().isHoning())) {
                chainKeys.add(key);
            }
        });

        Set<String> renderedChains = new HashSet<>();
        List<ImprovementDisplayEntry> displayEntries = new ArrayList<>();
        for (UpgradeSchematic schematic : schematics) {
            ImprovementChainEntry entry = entriesBySchematic.get(schematic);
            if (entry == null || !chainKeys.contains(entry.preview().variantKey)) {
                displayEntries.add(ImprovementDisplayEntry.single(schematic));
                continue;
            }

            String improvementKey = entry.preview().variantKey;
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

        PaginationWindow window = PaginationWindow.of(
                displayEntries.size(), tetraInsight$currentPage, tetraInsight$PAGE_SIZE);
        tetraInsight$currentPage = window.currentPage();
        tetraInsight$clearRows();
        for (int index = window.startIndex(); index < window.endIndex(); index++) {
            ImprovementDisplayEntry displayEntry = displayEntries.get(index);
            GuiHorizontalLayoutGroup targetGroup = tetraInsight$shortestRow();
            HoloImprovementGui improvement = tetraInsight$addImprovement(
                    targetGroup, displayEntry.schematic(),
                    tetraInsight$itemStack, tetraInsight$slot);
            if (displayEntry.isChain()) {
                ((HoloImprovementGuiExtension) improvement).tetraInsight$setImprovementChain(
                        displayEntry.improvementKey(), displayEntry.chain(),
                        tetraInsight$itemStack);
            }
            improvement.updateSelection(
                    tetraInsight$itemStack, tetraInsight$selectedOutcomes);
        }

        tetraInsight$refreshImprovementLayout();
        tetraInsight$updatePageControls(
                tetraInsight$currentPage + 1, window.totalPages());
        tetraInsight$renderDirty = false;
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
        if (elapsedMillis >= 10) {
            io.github.createdelight.tetrainsight.TetraInsight.LOGGER.info(
                    "Rendered Tetra improvement page {}/{} in {} ms: groups={}, visible={}",
                    tetraInsight$currentPage + 1, window.totalPages(), elapsedMillis,
                    displayEntries.size(), improvements.size());
        }
    }

    @Unique
    private boolean tetraInsight$changePage(int delta) {
        UpgradeSchematic[] schematics = tetraInsight$showApplicable
                ? tetraInsight$allSchematics
                : Arrays.stream(tetraInsight$allSchematics)
                        .filter(schematic -> !(schematic instanceof HoloDisplaySchematicAccess display)
                                || display.tetraInsight$isAvailable())
                        .toArray(UpgradeSchematic[]::new);
        int displayCount = tetraInsight$countDisplayEntries(schematics);
        int nextPage = PaginationWindow.of(
                displayCount, tetraInsight$currentPage + delta,
                tetraInsight$PAGE_SIZE).currentPage();
        if (nextPage == tetraInsight$currentPage) {
            return false;
        }
        tetraInsight$currentPage = nextPage;
        tetraInsight$renderSchematics();
        return true;
    }

    @Unique
    private int tetraInsight$countDisplayEntries(UpgradeSchematic[] schematics) {
        Set<String> chainKeys = new HashSet<>();
        Map<String, Integer> countsByKey = new LinkedHashMap<>();
        int ordinary = 0;
        for (UpgradeSchematic schematic : schematics) {
            OutcomePreview[] previews = schematic.getPreviews(
                    tetraInsight$itemStack, tetraInsight$slot);
            if (previews.length != 1 || previews[0].variantKey == null
                    || previews[0].level <= 0) {
                ordinary++;
                continue;
            }
            String key = previews[0].variantKey;
            countsByKey.merge(key, 1, Integer::sum);
            if (schematic.isHoning()) {
                chainKeys.add(key);
            }
        }
        int grouped = countsByKey.entrySet().stream()
                .mapToInt(entry -> chainKeys.contains(entry.getKey()) ? 1 : entry.getValue())
                .sum();
        return ordinary + grouped;
    }

    @Unique
    private void tetraInsight$clearRows() {
        improvements.clear();
        for (GuiHorizontalLayoutGroup group : groups) {
            group.clearChildren();
            group.setWidth(0);
        }
    }

    @Unique
    private void tetraInsight$refreshImprovementLayout() {
        for (HoloImprovementGui improvement : improvements) {
            ((HoloImprovementGuiExtension) improvement).tetraInsight$refreshLayoutWidth();
        }
        for (GuiHorizontalLayoutGroup group : groups) {
            group.forceLayout();
        }
        container.markDirty();
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
        return Arrays.stream(groups)
                .min(Comparator.comparingInt(GuiHorizontalLayoutGroup::getWidth))
                .orElse(groups[0]);
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
