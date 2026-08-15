package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilitySnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilityPathSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilityState;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class EffectApplicabilityTooltipFormatter {
    private EffectApplicabilityTooltipFormatter() {
    }

    public static List<Component> append(List<Component> original,
            EffectApplicabilitySnapshot snapshot, boolean detailed) {
        List<Component> tooltip = new ArrayList<>(original);
        tooltip.add(Component.empty());
        tooltip.add(labeled("tetra_insight.effect.scope", join(
                snapshot.scopes(), scope -> Component.translatable(scope.translationKey()))));
        tooltip.add(labeled("tetra_insight.effect.trigger", join(
                snapshot.triggers(), trigger -> Component.translatable(trigger.translationKey()))));
        tooltip.add(labeled("tetra_insight.effect.preview", previewState(snapshot.previewState())));

        if (detailed) {
            tooltip.add(Component.empty());
            for (int index = 0; index < snapshot.paths().size(); index++) {
                EffectApplicabilityPathSnapshot path = snapshot.paths().get(index);
                tooltip.add(pathHeading(index + 1, path));
                tooltip.add(indentedLabeled("tetra_insight.effect.stacking",
                        Component.translatable(path.stackingTranslationKey())
                                .withStyle(ChatFormatting.GRAY)));
                tooltip.add(indentedLabeled("tetra_insight.effect.evidence",
                        Component.translatable(path.evidenceTranslationKey())
                                .withStyle(ChatFormatting.DARK_GRAY)));
            }
            tooltip.add(labeled("tetra_insight.effect.id",
                    Component.literal(snapshot.effectKey()).withStyle(ChatFormatting.DARK_GRAY)));
        }
        return List.copyOf(tooltip);
    }

    private static MutableComponent labeled(String key, Component value) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(": "))
                .append(value.copy());
    }

    private static MutableComponent indentedLabeled(String key, Component value) {
        return Component.literal("  ")
                .append(labeled(key, value));
    }

    private static MutableComponent pathHeading(int index, EffectApplicabilityPathSnapshot path) {
        return Component.translatable(
                "tetra_insight.effect.path",
                index,
                join(path.scopes(), scope -> Component.translatable(scope.translationKey())),
                join(path.triggers(), trigger -> Component.translatable(trigger.translationKey())),
                previewState(path.previewState()));
    }

    private static Component previewState(EffectApplicabilityState state) {
        return switch (state) {
            case ACTIVE -> Component.translatable("tetra_insight.effect.preview.active")
                    .withStyle(ChatFormatting.GREEN);
            case PROVIDED_NOT_TRIGGERED -> Component.translatable(
                            "tetra_insight.effect.preview.provided_not_triggered")
                    .withStyle(ChatFormatting.YELLOW);
            case UNKNOWN -> Component.translatable("tetra_insight.effect.preview.unknown")
                    .withStyle(ChatFormatting.GRAY);
        };
    }

    private static <T> Component join(List<T> values, Function<T, Component> formatter) {
        MutableComponent result = Component.empty();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                result.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY));
            }
            result.append(formatter.apply(values.get(index)).copy().withStyle(ChatFormatting.GRAY));
        }
        return result;
    }
}
