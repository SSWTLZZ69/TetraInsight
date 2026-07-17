package io.github.createdelight.tetrainsight.client;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface PersistentVerticalTabButtonAccess {
    void tetraInsight$setPersistentLabel(@Nullable Component unavailableReason);

    boolean tetraInsight$hasContent();
}
