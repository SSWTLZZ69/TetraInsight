package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.OutcomeStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

/**
 * Marks an outcome selected from a honing chain. Tetra's native improvement
 * selection list is intended for stackable ordinary improvements, while a
 * honing chain is a single preview lock owned by Tetra Insight.
 */
public final class HoloHoningOutcomeStack extends OutcomeStack {
    private final UpgradeSchematic schematic;
    private final OutcomePreview preview;
    private final String chainKey;

    public HoloHoningOutcomeStack(UpgradeSchematic schematic, OutcomePreview preview) {
        this(schematic, preview, preview.variantKey);
    }

    public HoloHoningOutcomeStack(UpgradeSchematic schematic, OutcomePreview preview,
            String chainKey) {
        super(schematic, preview);
        this.schematic = schematic;
        this.preview = preview;
        this.chainKey = chainKey == null ? "" : chainKey;
    }

    public UpgradeSchematic schematic() {
        return schematic;
    }

    public OutcomePreview preview() {
        return preview;
    }

    public String chainKey() {
        return chainKey;
    }
}
