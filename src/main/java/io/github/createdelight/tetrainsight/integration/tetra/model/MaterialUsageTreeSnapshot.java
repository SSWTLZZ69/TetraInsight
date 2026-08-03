package io.github.createdelight.tetrainsight.integration.tetra.model;

import java.util.List;

public record MaterialUsageTreeSnapshot(List<MaterialItemUsageSnapshot> items) {
    public MaterialUsageTreeSnapshot {
        items = List.copyOf(items);
    }

    public int moduleCount() {
        return items.stream().mapToInt(item -> item.modules().size()).sum();
    }

    public int improvementCount() {
        return items.stream()
                .flatMap(item -> item.modules().stream())
                .mapToInt(module -> module.improvements().size())
                .sum();
    }
}
