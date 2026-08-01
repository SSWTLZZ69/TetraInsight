package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloCraftSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.HoloSlotNavigationAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.HoloPage;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloRootBaseGui;

@Mixin(value = HoloGui.class, remap = false)
public abstract class HoloGuiMixin implements HoloSlotNavigationAccess {
    @Shadow
    @Final
    private HoloRootBaseGui[] pages;

    @Shadow
    private Runnable closeCallback;

    @Shadow
    private void changePage(HoloPage page) {
    }

    @Override
    public void tetraInsight$openSlot(
            IModularItem item,
            ItemStack itemStack,
            String slot,
            Runnable closeCallback
    ) {
        changePage(HoloPage.craft);
        ((HoloCraftSlotNavigationAccess) pages[HoloPage.craft.ordinal()])
                .tetraInsight$openSlot(item, itemStack, slot);
        this.closeCallback = closeCallback;
    }
}
