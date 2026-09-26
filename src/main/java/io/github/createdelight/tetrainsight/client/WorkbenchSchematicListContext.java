package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The workbench schematic list entries only know their schematic; the slot
 * detail keeps the item and slot they apply to. The list mixin publishes that
 * context here so each list item can describe its crafting materials.
 */
public final class WorkbenchSchematicListContext {
    private static ItemStack itemStack = ItemStack.EMPTY;
    private static String slot = "";

    private WorkbenchSchematicListContext() {
    }

    public static void set(@Nullable ItemStack stack, @Nullable String slotKey) {
        itemStack = stack == null ? ItemStack.EMPTY : stack.copy();
        slot = slotKey == null ? "" : slotKey;
    }

    public static void clear() {
        itemStack = ItemStack.EMPTY;
        slot = "";
    }

    public static ItemStack itemStack() {
        return itemStack;
    }

    public static String slot() {
        return slot;
    }

    public static boolean present() {
        return !itemStack.isEmpty() && !slot.isEmpty();
    }
}
