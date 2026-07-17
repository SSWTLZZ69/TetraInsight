package io.github.createdelight.tetrainsight.client;

import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloMaterialTranslationGui;

public interface WorkbenchMaterialInfoAccess {
    void tetraInsight$showAsRowLink(
            String slotName,
            HoloMaterialTranslationGui translation,
            int width
    );

    void tetraInsight$restoreCompactIcon();
}
