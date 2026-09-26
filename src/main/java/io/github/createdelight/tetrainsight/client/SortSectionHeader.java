package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.StatSorters;
import se.mickelus.tetra.module.Priority;

import java.util.Comparator;
import java.util.function.Function;

/**
 * A non-functional row that visually labels a generated sorter section inside
 * the popover. It never sorts (delegates to the neutral comparator) and simply
 * renders its localized group label like a regular option.
 */
public final class SortSectionHeader implements IStatSorter {
    private final String label;
    private final Priority priority;

    public SortSectionHeader(String label, Priority priority) {
        this.label = label;
        this.priority = priority;
    }

    @Override
    public String getName() {
        return label;
    }

    @Override
    public <T> Comparator<T> compare(Player player, Function<? super T, ItemStack> stackGetter) {
        return StatSorters.none.compare(player, stackGetter);
    }

    @Override
    public String getValue(Player player, ItemStack stack) {
        return "";
    }

    @Override
    public boolean shouldShow(Player player, ItemStack stack) {
        return true;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }
}
