package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.MaterialInsightIndex;
import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public final class MaterialTooltipHandler {
    private static final int MAX_AMBIGUOUS_ENTRIES = 4;

    private MaterialTooltipHandler() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        List<MaterialProfileSnapshot> profiles = MaterialInsightIndex.findProfiles(event.getItemStack());
        MaterialDossierShortcut.setHoveredProfiles(profiles, event.getItemStack());
        if (profiles.isEmpty()) {
            return;
        }
        int specialUsages = TetraDataProbe.findSpecialMaterialSchematicKeys(
                event.getItemStack()).size();

        List<Component> insight = profiles.size() == 1
                ? singleProfile(profiles.get(0), specialUsages)
                : multipleProfiles(profiles, specialUsages);
        int insertion = Math.min(1, event.getToolTip().size());
        event.getToolTip().addAll(insertion, insight);
    }

    private static List<Component> singleProfile(
            MaterialProfileSnapshot profile,
            int specialUsages
    ) {
        List<Component> result = new ArrayList<>();
        int usages = MaterialInsightIndex.usageCount(profile.materialKey());
        addSpecialUsageHint(result, specialUsages);
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.identity",
                        MaterialInsightText.categoryName(profile),
                        usages)
                .withStyle(ChatFormatting.WHITE));
        result.add(MaterialInsightText.tendency(profile));

        if (Screen.hasShiftDown()) {
            result.add(Component.translatable(
                            "tetra_insight.material.tooltip.axes",
                            MaterialInsightText.number(profile.primary()),
                            MaterialInsightText.number(profile.secondary()),
                            MaterialInsightText.number(profile.tertiary()))
                    .withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable(
                            "tetra_insight.material.tooltip.capacity",
                            MaterialInsightText.number(profile.durability()),
                            MaterialInsightText.number(profile.integrityGain()),
                            MaterialInsightText.number(profile.integrityCost()),
                            profile.magicCapacity())
                    .withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable(
                            "tetra_insight.material.tooltip.intrinsic",
                            profile.attributeCount(),
                            profile.effectCount(),
                            profile.aspectCount(),
                            profile.featureCount(),
                            profile.improvementCount())
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            result.add(Component.translatable("tetra_insight.material.tooltip.shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.holosphere",
                        MaterialDossierShortcut.keyName())
                .withStyle(ChatFormatting.GRAY));
        return List.copyOf(result);
    }

    private static List<Component> multipleProfiles(
            List<MaterialProfileSnapshot> profiles,
            int specialUsages
    ) {
        List<Component> result = new ArrayList<>();
        int usageCount = profiles.stream()
                .mapToInt(profile -> MaterialInsightIndex.usageCount(profile.materialKey()))
                .sum();
        addSpecialUsageHint(result, specialUsages);
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.multiple",
                        profiles.size(),
                        usageCount)
                .withStyle(ChatFormatting.WHITE));
        if (Screen.hasShiftDown()) {
            profiles.stream().limit(MAX_AMBIGUOUS_ENTRIES).forEach(profile ->
                    result.add(Component.literal("  ")
                            .append(MaterialInsightText.materialName(profile))
                            .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                            .append(MaterialInsightText.categoryName(profile).copy()
                                    .withStyle(ChatFormatting.GRAY))));
            if (profiles.size() > MAX_AMBIGUOUS_ENTRIES) {
                result.add(Component.translatable(
                                "tetra_insight.material.tooltip.more",
                                profiles.size() - MAX_AMBIGUOUS_ENTRIES)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            result.add(Component.translatable("tetra_insight.material.tooltip.shift_multiple")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.holosphere",
                        MaterialDossierShortcut.keyName())
                .withStyle(ChatFormatting.GRAY));
        return List.copyOf(result);
    }

    private static void addSpecialUsageHint(List<Component> result, int specialUsages) {
        if (specialUsages > 0) {
            result.add(Component.translatable(
                            "tetra_insight.material.tooltip.special_usages",
                            specialUsages)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
