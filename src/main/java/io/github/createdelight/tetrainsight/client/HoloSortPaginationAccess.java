package io.github.createdelight.tetrainsight.client;

public interface HoloSortPaginationAccess {
    int tetraInsight$currentPage();

    int tetraInsight$totalPages();

    int tetraInsight$visibleItemCount();

    int tetraInsight$filteredItemCount();

    void tetraInsight$setPage(int page);

    void tetraInsight$setQuery(String query);
}
