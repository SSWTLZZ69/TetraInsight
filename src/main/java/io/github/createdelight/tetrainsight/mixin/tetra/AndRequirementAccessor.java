package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.module.schematic.requirement.AndRequirement;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;

@Mixin(value = AndRequirement.class, remap = false)
public interface AndRequirementAccessor {
    @Accessor("requirements")
    CraftingRequirement[] tetraInsight$getRequirements();
}
