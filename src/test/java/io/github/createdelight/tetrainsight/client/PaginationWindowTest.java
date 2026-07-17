package io.github.createdelight.tetrainsight.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationWindowTest {
    @Test
    void coversSorterBoundaryMatrix() {
        assertWindow(0, 0, 1, 0, 0);
        assertWindow(1, 0, 1, 0, 1);
        assertWindow(9, 0, 1, 0, 9);
        assertWindow(10, 0, 2, 0, 9);
        assertWindow(10, 1, 2, 9, 10);
        assertWindow(18, 1, 2, 9, 18);
        assertWindow(19, 2, 3, 18, 19);
        assertWindow(30, 3, 4, 27, 30);
    }

    @Test
    void clampsRequestedPage() {
        assertEquals(0, PaginationWindow.of(10, -4, 9).currentPage());
        assertEquals(1, PaginationWindow.of(10, 8, 9).currentPage());
    }

    @Test
    void coversImprovementPageBoundary() {
        PaginationWindow first = PaginationWindow.of(56, 0, 12);
        assertEquals(5, first.totalPages());
        assertEquals(0, first.startIndex());
        assertEquals(12, first.endIndex());

        PaginationWindow last = PaginationWindow.of(56, 4, 12);
        assertEquals(5, last.totalPages());
        assertEquals(48, last.startIndex());
        assertEquals(56, last.endIndex());
        assertEquals(8, last.visibleCount());
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> PaginationWindow.of(10, 0, 0));
    }

    private static void assertWindow(int totalItems, int page, int totalPages, int start, int end) {
        PaginationWindow window = PaginationWindow.of(totalItems, page, 9);
        assertEquals(totalPages, window.totalPages());
        assertEquals(start, window.startIndex());
        assertEquals(end, window.endIndex());
        assertEquals(end - start, window.visibleCount());
    }
}
