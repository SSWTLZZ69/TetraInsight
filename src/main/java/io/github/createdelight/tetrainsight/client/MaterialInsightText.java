package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.MaterialInsightIndex;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialAxis;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialAxisBand;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import java.math.BigDecimal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class MaterialInsightText {
    private MaterialInsightText() {
    }

    public static Component materialName(MaterialProfileSnapshot profile) {
        String key = "tetra.material." + profile.materialKey();
        return I18n.exists(key)
                ? Component.translatable(key)
                : Component.literal(humanize(profile.materialKey()));
    }

    public static Component categoryName(MaterialProfileSnapshot profile) {
        String key = "tetra.variant_category." + profile.category() + ".label";
        return I18n.exists(key)
                ? Component.translatable(key)
                : Component.literal(humanize(profile.category()));
    }

    public static Component tendency(MaterialProfileSnapshot profile) {
        MutableComponent result = Component.empty();
        appendTendency(result, profile, MaterialAxis.PRIMARY,
                "tetra.holo.craft.materials.stat.primary");
        result.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY));
        appendTendency(result, profile, MaterialAxis.SECONDARY,
                "tetra.holo.craft.materials.stat.secondary");
        result.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY));
        appendTendency(result, profile, MaterialAxis.TERTIARY,
                "tetra.holo.craft.materials.stat.tertiary");
        return result;
    }

    public static String number(float value) {
        if (!Float.isFinite(value)) {
            return "-";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static String number(Float value) {
        return value == null ? "-" : number(value.floatValue());
    }

    private static void appendTendency(
            MutableComponent target,
            MaterialProfileSnapshot profile,
            MaterialAxis axis,
            String axisKey
    ) {
        MaterialAxisBand band = MaterialInsightIndex.band(profile, axis);
        String bandKey = switch (band) {
            case LOW -> "tetra_insight.material.tendency.low";
            case MEDIUM -> "tetra_insight.material.tendency.medium";
            case HIGH -> "tetra_insight.material.tendency.high";
            case UNAVAILABLE -> "tetra_insight.material.tendency.unavailable";
        };
        target.append(Component.translatable(bandKey, Component.translatable(axisKey))
                .withStyle(switch (band) {
                    case LOW, UNAVAILABLE -> ChatFormatting.DARK_GRAY;
                    case MEDIUM -> ChatFormatting.GRAY;
                    case HIGH -> ChatFormatting.WHITE;
                }));
    }

    private static String humanize(String value) {
        String normalized = value;
        int separator = normalized.lastIndexOf('/');
        if (separator >= 0 && separator + 1 < normalized.length()) {
            normalized = normalized.substring(separator + 1);
        }
        return normalized.replace('_', ' ');
    }
}
