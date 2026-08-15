package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;

public record EffectApplicabilityPathSnapshot(
        List<EffectScope> scopes,
        List<EffectTrigger> triggers,
        EffectApplicabilityState previewState,
        String stackingTranslationKey,
        String evidenceTranslationKey) {

    public EffectApplicabilityPathSnapshot {
        scopes = List.copyOf(scopes);
        triggers = List.copyOf(triggers);
    }
}
