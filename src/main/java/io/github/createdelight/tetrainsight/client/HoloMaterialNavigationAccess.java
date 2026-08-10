package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;

public interface HoloMaterialNavigationAccess {
    void tetraInsight$openMaterial(String materialKey, Runnable closeCallback);

    void tetraInsight$openSpecialMaterial(ItemStack stack, Runnable closeCallback);

    void tetraInsight$navigateMaterial(String materialKey);
}
