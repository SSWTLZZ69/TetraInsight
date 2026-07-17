package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;

public record MaterialSlotSnapshot(
        int slotIndex,
        List<MaterialCandidateSnapshot> candidates
) {
    public MaterialSlotSnapshot {
        candidates = List.copyOf(candidates);
    }
}
