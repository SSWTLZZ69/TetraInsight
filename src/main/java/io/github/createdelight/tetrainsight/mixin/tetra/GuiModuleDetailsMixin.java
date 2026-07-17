package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.WorkbenchModuleMetadataButtonGui;
import io.github.createdelight.tetrainsight.client.WorkbenchModuleMetadataPanelGui;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.aspect.ItemAspect;
import se.mickelus.tetra.blocks.workbench.gui.GuiModuleDetails;
import se.mickelus.tetra.blocks.workbench.gui.RepairInfoGui;
import se.mickelus.tetra.gui.AspectIconGui;
import se.mickelus.tetra.gui.GuiSynergyIndicator;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.data.AspectData;

@Mixin(value = GuiModuleDetails.class, remap = false)
public abstract class GuiModuleDetailsMixin {
    @Unique
    private static final int tetraInsight$metadataX = 128;

    @Unique
    private static final int tetraInsight$metadataWidth = 44;

    @Shadow
    @Final
    private GuiElement wrapper;

    @Shadow
    @Final
    private GuiSynergyIndicator synergyIndicator;

    @Shadow
    @Final
    private AspectIconGui aspectIcon;

    @Shadow
    @Final
    private RepairInfoGui repairInfo;

    @Unique
    private WorkbenchModuleMetadataButtonGui tetraInsight$synergyButton;

    @Unique
    private WorkbenchModuleMetadataButtonGui tetraInsight$aspectButton;

    @Unique
    private WorkbenchModuleMetadataPanelGui tetraInsight$metadataPanel;

    @Unique
    private List<Component> tetraInsight$synergyLines = List.of();

    @Unique
    private List<Component> tetraInsight$aspectLines = List.of();

    @Unique
    private String tetraInsight$contextKey = "";

    @Unique
    private int tetraInsight$activePanel;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addDiscoverableMetadata(CallbackInfo ci) {
        synergyIndicator.setX(tetraInsight$metadataX);
        synergyIndicator.setY(6);
        aspectIcon.setX(tetraInsight$metadataX);
        aspectIcon.setY(20);

        tetraInsight$metadataPanel = new WorkbenchModuleMetadataPanelGui(
                4,
                2,
                () -> {
                    tetraInsight$activePanel = 0;
                    tetraInsight$synergyButton.setActive(false);
                    tetraInsight$aspectButton.setActive(false);
                }
        );
        wrapper.addChild(tetraInsight$metadataPanel);

        tetraInsight$synergyButton = new WorkbenchModuleMetadataButtonGui(
                tetraInsight$metadataX,
                6,
                tetraInsight$metadataWidth,
                this::tetraInsight$toggleSynergyPanel,
                color -> ((GuiSynergyIndicatorAccessor) synergyIndicator)
                        .tetraInsight$getIndicator().setColor(color)
        );
        wrapper.addChild(tetraInsight$synergyButton);

        tetraInsight$aspectButton = new WorkbenchModuleMetadataButtonGui(
                tetraInsight$metadataX,
                20,
                tetraInsight$metadataWidth,
                this::tetraInsight$toggleAspectPanel,
                this::tetraInsight$setAspectIconColor
        );
        wrapper.addChild(tetraInsight$aspectButton);
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$updateDiscoverableMetadata(
            ItemModule module,
            ItemStack stack,
            CallbackInfo ci
    ) {
        boolean visible = module != null;
        tetraInsight$synergyButton.setVisible(visible);
        tetraInsight$aspectButton.setVisible(visible);
        if (!visible) {
            tetraInsight$contextKey = "";
            tetraInsight$metadataPanel.close();
            return;
        }

        String variantKey = module.getVariantData(stack).key;
        String nextContextKey = module.getKey() + "|" + variantKey;
        if (!nextContextKey.equals(tetraInsight$contextKey)) {
            tetraInsight$contextKey = nextContextKey;
            tetraInsight$metadataPanel.close();
        }

        GuiTexture indicator = ((GuiSynergyIndicatorAccessor) synergyIndicator)
                .tetraInsight$getIndicator();
        int synergyTextureX = ((GuiTextureAccessor) indicator).tetraInsight$getTextureX();
        int synergyColor = switch (synergyTextureX) {
            case 176 -> 0xffffff;
            case 186 -> 0xaaaaaa;
            default -> 0x404040;
        };
        tetraInsight$synergyLines = List.copyOf(
                ((GuiSynergyIndicatorAccessor) synergyIndicator).tetraInsight$getTooltip()
        );
        tetraInsight$synergyButton.update(
                I18n.get("tetra_insight.workbench.module_metadata.synergy"),
                synergyColor,
                withOpenHint(tetraInsight$synergyLines)
        );

        AspectData aspects = module.getAspects(stack);
        int aspectCount = aspects == null ? 0 : aspects.getValues().size();
        tetraInsight$aspectLines = buildAspectLines(aspects);
        tetraInsight$aspectButton.update(
                I18n.get("tetra_insight.workbench.module_metadata.aspects"),
                aspectCount > 0 ? 0xffffff : 0x404040,
                withOpenHint(tetraInsight$aspectLines)
        );
    }

    @Unique
    private void tetraInsight$toggleSynergyPanel() {
        if (tetraInsight$metadataPanel.isVisible()
                && tetraInsight$activePanel == 1) {
            tetraInsight$metadataPanel.close();
            return;
        }
        tetraInsight$activePanel = 1;
        tetraInsight$synergyButton.setActive(true);
        tetraInsight$aspectButton.setActive(false);
        tetraInsight$metadataPanel.open(
                I18n.get("tetra_insight.workbench.module_metadata.synergy_title"),
                tetraInsight$synergyLines
        );
    }

    @Unique
    private void tetraInsight$toggleAspectPanel() {
        if (tetraInsight$metadataPanel.isVisible()
                && tetraInsight$activePanel == 2) {
            tetraInsight$metadataPanel.close();
            return;
        }
        tetraInsight$activePanel = 2;
        tetraInsight$synergyButton.setActive(false);
        tetraInsight$aspectButton.setActive(true);
        tetraInsight$metadataPanel.open(
                I18n.get("tetra_insight.workbench.module_metadata.aspects_title"),
                tetraInsight$aspectLines
        );
    }

    @Unique
    private void tetraInsight$setAspectIconColor(int color) {
        aspectIcon.getChildren(GuiTexture.class).stream()
                .findFirst()
                .ifPresent(texture -> texture.setColor(color));
    }

    @Unique
    private static List<Component> buildAspectLines(AspectData aspects) {
        if (aspects == null || aspects.getValues().isEmpty()) {
            return List.of(Component.translatable("tetra.modular.aspects.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        List<Component> result = new ArrayList<>();
        aspects.getLevelMap().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getKey()))
                .forEach(entry -> {
                    ItemAspect aspect = entry.getKey();
                    result.add(Component.empty()
                            .append(ItemAspect.getAspectLabel(aspect).withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" " + entry.getValue())
                                    .withStyle(ChatFormatting.WHITE)));
                    result.add(ItemAspect.getAspectDescription(aspect.getKey())
                            .withStyle(ChatFormatting.GRAY));
                    result.add(Component.empty());
                });
        return List.copyOf(result);
    }

    @Unique
    private static List<Component> withOpenHint(List<Component> original) {
        List<Component> result = new ArrayList<>(original);
        if (!result.isEmpty()) {
            result.add(Component.empty());
        }
        result.add(Component.translatable("tetra_insight.workbench.module_metadata.click")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return List.copyOf(result);
    }
}
