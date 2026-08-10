package io.github.createdelight.tetrainsight.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;
import se.mickelus.tetra.ClientScheduler;
import se.mickelus.tetra.client.keymap.TetraKeyMappings;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;

public final class MaterialDossierShortcut {
    private static final long HOVER_VALIDITY_MS = 300L;

    public static final KeyMapping OPEN = new KeyMapping(
            "key.tetra_insight.material_dossier",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            TetraKeyMappings.bindingGroup
    );

    private static List<MaterialProfileSnapshot> hoveredProfiles = List.of();
    private static ItemStack hoveredStack = ItemStack.EMPTY;
    private static boolean hoveredSpecialOnly;
    private static long lastHoverAt;

    private MaterialDossierShortcut() {
    }

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    public static void setHoveredProfiles(
            List<MaterialProfileSnapshot> profiles,
            ItemStack stack,
            boolean specialOnly
    ) {
        hoveredProfiles = List.copyOf(profiles);
        hoveredStack = stack != null ? stack.copy() : ItemStack.EMPTY;
        hoveredSpecialOnly = profiles.isEmpty() && specialOnly;
        lastHoverAt = Util.getMillis();
    }

    public static Component keyName() {
        return OPEN.getTranslatedKeyMessage();
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if ((hoveredProfiles.isEmpty() && !hoveredSpecialOnly)
                || Util.getMillis() - lastHoverAt > HOVER_VALIDITY_MS
                || !OPEN.isActiveAndMatches(InputConstants.getKey(
                        event.getKeyCode(), event.getScanCode()))) {
            return;
        }

        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (ModularHolosphereItem.findHolosphere(
                minecraft.player,
                minecraft.level,
                minecraft.player.blockPosition()).isEmpty()) {
            minecraft.player.displayClientMessage(
                    Component.translatable("tetra_insight.material.shortcut.no_holosphere")
                            .withStyle(ChatFormatting.RED),
                    true);
            return;
        }

        Screen previousScreen = event.getScreen();
        HoloGui holoGui = HoloGui.getInstance();
        ((HoloMaterialDossierLifecycleAccess) holoGui)
                .tetraInsight$resetMaterialDossier();
        if (hoveredProfiles.isEmpty()) {
            MaterialDossierSession.startSpecial(hoveredStack);
        } else {
            MaterialDossierSession.start(hoveredProfiles, hoveredStack);
        }
        minecraft.setScreen(holoGui);
        Runnable closeCallback = () -> ClientScheduler.schedule(
                0,
                () -> Minecraft.getInstance().setScreen(previousScreen));
        if (hoveredProfiles.isEmpty()) {
            ((HoloMaterialNavigationAccess) holoGui)
                    .tetraInsight$openSpecialMaterial(hoveredStack, closeCallback);
        } else {
            MaterialProfileSnapshot profile = hoveredProfiles.get(0);
            ((HoloMaterialNavigationAccess) holoGui)
                    .tetraInsight$openMaterial(profile.materialKey(), closeCallback);
        }
        holoGui.onShow();
    }
}
