package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.data.ImprovementStore;
import se.mickelus.tetra.module.data.ImprovementData;
import se.mickelus.tetra.module.data.MaterialImprovementData;

import java.util.stream.Stream;

@Mixin(value = ImprovementStore.class, remap = false)
public abstract class ImprovementStoreMixin {
    @Inject(method = "processData()V", at = @At("HEAD"), remap = false)
    private void tetraInsight$beginReload(CallbackInfo ci) {
        TetraDataProbe.beginImprovementReload();
    }

    @Inject(method = "expandMaterialImprovement", at = @At("HEAD"), remap = false)
    private void tetraInsight$capture(MaterialImprovementData data,
            CallbackInfoReturnable<Stream<ImprovementData>> cir) {
        TetraDataProbe.captureImprovement(data);
    }

    @Inject(method = "processData()V", at = @At("RETURN"), remap = false)
    private void tetraInsight$finishReload(CallbackInfo ci) {
        TetraDataProbe.finishImprovementReload();
    }
}
