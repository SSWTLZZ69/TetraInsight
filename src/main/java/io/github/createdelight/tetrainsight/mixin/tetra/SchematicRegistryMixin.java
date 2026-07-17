package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.SchematicDefinition;

import java.util.Map;

@Mixin(value = SchematicRegistry.class, remap = false)
public abstract class SchematicRegistryMixin {
    @Inject(method = "setupSchematics", at = @At("HEAD"), remap = false)
    private void tetraInsight$beginReload(Map<ResourceLocation, SchematicDefinition> data, CallbackInfo ci) {
        TetraDataProbe.beginSchematicReload();
    }

    @Inject(method = "processDefinition", at = @At("HEAD"), remap = false)
    private void tetraInsight$capture(SchematicDefinition definition, CallbackInfo ci) {
        TetraDataProbe.captureSchematic(definition);
    }

    @Inject(method = "setupSchematics", at = @At("RETURN"), remap = false)
    private void tetraInsight$finishReload(Map<ResourceLocation, SchematicDefinition> data, CallbackInfo ci) {
        TetraDataProbe.finishSchematicReload();
    }
}
