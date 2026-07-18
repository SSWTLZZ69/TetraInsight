package io.github.createdelight.tetrainsight.integration.tetra;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialCandidateSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialGlyphTintSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialItemSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.Arrays;
import java.util.Optional;

public final class MaterialGlyphTintResolver {
    private MaterialGlyphTintResolver() {
    }

    public static Optional<MaterialGlyphTintSnapshot> resolve(
            String schematicKey, OutcomePreview preview) {
        if (!hasUsableMaterials(preview)) {
            return Optional.empty();
        }

        return TetraDataProbe.findSchematic(schematicKey).stream()
                .flatMap(schematic -> schematic.materialSlots().stream())
                .flatMap(slot -> slot.candidates().stream())
                .filter(candidate -> matchesAnySource(candidate, preview.materials))
                .max((left, right) -> Integer.compare(
                        matchScore(left, preview), matchScore(right, preview)))
                .map(candidate -> new MaterialGlyphTintSnapshot(
                        candidate.materialKey(), candidate.glyphTint()));
    }

    public static boolean hasUsableMaterials(OutcomePreview preview) {
        return preview != null
                && preview.materials != null
                && Arrays.stream(preview.materials)
                        .anyMatch(stack -> stack != null && !stack.isEmpty());
    }

    public static boolean requiresUsableMaterial(UpgradeSchematic schematic) {
        return schematic.getNumMaterialSlots() > 0
                && TetraDataProbe.findFixedConsumableSchematic(
                        schematic.getKey()).isEmpty();
    }

    public static boolean shouldDisplay(
            UpgradeSchematic schematic, OutcomePreview preview) {
        if (!requiresUsableMaterial(schematic)) {
            return true;
        }
        if (!hasUsableMaterials(preview)) {
            return false;
        }
        return TetraDataProbe.findSchematic(schematic.getKey()).isEmpty()
                || resolve(schematic.getKey(), preview).isPresent();
    }

    private static boolean matchesAnySource(MaterialCandidateSnapshot candidate, ItemStack[] materials) {
        return candidate.sourceItems().stream()
                .anyMatch(source -> Arrays.stream(materials)
                        .anyMatch(stack -> matches(source, stack)));
    }

    private static int matchScore(MaterialCandidateSnapshot candidate, OutcomePreview preview) {
        int score = 0;
        for (MaterialItemSource source : candidate.sourceItems()) {
            for (ItemStack stack : preview.materials) {
                if (matches(source, stack)) {
                    score += source.nbt().isEmpty() ? 10 : 20;
                }
            }
        }

        String candidateKey = candidate.materialKey();
        if (candidateKey != null && preview.variantKey != null) {
            String normalized = candidateKey.replace(':', '/');
            if (preview.variantKey.equals(candidateKey)
                    || preview.variantKey.endsWith(candidateKey)
                    || preview.variantKey.endsWith(normalized)) {
                score += 100;
            }
        }
        return score;
    }

    private static boolean matches(MaterialItemSource source, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !source.itemId().equals(itemId.toString())) {
            return false;
        }
        return source.nbt().isEmpty()
                || source.nbt().equals(stack.hasTag() ? stack.getTag().toString() : "");
    }
}
