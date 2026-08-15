package io.github.createdelight.tetrainsight.integration.tetra.effect;

import java.util.List;

public record EffectApplicabilityRuleSet(
        boolean replace,
        List<EffectApplicabilityDefinition> definitions) {

    public EffectApplicabilityRuleSet {
        definitions = List.copyOf(definitions);
    }
}
