package io.github.createdelight.tetrainsight.integration.tetra;

import org.junit.jupiter.api.Test;
import se.mickelus.mutil.util.Filter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewPredicateTest {
    @Test
    void rejectsNullBeforeCallingThePreviewPredicate() {
        AtomicInteger calls = new AtomicInteger();
        Predicate<String> predicate = PreviewPredicate.rejectingNulls(value -> {
            calls.incrementAndGet();
            return value.startsWith("tetra:");
        });

        assertFalse(predicate.test(null));
        assertEquals(0, calls.get());
        assertTrue(predicate.test("tetra:binding"));
        assertFalse(predicate.test("other:binding"));
        assertEquals(2, calls.get());
    }

    @Test
    void removesPreviewlessOutcomesBeforeMutilReadsTheirKey() {
        List<TestPreview> previews = Stream.of(
                        null,
                        new TestPreview("tetra_insight:arcane_capacity", 1),
                        new TestPreview("tetra_insight:arcane_capacity", 1))
                .filter(PreviewPredicate.rejectingNulls(
                        Filter.distinct(TestPreview::variantKey)))
                .toList();

        assertEquals(List.of(
                new TestPreview("tetra_insight:arcane_capacity", 1)), previews);
    }

    private record TestPreview(String variantKey, int level) {
    }
}
