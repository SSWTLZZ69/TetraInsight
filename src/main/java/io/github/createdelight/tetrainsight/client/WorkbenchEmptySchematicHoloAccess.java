package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.items.modular.IModularItem;

public interface WorkbenchEmptySchematicHoloAccess {
    void tetraInsight$setHoloSlotContext(
            Player player,
            WorkbenchTile workbench,
            @Nullable IModularItem item,
            ItemStack itemStack,
            @Nullable String slot
    );
}
