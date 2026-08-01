package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloSlotNavigationAccess;
import io.github.createdelight.tetrainsight.client.WorkbenchEmptySchematicHoloAccess;
import io.github.createdelight.tetrainsight.client.WorkbenchHoloSlotButtonGui;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiText;
import se.mickelus.tetra.ClientScheduler;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.blocks.workbench.gui.GuiSchematicList;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.ModularHolosphereItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

@Mixin(value = GuiSchematicList.class, remap = false)
public abstract class GuiSchematicListMixin implements WorkbenchEmptySchematicHoloAccess {
    @Shadow
    @Final
    private GuiText emptyStateText;

    @Unique
    private WorkbenchHoloSlotButtonGui tetraInsight$holoButton;

    @Unique
    private Player tetraInsight$player;

    @Unique
    private WorkbenchTile tetraInsight$workbench;

    @Unique
    private IModularItem tetraInsight$item;

    @Unique
    private ItemStack tetraInsight$itemStack = ItemStack.EMPTY;

    @Unique
    private String tetraInsight$slot;

    @Unique
    private boolean tetraInsight$emptySchematics;

    @Unique
    private boolean tetraInsight$holoAvailable;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addEmptyListHoloButton(
            int x,
            int y,
            Consumer<UpgradeSchematic> schematicSelectionConsumer,
            CallbackInfo ci
    ) {
        tetraInsight$holoButton = new WorkbenchHoloSlotButtonGui(
                this::tetraInsight$openSlotInHolo,
                this::tetraInsight$hasHolosphere
        );
        tetraInsight$holoButton.setVisible(false);
        ((GuiSchematicList) (Object) this).addChild(tetraInsight$holoButton);
    }

    @Inject(method = "setSchematics", at = @At("RETURN"), remap = false)
    private void tetraInsight$trackEmptySchematics(
            UpgradeSchematic[] schematics,
            CallbackInfo ci
    ) {
        tetraInsight$emptySchematics = schematics.length == 0;
        tetraInsight$updateHoloButton();
    }

    @Override
    public void tetraInsight$setHoloSlotContext(
            Player player,
            WorkbenchTile workbench,
            @Nullable IModularItem item,
            ItemStack itemStack,
            @Nullable String slot
    ) {
        tetraInsight$player = player;
        tetraInsight$workbench = workbench;
        tetraInsight$item = item;
        tetraInsight$itemStack = itemStack.copy();
        tetraInsight$slot = slot;
        tetraInsight$holoAvailable = tetraInsight$findHolosphere();
        tetraInsight$updateHoloButton();
    }

    @Unique
    private void tetraInsight$updateHoloButton() {
        if (tetraInsight$holoButton == null) {
            return;
        }

        boolean visible = tetraInsight$emptySchematics
                && tetraInsight$item != null
                && tetraInsight$slot != null;
        tetraInsight$holoButton.setVisible(visible);
        emptyStateText.setY(visible ? 14 : 23);
    }

    @Unique
    private boolean tetraInsight$hasHolosphere() {
        return tetraInsight$holoAvailable;
    }

    @Unique
    private boolean tetraInsight$findHolosphere() {
        return tetraInsight$player != null
                && tetraInsight$workbench != null
                && tetraInsight$workbench.getLevel() != null
                && !ModularHolosphereItem.findHolosphere(
                        tetraInsight$player,
                        tetraInsight$workbench.getLevel(),
                        tetraInsight$workbench.getBlockPos()
                ).isEmpty();
    }

    @Unique
    private void tetraInsight$openSlotInHolo() {
        if (!tetraInsight$findHolosphere()
                || tetraInsight$item == null
                || tetraInsight$slot == null) {
            tetraInsight$holoAvailable = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen previousScreen = minecraft.screen;
        HoloGui holoGui = HoloGui.getInstance();
        minecraft.setScreen(holoGui);
        ((HoloSlotNavigationAccess) holoGui).tetraInsight$openSlot(
                tetraInsight$item,
                tetraInsight$itemStack.copy(),
                tetraInsight$slot,
                () -> ClientScheduler.schedule(
                        0,
                        () -> Minecraft.getInstance().setScreen(previousScreen)
                )
        );
    }
}
