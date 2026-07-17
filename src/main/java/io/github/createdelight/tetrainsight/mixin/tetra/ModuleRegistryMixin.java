package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.module.ModuleRegistry;
import se.mickelus.tetra.module.data.MaterialVariantData;
import se.mickelus.tetra.module.data.ModuleData;
import se.mickelus.tetra.module.data.VariantData;

import java.util.Map;
import java.util.stream.Stream;

@Mixin(value = ModuleRegistry.class, remap = false)
public abstract class ModuleRegistryMixin {
    @Inject(method = "setupModules", at = @At("HEAD"), remap = false)
    private void tetraInsight$beginReload(Map<ResourceLocation, ModuleData> data, CallbackInfo ci) {
        TetraDataProbe.beginVariantReload();
    }

    @Inject(method = "expandMaterialVariant", at = @At("HEAD"), remap = false)
    private void tetraInsight$capture(MaterialVariantData source,
            CallbackInfoReturnable<Stream<VariantData>> cir) {
        TetraDataProbe.captureVariant(source);
    }

    @Inject(method = "setupModules", at = @At("RETURN"), remap = false)
    private void tetraInsight$finishReload(Map<ResourceLocation, ModuleData> data, CallbackInfo ci) {
        TetraDataProbe.finishVariantReload();
    }
}
