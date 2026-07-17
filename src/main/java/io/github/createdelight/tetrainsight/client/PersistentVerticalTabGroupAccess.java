package io.github.createdelight.tetrainsight.client;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface PersistentVerticalTabGroupAccess {
    void tetraInsight$setPersistentLabels(@Nullable Component... unavailableReasons);
}
