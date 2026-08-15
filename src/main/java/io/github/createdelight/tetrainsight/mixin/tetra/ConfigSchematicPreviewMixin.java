package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.PreviewPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import se.mickelus.tetra.module.schematic.ConfigSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.function.Predicate;

@Mixin(value = ConfigSchematic.class, remap = false)
public class ConfigSchematicPreviewMixin {
    @ModifyArg(
            method = "getPreviews",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;",
                    ordinal = 1),
            index = 0,
            remap = false)
    private Predicate<OutcomePreview> tetraInsight$ignorePreviewlessOutcomes(
            Predicate<OutcomePreview> distinctByVariant) {
        return PreviewPredicate.rejectingNulls(distinctByVariant);
    }
}
