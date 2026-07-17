package io.github.createdelight.tetrainsight.integration.tetra;

import com.google.common.collect.Multimap;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialInputChannel;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialOutputKind;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationInsight;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.module.data.EffectData;
import se.mickelus.tetra.module.data.MaterialMultiplier;
import se.mickelus.tetra.module.data.ToolData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MaterialTranslationGenerator {
    private MaterialTranslationGenerator() {
    }

    public static MaterialTranslationInsight fromExtract(String ownerKey, MaterialMultiplier extract) {
        return fromMultiplier(ownerKey, extract, TranslationProvenance.GENERATED_FROM_EXTRACT, false);
    }

    public static MaterialTranslationInsight fromAuthorTranslation(String ownerKey, MaterialMultiplier translation) {
        return fromMultiplier(ownerKey, translation, TranslationProvenance.AUTHOR, true);
    }

    private static MaterialTranslationInsight fromMultiplier(String ownerKey, MaterialMultiplier multiplier,
            TranslationProvenance provenance, boolean authorDisplayDefinition) {
        List<MaterialTranslationEntry> entries = new ArrayList<>();

        appendAttributes(entries, MaterialInputChannel.PRIMARY, multiplier.primaryAttributes, authorDisplayDefinition);
        appendAttributes(entries, MaterialInputChannel.SECONDARY, multiplier.secondaryAttributes, authorDisplayDefinition);
        appendAttributes(entries, MaterialInputChannel.TERTIARY, multiplier.tertiaryAttributes, authorDisplayDefinition);

        appendEffects(entries, MaterialInputChannel.PRIMARY, multiplier.primaryEffects, authorDisplayDefinition);
        appendEffects(entries, MaterialInputChannel.SECONDARY, multiplier.secondaryEffects, authorDisplayDefinition);
        appendEffects(entries, MaterialInputChannel.TERTIARY, multiplier.tertiaryEffects, authorDisplayDefinition);

        appendScalar(entries, MaterialInputChannel.DURABILITY, MaterialOutputKind.DURABILITY,
                "tetra:durability", multiplier.durability, authorDisplayDefinition);
        appendScalar(entries, MaterialInputChannel.DURABILITY, MaterialOutputKind.DURABILITY_MULTIPLIER,
                "tetra:durability_multiplier", multiplier.durabilityMultiplier, authorDisplayDefinition);
        appendScalar(entries, MaterialInputChannel.INTEGRITY, MaterialOutputKind.INTEGRITY,
                "tetra:integrity", multiplier.integrity, authorDisplayDefinition);
        appendScalar(entries, MaterialInputChannel.MAGIC_CAPACITY, MaterialOutputKind.MAGIC_CAPACITY,
                "tetra:magic_capacity", multiplier.magicCapacity, authorDisplayDefinition);

        appendTools(entries, multiplier.tools, authorDisplayDefinition);

        return new MaterialTranslationInsight(ownerKey, provenance, entries);
    }

    private static void appendAttributes(List<MaterialTranslationEntry> entries, MaterialInputChannel input,
            Multimap<Attribute, AttributeModifier> attributes, boolean authorDisplayDefinition) {
        if (attributes == null) {
            return;
        }

        for (Map.Entry<Attribute, AttributeModifier> entry : attributes.entries()) {
            AttributeModifier modifier = entry.getValue();
            ResourceLocation identifier = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            append(entries, input, MaterialOutputKind.ATTRIBUTE,
                    identifier != null ? identifier.toString() : entry.getKey().getDescriptionId(),
                    modifier.getOperation().name(), modifier.getAmount(), authorDisplayDefinition);
        }
    }

    private static void appendEffects(List<MaterialTranslationEntry> entries, MaterialInputChannel input,
            EffectData effects, boolean authorDisplayDefinition) {
        if (effects == null) {
            return;
        }

        for (Map.Entry<ItemEffect, Float> entry : effects.levelMap.entrySet()) {
            append(entries, input, MaterialOutputKind.EFFECT_LEVEL, entry.getKey().getKey(), "LEVEL",
                    entry.getValue(), authorDisplayDefinition);
        }
        for (Map.Entry<ItemEffect, Float> entry : effects.efficiencyMap.entrySet()) {
            append(entries, input, MaterialOutputKind.EFFECT_EFFICIENCY, entry.getKey().getKey(), "EFFICIENCY",
                    entry.getValue(), authorDisplayDefinition);
        }
    }

    private static void appendTools(List<MaterialTranslationEntry> entries, ToolData tools,
            boolean authorDisplayDefinition) {
        if (tools == null) {
            return;
        }

        for (Map.Entry<ToolAction, Float> entry : tools.levelMap.entrySet()) {
            append(entries, MaterialInputChannel.TOOL, MaterialOutputKind.TOOL_LEVEL,
                    entry.getKey().name(), "LEVEL", entry.getValue(), authorDisplayDefinition);
        }
        for (Map.Entry<ToolAction, Float> entry : tools.efficiencyMap.entrySet()) {
            append(entries, MaterialInputChannel.TOOL, MaterialOutputKind.TOOL_EFFICIENCY,
                    entry.getKey().name(), "EFFICIENCY", entry.getValue(), authorDisplayDefinition);
        }
    }

    private static void appendScalar(List<MaterialTranslationEntry> entries, MaterialInputChannel input,
            MaterialOutputKind outputKind, String outputId, Float coefficient, boolean authorDisplayDefinition) {
        if (coefficient != null) {
            append(entries, input, outputKind, outputId, "SCALAR", coefficient, authorDisplayDefinition);
        }
    }

    private static void append(List<MaterialTranslationEntry> entries, MaterialInputChannel input,
            MaterialOutputKind outputKind, String outputId, String operation, double value,
            boolean authorDisplayDefinition) {
        int displayLevel = authorDisplayDefinition ? (int) value : value > 0 ? 1 : -1;
        if (value == 0 || displayLevel == 0) {
            return;
        }

        double actualCoefficient = authorDisplayDefinition ? Double.NaN : value;
        entries.add(new MaterialTranslationEntry(input, outputKind, outputId, operation,
                actualCoefficient, displayLevel));
    }
}
