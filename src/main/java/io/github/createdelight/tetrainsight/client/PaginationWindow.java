package io.github.createdelight.tetrainsight.client;

public record PaginationWindow(
        int totalItems,
        int pageSize,
        int currentPage,
        int totalPages,
        int startIndex,
        int endIndex
) {
    public static PaginationWindow of(int totalItems, int requestedPage, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }

        int safeTotal = Math.max(0, totalItems);
        int totalPages = Math.max(1, (safeTotal + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int startIndex = Math.min(currentPage * pageSize, safeTotal);
        int endIndex = Math.min(startIndex + pageSize, safeTotal);
        return new PaginationWindow(safeTotal, pageSize, currentPage, totalPages, startIndex, endIndex);
    }

    public int visibleCount() {
        return endIndex - startIndex;
    }
}
