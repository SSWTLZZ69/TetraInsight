package io.github.createdelight.tetrainsight.integration.tetra.effect;

import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;

import java.util.List;
import java.util.stream.Stream;

public record EffectApplicabilityDefinition(
        List<EffectScope> scopes,
        List<EffectTrigger> triggers,
        String stackingTranslationKey,
        String evidenceTranslationKey) {

    public EffectApplicabilityDefinition {
        scopes = List.copyOf(scopes);
        triggers = List.copyOf(triggers);
    }

    @SafeVarargs
    public static List<EffectApplicabilityDefinition> merge(
            List<EffectApplicabilityDefinition>... sources) {
        return Stream.of(sources)
                .flatMap(List::stream)
                .distinct()
                .toList();
    }
}
