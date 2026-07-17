package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.Arrays;
import java.util.List;

public record ImprovementDisplayEntry(
        UpgradeSchematic schematic,
        String improvementKey,
        List<ImprovementChainEntry> chain,
        OutcomePreview[] previews
) {
    public ImprovementDisplayEntry {
        chain = List.copyOf(chain);
        previews = Arrays.copyOf(previews, previews.length);
    }

    public static ImprovementDisplayEntry single(UpgradeSchematic schematic,
            OutcomePreview[] previews) {
        return new ImprovementDisplayEntry(schematic, "", List.of(), previews);
    }

    public static ImprovementDisplayEntry chain(String improvementKey,
            List<ImprovementChainEntry> entries) {
        return new ImprovementDisplayEntry(
                entries.get(0).schematic(), improvementKey, entries,
                entries.stream().map(ImprovementChainEntry::preview)
                        .toArray(OutcomePreview[]::new));
    }

    public boolean isChain() {
        return !chain.isEmpty();
    }

    @Override
    public OutcomePreview[] previews() {
        return Arrays.copyOf(previews, previews.length);
    }
}
