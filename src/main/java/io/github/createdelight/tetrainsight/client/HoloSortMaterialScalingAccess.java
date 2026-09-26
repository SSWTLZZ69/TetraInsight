package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;

import java.util.List;

public interface HoloSortMaterialScalingAccess {
    void tetraInsight$setActualMaterialScaling(List<MaterialTranslationEntry> entries,
            MaterialSchematicSnapshot schematic);
}
