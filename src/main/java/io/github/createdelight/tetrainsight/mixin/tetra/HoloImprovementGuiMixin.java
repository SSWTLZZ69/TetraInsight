package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloMaterialImprovementVariantGui;
import io.github.createdelight.tetrainsight.client.HoloConsumableImprovementVariantGui;
import io.github.createdelight.tetrainsight.client.HoloFixedConsumableSummaryGui;
import io.github.createdelight.tetrainsight.client.HoloHoningOutcomeStack;
import io.github.createdelight.tetrainsight.client.HoloImprovementChainLevelGui;
import io.github.createdelight.tetrainsight.client.HoloImprovementGuiExtension;
import io.github.createdelight.tetrainsight.client.HoloImprovementRequirementSummaryGui;
import io.github.createdelight.tetrainsight.client.ImprovementChainEntry;
import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.integration.tetra.MaterialGlyphTintResolver;
import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementVariantGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.OutcomeStack;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.schematic.ConfigSchematic;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.SchematicRarity;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(value = HoloImprovementGui.class, remap = false)
public abstract class HoloImprovementGuiMixin implements HoloImprovementGuiExtension {
    @Unique
    private static final String tetraInsight$BOOK_ENCHANT = "book_enchant";

    @Unique
    private static final int tetraInsight$GROUP_GAP = 8;

    @Unique
    private static final int tetraInsight$LEVEL_GAP = 2;

    @Unique
    private static final int tetraInsight$NATIVE_VARIANT_WIDTH = 19;

    @Unique
    private static final int tetraInsight$MATERIAL_VARIANT_GAP = 2;

    @Unique
    private HoloFixedConsumableSummaryGui tetraInsight$fixedMaterial;

    @Unique
    private HoloImprovementRequirementSummaryGui tetraInsight$requirements;

    @Unique
    private ItemStack tetraInsight$itemStack = ItemStack.EMPTY;

    @Unique
    private String tetraInsight$chainKey;

    @Unique
    private List<ImprovementChainEntry> tetraInsight$chainEntries = List.of();

    @Shadow
    @Final
    private GuiHorizontalLayoutGroup header;

    @Shadow
    @Final
    private GuiTexture backdrop;

    @Shadow
    @Final
    private GuiString label;

    @Shadow
    @Final
    private GuiElement variants;

    @Shadow
    @Final
    private Consumer<OutcomeStack> onVariantSelect;

    @Shadow
    @Final
    private UpgradeSchematic schematic;

    @Shadow
    @Final
    private String slot;

    @Shadow
    @Final
    private Consumer<OutcomePreview> onVariantHover;

    @Shadow
    @Final
    private Consumer<OutcomePreview> onVariantBlur;

    @Shadow
    private boolean isActive;

    @Shadow
    private OutcomePreview preview;

    @Shadow
    protected abstract void updateTint(boolean focused);

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addFixedMaterialSummary(int x, int y,
            UpgradeSchematic schematic, ItemStack itemStack, String slot,
            Consumer<OutcomePreview> onVariantHover,
            Consumer<OutcomePreview> onVariantBlur,
            Consumer<OutcomeStack> onVariantSelect,
            CallbackInfo ci) {
        tetraInsight$itemStack = itemStack.copy();
        TetraDataProbe.findFixedConsumableSchematic(schematic.getKey())
                .ifPresent(snapshot -> {
                    tetraInsight$fixedMaterial = new HoloFixedConsumableSummaryGui(
                            0, -2, snapshot.outcomes());
                    header.addChild(tetraInsight$fixedMaterial);
                    tetraInsight$updateFixedMaterialSummary();
                    if (schematic.getKey().startsWith("toolbelt/")
                            || schematic.getKey().equals("bow/riser/adjustable_strength")) {
                        TetraInsight.LOGGER.debug(
                                "Attached fixed-consumable summary: schematic={} outcomes={} width={}",
                                schematic.getKey(), snapshot.outcomes().size(),
                                tetraInsight$fixedMaterial.getWidth());
                    }
                });

        tetraInsight$requirements = new HoloImprovementRequirementSummaryGui(
                0, -2, schematic, itemStack, slot,
                schematic.getPreviews(itemStack, slot));
        header.addChild(tetraInsight$requirements);
        tetraInsight$updateFixedMaterialSummary();
    }

    @Inject(method = "updateSelection", at = @At("HEAD"), remap = false)
    private void tetraInsight$rememberCurrentItemStack(
            ItemStack itemStack, List<OutcomeStack> selectedOutcomes, CallbackInfo ci) {
        tetraInsight$itemStack = itemStack.copy();
    }

    @Redirect(
            method = "updateVariants",
            at = @At(
                    value = "NEW",
                    target = "se/mickelus/tetra/items/modular/impl/holo/gui/craft/HoloImprovementVariantGui"),
            remap = false)
    private HoloImprovementVariantGui tetraInsight$useModuleVariantPresentation(
            int x, int y, String name, int labelStart, OutcomePreview preview,
            boolean nextInSeries, Consumer<OutcomePreview> onHover,
            Consumer<OutcomePreview> onBlur, Consumer<OutcomePreview> onSelect) {
        if (TetraDataProbe.findSchematic(schematic.getKey()).isPresent()) {
            return new HoloMaterialImprovementVariantGui(
                    x, y, name, labelStart, schematic.getKey(), slot,
                    preview, nextInSeries,
                    onHover, onBlur, onSelect);
        }
        if (preview.materials != null && preview.materials.length > 0) {
            return new HoloConsumableImprovementVariantGui(
                    x, y, name, labelStart, preview, nextInSeries,
                    onHover, onBlur, onSelect);
        }
        HoloImprovementVariantGui variant = new HoloImprovementVariantGui(
                x, y, name, labelStart, preview, nextInSeries,
                onHover, onBlur, onSelect);
        variant.setX(x);
        variant.setWidth(tetraInsight$NATIVE_VARIANT_WIDTH);
        return variant;
    }

    @ModifyVariable(
            method = "updateVariants",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false)
    private OutcomePreview[] tetraInsight$removeUnresolvedMaterialVariants(
            OutcomePreview[] previews) {
        int requiredMaterials = schematic.getNumMaterialSlots();
        if (requiredMaterials <= 0 || !(schematic instanceof ConfigSchematic)) {
            return previews;
        }

        boolean materialSchematic = TetraDataProbe.findSchematic(schematic.getKey()).isPresent();
        if (!materialSchematic
                && TetraDataProbe.findFixedConsumableSchematic(schematic.getKey()).isPresent()) {
            return previews;
        }
        OutcomePreview[] resolved = java.util.Arrays.stream(previews)
                .filter(preview -> materialSchematic
                        ? MaterialGlyphTintResolver.resolve(schematic.getKey(), preview).isPresent()
                        : false)
                .toArray(OutcomePreview[]::new);
        if (resolved.length != previews.length) {
            TetraInsight.LOGGER.debug(
                    "Skipped {} unresolved material improvement variants for schematic {}",
                    previews.length - resolved.length, schematic.getKey());
        }
        return resolved;
    }

    @Inject(method = "updateVariants", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$groupEnchantments(OutcomePreview[] previews,
            List<OutcomeStack> selectedOutcomes, CallbackInfo ci) {
        if (!tetraInsight$chainEntries.isEmpty()) {
            tetraInsight$renderImprovementChain(selectedOutcomes);
            ci.cancel();
            return;
        }
        if (!tetraInsight$BOOK_ENCHANT.equals(schematic.getKey()) || previews.length <= 1) {
            return;
        }

        variants.clearChildren();
        preview = null;
        isActive = selectedOutcomes.stream().anyMatch(outcome -> outcome.schematicEquals(schematic));

        Map<String, List<OutcomePreview>> groups = new LinkedHashMap<>();
        for (OutcomePreview outcome : previews) {
            groups.computeIfAbsent(outcome.variantKey, ignored -> new ArrayList<>()).add(outcome);
        }

        int cursor = 0;
        for (List<OutcomePreview> group : groups.values()) {
            OutcomePreview first = group.get(0);
            String enchantmentName = tetraInsight$getEnchantmentName(first);
            GuiString groupLabel = new GuiString(cursor, 2, enchantmentName);
            variants.addChild(groupLabel);
            cursor += Minecraft.getInstance().font.width(enchantmentName) + 4;

            for (int index = 0; index < group.size(); index++) {
                OutcomePreview outcome = group.get(index);
                HoloImprovementVariantGui levelButton = new HoloImprovementVariantGui(
                        cursor, 0, tetraInsight$formatLevel(outcome.level), 0, outcome,
                        index + 1 < group.size(), onVariantHover, onVariantBlur,
                        selected -> onVariantSelect.accept(new OutcomeStack(schematic, selected)));
                levelButton.setX(cursor);
                levelButton.setWidth(tetraInsight$NATIVE_VARIANT_WIDTH);
                if (isActive) {
                    boolean selected = selectedOutcomes.stream()
                            .anyMatch(stack -> stack.schematicEquals(schematic)
                                    && stack.previewEquals(outcome));
                    levelButton.setMuted(!selected);
                }
                variants.addChild(levelButton);
                cursor += tetraInsight$getLayoutWidth(levelButton)
                        + tetraInsight$LEVEL_GAP;
            }
            cursor += tetraInsight$GROUP_GAP;
        }

        backdrop.setColor(0xffffff);
        label.setColor(0xffffff);
        variants.setVisible(true);
        header.setY(0);
        tetraInsight$updateWidth(Math.max(0, cursor - tetraInsight$GROUP_GAP));
        updateTint(false);
        ci.cancel();
    }

    @Inject(method = "updateVariants", at = @At("RETURN"), remap = false)
    private void tetraInsight$refreshFixedMaterialSummary(OutcomePreview[] previews,
            List<OutcomeStack> selectedOutcomes, CallbackInfo ci) {
        tetraInsight$compactMaterialVariants();
        if (tetraInsight$fixedMaterial != null) {
            tetraInsight$updateFixedMaterialSummary();
        }
        if (tetraInsight$requirements != null) {
            tetraInsight$requirements.update(
                    tetraInsight$itemStack, previews);
            header.forceLayout();
            tetraInsight$updateFixedMaterialSummary();
        }
    }

    @Unique
    private void tetraInsight$compactMaterialVariants() {
        List<GuiElement> children = variants.getChildren();
        if (children.isEmpty()
                || children.stream().anyMatch(child ->
                        !(child instanceof HoloMaterialImprovementVariantGui))) {
            return;
        }

        int cursor = 0;
        for (GuiElement child : children) {
            child.setX(cursor);
            cursor += child.getWidth() + tetraInsight$MATERIAL_VARIANT_GAP;
        }
        variants.setWidth(Math.max(0, cursor - tetraInsight$MATERIAL_VARIANT_GAP));
    }

    @Unique
    private void tetraInsight$updateFixedMaterialSummary() {
        tetraInsight$updateWidth(variants.getWidth());
    }

    @Unique
    private void tetraInsight$updateWidth(int variantContentWidth) {
        header.forceLayout();

        GuiElement self = (GuiElement) (Object) this;
        int headerContentExtent = header.getChildren().stream()
                .mapToInt(child -> child.getX() + child.getWidth())
                .max()
                .orElse(0);
        int headerExtent = header.getX() + headerContentExtent;
        if (variantContentWidth > variants.getWidth()) {
            variants.setWidth(variantContentWidth);
        }
        int variantsExtent = variants.isVisible()
                ? variants.getX() + variants.getWidth()
                : 0;
        self.setWidth(Math.max(headerExtent, variantsExtent));
    }

    @Override
    @Unique
    public void tetraInsight$refreshLayoutWidth() {
        int variantContentExtent = variants.getChildren().stream()
                .mapToInt(child -> child.getX() + tetraInsight$getLayoutWidth(child))
                .max()
                .orElse(0);
        variants.setWidth(variantContentExtent);
        tetraInsight$updateWidth(variantContentExtent);
    }

    @Unique
    private static int tetraInsight$getLayoutWidth(GuiElement child) {
        if (child instanceof HoloImprovementVariantGui
                && !(child instanceof HoloMaterialImprovementVariantGui)) {
            return Math.max(tetraInsight$NATIVE_VARIANT_WIDTH, child.getWidth());
        }
        return child.getWidth();
    }

    @Override
    @Unique
    public void tetraInsight$setImprovementChain(String improvementKey,
            List<ImprovementChainEntry> entries, ItemStack itemStack) {
        tetraInsight$chainKey = improvementKey;
        tetraInsight$chainEntries = List.copyOf(entries);
    }

    @Unique
    private void tetraInsight$renderImprovementChain(List<OutcomeStack> selectedOutcomes) {
        variants.clearChildren();
        preview = null;
        isActive = selectedOutcomes.stream().anyMatch(selected ->
                tetraInsight$chainEntries.stream()
                        .anyMatch(entry -> selected.schematicEquals(entry.schematic())));

        label.setString(IModularItem.getImprovementName(tetraInsight$chainKey, 0));
        header.forceLayout();

        int cursor = 0;
        for (int index = 0; index < tetraInsight$chainEntries.size(); index++) {
            ImprovementChainEntry entry = tetraInsight$chainEntries.get(index);
            boolean selected = selectedOutcomes.stream().anyMatch(stack ->
                    stack.schematicEquals(entry.schematic())
                            && stack.previewEquals(entry.preview()));
            int tint = entry.schematic().isHoning()
                    ? SchematicRarity.hone.tint
                    : SchematicRarity.basic.tint;
            HoloImprovementChainLevelGui levelButton = new HoloImprovementChainLevelGui(
                    cursor, 0, tetraInsight$formatLevel(entry.preview().level),
                    entry.preview(), index + 1 < tetraInsight$chainEntries.size(),
                    onVariantHover, onVariantBlur,
                    outcome -> onVariantSelect.accept(
                            new HoloHoningOutcomeStack(entry.schematic(), outcome)),
                    tint, entry.available(), tetraInsight$buildChainTooltip(entry, tint));
            levelButton.tetraInsight$setSelectionState(isActive, selected);
            variants.addChild(levelButton);
            cursor += tetraInsight$getLayoutWidth(levelButton)
                    + tetraInsight$LEVEL_GAP;
        }

        backdrop.setColor(0xffffff);
        label.setColor(0xffffff);
        variants.setVisible(true);
        header.setY(0);
        tetraInsight$updateWidth(Math.max(0, cursor - tetraInsight$LEVEL_GAP));
        updateTint(false);
    }

    @Unique
    private static List<Component> tetraInsight$buildChainTooltip(
            ImprovementChainEntry entry, int tint) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(entry.schematic().getName())
                .withStyle(style -> style.withColor(tint)));
        if (!entry.available()) {
            tooltip.add(Component.translatable("tetra_insight.holo.improvement.locked")
                    .withStyle(ChatFormatting.GRAY));
            List<Component> requirements = entry.schematic().getRequirementDescription();
            if (requirements != null) {
                requirements.forEach(requirement ->
                        tooltip.add(requirement.copy().withStyle(ChatFormatting.GRAY)));
            }
        }
        return tooltip;
    }

    @Unique
    private static String tetraInsight$getEnchantmentName(OutcomePreview preview) {
        ResourceLocation id = ResourceLocation.tryParse(preview.variantKey);
        Enchantment enchantment = id == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (enchantment != null) {
            return Component.translatable(enchantment.getDescriptionId()).getString();
        }

        String level = tetraInsight$formatLevel(preview.level);
        String suffix = " " + level;
        return preview.variantName.endsWith(suffix)
                ? preview.variantName.substring(0, preview.variantName.length() - suffix.length())
                : preview.variantName;
    }

    @Unique
    private static String tetraInsight$formatLevel(int level) {
        if (level >= 1 && level <= 10) {
            return Component.translatable("enchantment.level." + level).getString();
        }
        return Integer.toString(level);
    }
}
