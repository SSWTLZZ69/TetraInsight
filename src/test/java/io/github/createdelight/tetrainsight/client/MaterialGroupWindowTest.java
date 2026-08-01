package io.github.createdelight.tetrainsight.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaterialGroupWindowTest {
    @Test
    void leavesSmallGroupsUncollapsed() {
        MaterialGroupWindow window = MaterialGroupWindow.of(8, -1, false);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), window.visibleIndices());
        assertEquals(0, window.hiddenCount());
    }

    @Test
    void reservesTheEighthCellForExpansion() {
        MaterialGroupWindow window = MaterialGroupWindow.of(20, -1, false);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), window.visibleIndices());
        assertEquals(13, window.hiddenCount());
    }

    @Test
    void startsCollapsingAtNineEntries() {
        MaterialGroupWindow window = MaterialGroupWindow.of(9, -1, false);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), window.visibleIndices());
        assertEquals(2, window.hiddenCount());
    }

    @Test
    void pinsASelectedMaterialOutsideTheCompactWindow() {
        MaterialGroupWindow window = MaterialGroupWindow.of(20, 17, false);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 17), window.visibleIndices());
        assertEquals(13, window.hiddenCount());
    }

    @Test
    void expansionShowsEveryMaterial() {
        MaterialGroupWindow window = MaterialGroupWindow.of(10, 9, true);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
                window.visibleIndices());
        assertEquals(0, window.hiddenCount());
    }

    @Test
    void usesTheSameSeventhEntryPinForSchematicVariants() {
        MaterialGroupWindow window = MaterialGroupWindow.of(
                30, 23, false, MaterialGroupWindow.COMPACT_MATERIAL_COUNT);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 23),
                window.visibleIndices());
        assertEquals(23, window.hiddenCount());
    }
}
