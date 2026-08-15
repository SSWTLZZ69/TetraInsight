package io.github.createdelight.tetrainsight.integration.tetra;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Predicate adapters used while Tetra builds UI-only outcome previews.
 */
public final class PreviewPredicate {
    private PreviewPredicate() {
    }

    public static <T> Predicate<T> rejectingNulls(Predicate<T> delegate) {
        Objects.requireNonNull(delegate);
        return value -> value != null && delegate.test(value);
    }
}
