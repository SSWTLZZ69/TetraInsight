package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.gui.stats.getter.IStatFormat;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;

import java.util.Comparator;
import java.util.function.Function;

public final class ContextualStatSorter implements IStatSorter {
    private final String semanticKey;
    private final String name;
    private final IStatGetter getter;
    private final IStatFormat format;

    public ContextualStatSorter(String semanticKey, String name, IStatGetter getter, IStatFormat format) {
        this.semanticKey = semanticKey;
        this.name = name;
        this.getter = getter;
        this.format = format;
    }

    public String semanticKey() {
        return semanticKey;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T> Comparator<T> compare(Player player, Function<? super T, ItemStack> stackGetter) {
        return Comparator.comparingDouble(value -> -getter.getValue(player, stackGetter.apply(value)));
    }

    @Override
    public String getValue(Player player, ItemStack stack) {
        return format.get(getter.getValue(player, stack));
    }

    @Override
    public boolean shouldShow(Player player, ItemStack stack) {
        return getter.shouldShow(player, stack, stack);
    }
}
