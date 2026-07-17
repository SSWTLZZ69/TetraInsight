package io.github.createdelight.tetrainsight.client;

import java.util.Comparator;
import java.util.function.Function;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.StatSorters;
import se.mickelus.tetra.module.Priority;

/**
 * A clearly labelled UI option that delegates to Tetra's natural, unsorted
 * sorter. The wrapper is kept client-side and does not change sorting rules.
 */
public final class ClearSorterOption implements IStatSorter {
    public static final ClearSorterOption INSTANCE = new ClearSorterOption();

    private ClearSorterOption() {
    }

    @Override
    public String getName() {
        return I18n.get("tetra_insight.holo.sort.clear");
    }

    @Override
    public <T> Comparator<T> compare(Player player, Function<? super T, ItemStack> stackGetter) {
        return StatSorters.none.compare(player, stackGetter);
    }

    @Override
    public String getValue(Player player, ItemStack stack) {
        return StatSorters.none.getValue(player, stack);
    }

    @Override
    public boolean shouldShow(Player player, ItemStack stack) {
        return true;
    }

    @Override
    public Priority getPriority() {
        return StatSorters.none.getPriority();
    }
}
