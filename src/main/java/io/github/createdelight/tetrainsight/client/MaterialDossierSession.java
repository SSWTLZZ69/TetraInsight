package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class MaterialDossierSession {
    private static List<MaterialProfileSnapshot> definitions = List.of();
    private static ItemStack sourceStack = ItemStack.EMPTY;
    private static int index;
    private static boolean autoOpen;

    private MaterialDossierSession() {
    }

    public static void start(List<MaterialProfileSnapshot> profiles, ItemStack stack) {
        definitions = List.copyOf(profiles);
        sourceStack = stack != null ? stack.copy() : ItemStack.EMPTY;
        index = 0;
        autoOpen = !definitions.isEmpty();
    }

    public static void startSpecial(ItemStack stack) {
        definitions = List.of();
        sourceStack = stack != null ? stack.copy() : ItemStack.EMPTY;
        index = 0;
        autoOpen = false;
    }

    public static void clear() {
        definitions = List.of();
        sourceStack = ItemStack.EMPTY;
        index = 0;
        autoOpen = false;
    }

    public static void cancelAutoOpen() {
        autoOpen = false;
    }

    public static ItemStack sourceStack() {
        return sourceStack.copy();
    }

    public static DefinitionPage pageFor(String materialKey) {
        for (int candidate = 0; candidate < definitions.size(); candidate++) {
            if (definitions.get(candidate).materialKey().equals(materialKey)) {
                index = candidate;
                return new DefinitionPage(candidate, definitions.size());
            }
        }
        return new DefinitionPage(0, 1);
    }

    public static Optional<MaterialProfileSnapshot> move(int offset) {
        if (definitions.size() < 2 || offset == 0) {
            return Optional.empty();
        }
        index = Math.floorMod(index + offset, definitions.size());
        autoOpen = false;
        return Optional.of(definitions.get(index));
    }

    public static boolean consumeAutoOpen(String materialKey) {
        if (!autoOpen || definitions.isEmpty()
                || !definitions.get(index).materialKey().equals(materialKey)) {
            return false;
        }
        autoOpen = false;
        return true;
    }

    public record DefinitionPage(int index, int total) {
    }
}
