package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;
import java.util.stream.Stream;

public record EffectApplicabilitySnapshot(
        String effectKey,
        List<EffectApplicabilityPathSnapshot> paths,
        EffectApplicabilityState previewState) {

    public EffectApplicabilitySnapshot {
        paths = List.copyOf(paths);
    }

    public List<EffectScope> scopes() {
        return distinct(paths.stream().flatMap(path -> path.scopes().stream()));
    }

    public List<EffectTrigger> triggers() {
        return distinct(paths.stream().flatMap(path -> path.triggers().stream()));
    }

    private static <T> List<T> distinct(Stream<T> values) {
        return values.distinct().toList();
    }
}
