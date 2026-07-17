package io.github.createdelight.tetrainsight.client;

import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.module.schematic.SchematicType;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public final class HoloDisplaySchematic {
    private HoloDisplaySchematic() {
    }

    public static UpgradeSchematic wrap(UpgradeSchematic delegate, boolean available,
            ItemStack cachedStack, String cachedSlot, OutcomePreview[] cachedPreviews) {
        ItemStack stackSnapshot = cachedStack.copy();
        OutcomePreview[] previewSnapshot = Arrays.copyOf(cachedPreviews, cachedPreviews.length);
        return (UpgradeSchematic) Proxy.newProxyInstance(
                UpgradeSchematic.class.getClassLoader(),
                new Class<?>[] { UpgradeSchematic.class, HoloDisplaySchematicAccess.class },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == HoloDisplaySchematicAccess.class) {
                        return switch (method.getName()) {
                            case "tetraInsight$isAvailable" -> available;
                            case "tetraInsight$delegate" -> delegate;
                            default -> throw new IllegalStateException(method.getName());
                        };
                    }
                    if (method.getName().equals("getType") && delegate.isHoning()) {
                        return SchematicType.improvement;
                    }
                    if (method.getName().equals("getPreviews")
                            && args != null && args.length == 2
                            && args[0] instanceof ItemStack requestedStack
                            && java.util.Objects.equals(cachedSlot, args[1])
                            && ItemStack.matches(stackSnapshot, requestedStack)) {
                        return Arrays.copyOf(previewSnapshot, previewSnapshot.length);
                    }
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> delegate.toString();
                            case "hashCode" -> delegate.hashCode();
                            case "equals" -> proxy == args[0];
                            default -> method.invoke(delegate, args);
                        };
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }
}
