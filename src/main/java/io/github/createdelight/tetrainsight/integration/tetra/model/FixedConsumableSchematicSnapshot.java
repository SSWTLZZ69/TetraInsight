package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;
import java.util.Set;

public record FixedConsumableSchematicSnapshot(
        String schematicKey,
        Set<String> aliases,
        List<FixedConsumableOutcomeSnapshot> outcomes
) {
    public FixedConsumableSchematicSnapshot {
        aliases = Set.copyOf(aliases);
        outcomes = List.copyOf(outcomes);
    }

    public boolean matches(String key) {
        return schematicKey.equals(key) || aliases.contains(key);
    }
}
