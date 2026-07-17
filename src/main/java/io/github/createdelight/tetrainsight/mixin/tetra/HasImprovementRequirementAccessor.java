package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.module.schematic.requirement.HasImprovementRequirement;

@Mixin(value = HasImprovementRequirement.class, remap = false)
public interface HasImprovementRequirementAccessor {
    @Accessor("improvement")
    String tetraInsight$getImprovement();
}
