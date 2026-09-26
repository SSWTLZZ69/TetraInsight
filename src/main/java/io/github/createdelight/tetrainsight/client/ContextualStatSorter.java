package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.gui.stats.getter.IStatFormat;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.module.Priority;

import java.util.Comparator;
import java.util.function.Function;

/**
 * A generated sorter option whose existence is justified by data (module
 * extract outputs or material-carried stats). shouldShow delegates to the
 * backing getter: the split operation-specific getters (addition/multiply)
 * report presence per operation, which is exactly what the caller needs.
 */
public final class ContextualStatSorter implements IStatSorter {
    private final String semanticKey;
    private final String name;
    private final IStatGetter getter;
    private final IStatFormat format;
    private final Priority priority;

    public ContextualStatSorter(String semanticKey, String name, IStatGetter getter,
            IStatFormat format, Priority priority) {
        this.semanticKey = semanticKey;
        this.name = name;
        this.getter = getter;
        this.format = format;
        this.priority = priority;
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

    @Override
    public Priority getPriority() {
        return priority;
    }
}
