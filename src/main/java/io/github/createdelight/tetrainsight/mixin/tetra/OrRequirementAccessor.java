package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;
import se.mickelus.tetra.module.schematic.requirement.OrRequirement;

@Mixin(value = OrRequirement.class, remap = false)
public interface OrRequirementAccessor {
    @Accessor("requirements")
    CraftingRequirement[] tetraInsight$getRequirements();
}
