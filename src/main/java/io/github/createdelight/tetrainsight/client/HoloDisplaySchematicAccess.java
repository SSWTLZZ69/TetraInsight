package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public interface HoloDisplaySchematicAccess {
    boolean tetraInsight$isAvailable();

    UpgradeSchematic tetraInsight$delegate();
}
