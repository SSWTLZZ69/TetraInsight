package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;

public interface HoloSlotNavigationAccess {
    void tetraInsight$openSlot(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            Runnable closeCallback
    );
}
