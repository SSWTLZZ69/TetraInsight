package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.IdentityHashMap;
import java.util.Map;

/** Associates the exact preview object owned by a rendered button with its schematic. */
public final class ImprovementPreviewContext {
    private static final Map<OutcomePreview, UpgradeSchematic> SCHEMATICS =
            new IdentityHashMap<>();

    private ImprovementPreviewContext() {
    }

    public static void clear() {
        SCHEMATICS.clear();
    }

    public static void register(UpgradeSchematic schematic, OutcomePreview[] previews) {
        for (OutcomePreview preview : previews) {
            SCHEMATICS.put(preview, schematic);
        }
    }

    public static UpgradeSchematic find(OutcomePreview preview) {
        return SCHEMATICS.get(preview);
    }
}
