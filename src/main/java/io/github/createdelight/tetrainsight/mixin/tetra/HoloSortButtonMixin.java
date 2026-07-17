package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.ClearSorterOption;
import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.client.ContextualSorterFactory;
import io.github.createdelight.tetrainsight.client.HoloSortMaterialScalingAccess;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.StatSorters;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSortButton;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.List;

@Mixin(value = HoloSortButton.class, remap = false)
public abstract class HoloSortButtonMixin implements HoloSortMaterialScalingAccess {
    @Shadow
    private GuiString label;

    @Unique
    private int tetraInsight$lastContextualSorterCount = -1;

    @Unique
    private List<MaterialTranslationEntry> tetraInsight$actualMaterialScaling = List.of();

    @Redirect(
            method = "update",
            at = @At(
                    value = "FIELD",
                    target = "Lse/mickelus/tetra/gui/stats/sorting/StatSorters;derivedSorters:Ljava/util/List;",
                    opcode = Opcodes.GETSTATIC),
            remap = false)
    private List<IStatSorter> tetraInsight$addContextualSorters(OutcomePreview[] previews) {
        List<IStatSorter> original = StatSorters.derivedSorters;
        List<IStatSorter> merged = ContextualSorterFactory.mergeWithDerived(
                previews, Minecraft.getInstance().player, original,
                tetraInsight$actualMaterialScaling);
        int contextualCount = ContextualSorterFactory.contextualCount(merged, original);
        if (contextualCount != tetraInsight$lastContextualSorterCount) {
            TetraInsight.LOGGER.info("Added {} contextual fallback sorters from actual material scaling",
                    contextualCount);
            tetraInsight$lastContextualSorterCount = contextualCount;
        }
        return merged;
    }

    @Override
    @Unique
    public void tetraInsight$setActualMaterialScaling(List<MaterialTranslationEntry> entries) {
        tetraInsight$actualMaterialScaling = List.copyOf(entries);
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "FIELD",
                    target = "Lse/mickelus/tetra/gui/stats/sorting/StatSorters;staticSorters:Ljava/util/List;",
                    opcode = Opcodes.GETSTATIC),
            remap = false)
    private List<IStatSorter> tetraInsight$makeClearActionExplicit(OutcomePreview[] previews) {
        return StatSorters.staticSorters.stream()
                .map(sorter -> sorter == StatSorters.none
                        ? ClearSorterOption.INSTANCE
                        : sorter)
                .toList();
    }

    @Inject(method = "onSelect", at = @At("RETURN"), remap = false)
    private void tetraInsight$restoreIdleLabelAfterClear(IStatSorter sorter, CallbackInfo ci) {
        if (sorter == ClearSorterOption.INSTANCE) {
            label.setString(StatSorters.none.getName());
        }
    }
}
