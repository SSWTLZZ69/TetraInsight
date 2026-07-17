package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialInputChannel;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialOutputKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.module.schematic.SchematicType;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

public final class MaterialTranslationTooltipFormatter {
    private MaterialTranslationTooltipFormatter() {
    }

    public static List<Component> format(MaterialSchematicSnapshot schematic, SchematicType type, boolean detailed) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(type == SchematicType.improvement
                ? "tetra.holo.craft.translation_improvement"
                : "tetra.holo.craft.translation_module"));

        if (schematic.displayTranslation().provenance() == TranslationProvenance.NO_MATERIAL_SCALING) {
            tooltip.add(Component.translatable("tetra_insight.holo.translation.no_scaling")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return List.copyOf(tooltip);
        }

        tooltip.add(Component.translatable("tetra_insight.holo.translation.generated")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        appendEntries(tooltip, schematic, List.of(
                MaterialInputChannel.DURABILITY,
                MaterialInputChannel.INTEGRITY,
                MaterialInputChannel.MAGIC_CAPACITY,
                MaterialInputChannel.TOOL));
        appendGroup(tooltip, schematic, MaterialInputChannel.PRIMARY,
                "tetra.holo.craft.materials.stat.primary");
        appendGroup(tooltip, schematic, MaterialInputChannel.SECONDARY,
                "tetra.holo.craft.materials.stat.secondary");
        appendGroup(tooltip, schematic, MaterialInputChannel.TERTIARY,
                "tetra.holo.craft.materials.stat.tertiary");
        tooltip.add(Component.translatable("tetra_insight.holo.translation.level_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (detailed) {
            appendFormulaEntries(tooltip, schematic);
        }
        return List.copyOf(tooltip);
    }

    private static void appendFormulaEntries(List<Component> tooltip, MaterialSchematicSnapshot schematic) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tetra_insight.holo.translation.formula_heading")
                .withStyle(ChatFormatting.WHITE));
        schematic.displayTranslation().entries().stream()
                .map(MaterialTranslationTooltipFormatter::formatFormulaEntry)
                .forEach(tooltip::add);
    }

    private static Component formatFormulaEntry(MaterialTranslationEntry entry) {
        MutableComponent line = Component.literal("  ").append(entryName(entry).withStyle(ChatFormatting.GRAY));
        if (!Double.isFinite(entry.actualCoefficient())) {
            return line.append(Component.literal(": "))
                    .append(Component.translatable("tetra_insight.holo.translation.formula_unavailable")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }

        String coefficient = BigDecimal.valueOf(entry.actualCoefficient())
                .stripTrailingZeros()
                .toPlainString();
        return line.append(Component.literal(": "))
                .append(materialInputName(entry).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" × " + coefficient).withStyle(ChatFormatting.YELLOW));
    }

    private static MutableComponent materialInputName(MaterialTranslationEntry entry) {
        return switch (entry.input()) {
            case PRIMARY -> Component.translatable("tetra.holo.craft.materials.stat.primary");
            case SECONDARY -> Component.translatable("tetra.holo.craft.materials.stat.secondary");
            case TERTIARY -> Component.translatable("tetra.holo.craft.materials.stat.tertiary");
            case DURABILITY -> Component.translatable("tetra.holo.craft.materials.stat.durability");
            case INTEGRITY -> Component.translatable("tetra.holo.craft.materials.stat.integrity");
            case MAGIC_CAPACITY -> Component.translatable("tetra.holo.craft.materials.stat.magic_capacity");
            case TOOL -> Component.translatable(entry.outputKind() == MaterialOutputKind.TOOL_LEVEL
                    ? "tetra.holo.craft.materials.stat.tool_level"
                    : "tetra.holo.craft.materials.stat.tool_efficiency");
        };
    }

    private static void appendGroup(List<Component> tooltip, MaterialSchematicSnapshot schematic,
            MaterialInputChannel input, String headingKey) {
        List<MaterialTranslationEntry> entries = schematic.displayTranslation().entries().stream()
                .filter(entry -> entry.input() == input)
                .toList();
        if (entries.isEmpty()) {
            return;
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(headingKey).withStyle(ChatFormatting.WHITE));
        entries.stream().map(MaterialTranslationTooltipFormatter::formatEntry).forEach(tooltip::add);
    }

    private static void appendEntries(List<Component> tooltip, MaterialSchematicSnapshot schematic,
            List<MaterialInputChannel> inputs) {
        List<MaterialTranslationEntry> entries = schematic.displayTranslation().entries().stream()
                .filter(entry -> inputs.contains(entry.input()))
                .toList();
        if (entries.isEmpty()) {
            return;
        }

        tooltip.add(Component.empty());
        entries.stream().map(MaterialTranslationTooltipFormatter::formatEntry).forEach(tooltip::add);
    }

    private static Component formatEntry(MaterialTranslationEntry entry) {
        MutableComponent line = entryName(entry).withStyle(ChatFormatting.GRAY);
        int level = entry.generatedDisplayLevel();
        line.append(Component.literal(level >= 0 ? " +" : " -")
                .withStyle(level >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        line.append(Component.literal(toRomanNumeral(Math.abs(level)))
                .withStyle(level >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        return line;
    }

    public static String toRomanNumeral(int value) {
        if (value <= 0) {
            return "0";
        }

        int remaining = value;
        StringBuilder result = new StringBuilder();
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] numerals = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                result.append(numerals[i]);
                remaining -= values[i];
            }
        }
        return result.toString();
    }

    public static MutableComponent entryName(MaterialTranslationEntry entry) {
        return switch (entry.outputKind()) {
            case ATTRIBUTE -> attributeName(entry.outputId(), entry.operation());
            case EFFECT_LEVEL -> translatedOrLiteral("tetra.stats." + entry.outputId() + ".level",
                    "tetra.stats." + entry.outputId(), entry.outputId());
            case EFFECT_EFFICIENCY -> translatedOrLiteral("tetra.stats." + entry.outputId() + ".efficiency",
                    null, entry.outputId() + " " + I18n.get("tetra.stats.efficiency_suffix"));
            case DURABILITY -> Component.translatable("tetra.stats.durability");
            case DURABILITY_MULTIPLIER -> Component.translatable("tetra.stats.durability")
                    .append(Component.literal(" "))
                    .append(Component.translatable("tetra.attribute.multiplier"));
            case INTEGRITY -> Component.translatable("tetra.stats.integrity");
            case MAGIC_CAPACITY -> Component.translatable("tetra.stats.magicCapacity");
            case TOOL_LEVEL -> translatedOrLiteral("tetra.stats." + entry.outputId(), null, entry.outputId())
                    .append(Component.literal(" "))
                    .append(Component.translatable("tetra.stats.tier_suffix"));
            case TOOL_EFFICIENCY -> translatedOrLiteral("tetra.stats." + entry.outputId(), null, entry.outputId())
                    .append(Component.literal(" "))
                    .append(Component.translatable("tetra.stats.efficiency_suffix"));
        };
    }

    private static MutableComponent attributeName(String outputId, String operation) {
        ResourceLocation identifier = ResourceLocation.tryParse(outputId);
        Attribute attribute = identifier != null ? ForgeRegistries.ATTRIBUTES.getValue(identifier) : null;
        MutableComponent name = attribute != null
                ? Component.translatable(attribute.getDescriptionId())
                : Component.literal(outputId);
        if (!"ADDITION".equals(operation)) {
            name.append(Component.literal(" "))
                    .append(Component.translatable("tetra.attribute.multiplier"));
        }
        return name;
    }

    private static MutableComponent translatedOrLiteral(String preferredKey, String fallbackKey, String literal) {
        if (preferredKey != null && I18n.exists(preferredKey)) {
            return Component.translatable(preferredKey);
        }
        if (fallbackKey != null && I18n.exists(fallbackKey)) {
            return Component.translatable(fallbackKey);
        }
        return Component.literal(literal);
    }
}
