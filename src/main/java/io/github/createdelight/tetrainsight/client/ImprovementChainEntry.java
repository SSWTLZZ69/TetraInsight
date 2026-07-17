package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public record ImprovementChainEntry(
        UpgradeSchematic schematic,
        OutcomePreview preview,
        boolean available) {
}
