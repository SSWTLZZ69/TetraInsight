package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.List;

public record ImprovementDisplayEntry(
        UpgradeSchematic schematic,
        String improvementKey,
        List<ImprovementChainEntry> chain
) {
    public ImprovementDisplayEntry {
        chain = List.copyOf(chain);
    }

    public static ImprovementDisplayEntry single(UpgradeSchematic schematic) {
        return new ImprovementDisplayEntry(schematic, "", List.of());
    }

    public static ImprovementDisplayEntry chain(String improvementKey,
            List<ImprovementChainEntry> entries) {
        return new ImprovementDisplayEntry(
                entries.get(0).schematic(), improvementKey, entries);
    }

    public boolean isChain() {
        return !chain.isEmpty();
    }
}
