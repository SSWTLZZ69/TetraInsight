package io.github.createdelight.tetrainsight.integration.tetra.model;

public record MaterialUsageSnapshot(
        String schematicKey,
        int slotIndex,
        int requiredQuantity
) {
}
