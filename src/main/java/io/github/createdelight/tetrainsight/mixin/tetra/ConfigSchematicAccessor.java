package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.module.schematic.ConfigSchematic;
import se.mickelus.tetra.module.schematic.SchematicDefinition;

@Mixin(value = ConfigSchematic.class, remap = false)
public interface ConfigSchematicAccessor {
    @Accessor("definition")
    SchematicDefinition tetraInsight$getDefinition();
}
