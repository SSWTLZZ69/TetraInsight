package io.github.createdelight.tetrainsight.mixin.tetra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import se.mickelus.tetra.module.schematic.requirement.CraftingRequirement;
import se.mickelus.tetra.module.schematic.requirement.NotRequirement;

@Mixin(value = NotRequirement.class, remap = false)
public interface NotRequirementAccessor {
    @Accessor("requirement")
    CraftingRequirement tetraInsight$getRequirement();
}
