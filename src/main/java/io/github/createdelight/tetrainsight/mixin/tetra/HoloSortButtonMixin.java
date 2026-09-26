package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.ClearSorterOption;
import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.client.ContextualSorterFactory;
import io.github.createdelight.tetrainsight.client.HoloSortMaterialScalingAccess;
import io.github.createdelight.tetrainsight.client.HoloSortMergedListAccess;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.tetra.gui.stats.data.StatSorterStore;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.StatSorters;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSortButton;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.List;

@Mixin(value = HoloSortButton.class, remap = false)
public abstract class HoloSortButtonMixin implements HoloSortMaterialScalingAccess,
        HoloSortMergedListAccess {
    @Shadow
    private GuiString label;

    @Unique
    private int tetraInsight$lastContextualSorterCount = -1;

    @Unique
    private List<MaterialTranslationEntry> tetraInsight$actualMaterialScaling = List.of();

    @Unique
    private MaterialSchematicSnapshot tetraInsight$schematicSnapshot;

    @Unique
    private ContextualSorterFactory.MergeContext tetraInsight$sorterContext;

    @Unique
    private boolean tetraInsight$sorterContextComputed;

    @Unique
    private List<IStatSorter> tetraInsight$mergedSorters = List.of();

    @Unique
    private ContextualSorterFactory.MergeContext tetraInsight$context() {
        if (!tetraInsight$sorterContextComputed) {
            tetraInsight$sorterContext = ContextualSorterFactory.compute(
                    tetraInsight$actualMaterialScaling, tetraInsight$schematicSnapshot);
            tetraInsight$sorterContextComputed = true;
        }
        return tetraInsight$sorterContext;
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "FIELD",
                    target = "Lse/mickelus/tetra/gui/stats/sorting/StatSorters;staticSorters:Ljava/util/List;",
                    opcode = Opcodes.GETSTATIC),
            remap = false)
    private List<IStatSorter> tetraInsight$filterStaticSorters(OutcomePreview[] previews) {
        return ContextualSorterFactory.filterBase(
                StatSorters.staticSorters.stream()
                        .map(sorter -> sorter == StatSorters.none
                                ? ClearSorterOption.INSTANCE
                                : sorter)
                        .toList(),
                previews.length > 0 ? tetraInsight$context() : null);
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "FIELD",
                    target = "Lse/mickelus/tetra/gui/stats/sorting/StatSorters;derivedSorters:Ljava/util/List;",
                    opcode = Opcodes.GETSTATIC),
            remap = false)
    private List<IStatSorter> tetraInsight$addContextualSorters(OutcomePreview[] previews) {
        List<IStatSorter> original = StatSorters.derivedSorters;
        if (previews.length == 0 || Minecraft.getInstance().player == null) {
            tetraInsight$mergedSorters = original;
            return original;
        }
        ContextualSorterFactory.MergeContext context = tetraInsight$context();
        List<IStatSorter> merged = ContextualSorterFactory.mergeWithDerived(original, context);
        int contextualCount = context.generatedCount();
        if (contextualCount != tetraInsight$lastContextualSorterCount) {
            TetraInsight.LOGGER.info(
                    "Added {} contextual sorters ({} module, {} material; scaling entries: {})",
                    contextualCount, context.moduleSorters().size(),
                    context.materialSorters().size(),
                    tetraInsight$actualMaterialScaling.size());
            tetraInsight$lastContextualSorterCount = contextualCount;
        }
        tetraInsight$mergedSorters = merged;
        return merged;
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lse/mickelus/tetra/gui/stats/data/StatSorterStore;getSorters()[Lse/mickelus/tetra/gui/stats/sorting/IStatSorter;"),
            remap = false)
    private IStatSorter[] tetraInsight$filterStoreSorters(StatSorterStore store,
            OutcomePreview[] previews) {
        return ContextualSorterFactory.filterBase(
                        List.of(store.getSorters()),
                        previews.length > 0 ? tetraInsight$context() : null)
                .toArray(IStatSorter[]::new);
    }

    /**
     * Pins the popover row order after Tetra applied its own filter+sort:
     * clear action, module section (header + entries), material section, then
     * everything else in Tetra's order. Priority alone cannot express this
     * (the clear action must sit above every generated group).
     */
    @ModifyArg(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lse/mickelus/tetra/items/modular/impl/holo/gui/craft/schematic/HoloSortPopover;update([Lse/mickelus/tetra/gui/stats/sorting/IStatSorter;)V"),
            remap = false)
    private IStatSorter[] tetraInsight$orderPopoverSorters(IStatSorter[] sorters) {
        return java.util.Arrays.stream(sorters)
                .sorted(java.util.Comparator
                        .comparingInt(this::tetraInsight$sectionOrder)
                        .thenComparing(IStatSorter::getName))
                .toArray(IStatSorter[]::new);
    }

    @Unique
    private int tetraInsight$sectionOrder(IStatSorter sorter) {
        if (sorter == ClearSorterOption.INSTANCE) {
            return 0;
        }
        if (sorter instanceof io.github.createdelight.tetrainsight.client.SortSectionHeader header) {
            return header.getPriority() == se.mickelus.tetra.module.Priority.HIGHEST ? 1 : 3;
        }
        if (sorter instanceof io.github.createdelight.tetrainsight.client.ContextualStatSorter contextual) {
            return contextual.getPriority() == se.mickelus.tetra.module.Priority.HIGHER ? 2 : 4;
        }
        return 5;
    }

    @Override
    @Unique
    public List<IStatSorter> tetraInsight$getMergedSorters() {
        return tetraInsight$mergedSorters;
    }

    @Override
    @Unique
    public void tetraInsight$setActualMaterialScaling(List<MaterialTranslationEntry> entries,
            MaterialSchematicSnapshot schematic) {
        tetraInsight$actualMaterialScaling = List.copyOf(entries);
        tetraInsight$schematicSnapshot = schematic;
        tetraInsight$sorterContext = null;
        tetraInsight$sorterContextComputed = false;
    }

    @Inject(method = "onSelect", at = @At("RETURN"), remap = false)
    private void tetraInsight$restoreIdleLabelAfterClear(IStatSorter sorter, CallbackInfo ci) {
        if (sorter == ClearSorterOption.INSTANCE) {
            label.setString(StatSorters.none.getName());
        }
    }
}
