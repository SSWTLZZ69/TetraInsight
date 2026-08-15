package io.github.createdelight.tetrainsight.integration.tetra.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EffectApplicabilityJsonParser {
    private static final String MANUAL_EVIDENCE_KEY =
            "tetra_insight.effect.evidence.manual_json";
    private static final Map<String, String> STACKING_KEYS = Map.ofEntries(
            Map.entry("item", "tetra_insight.effect.stacking.item"),
            Map.entry("current_item", "tetra_insight.effect.stacking.item"),
            Map.entry("inventory_max", "tetra_insight.effect.stacking.inventory_max"),
            Map.entry("armor_sum", "tetra_insight.effect.stacking.armor_sum"),
            Map.entry("armor_max", "tetra_insight.effect.stacking.armor_max"),
            Map.entry("single_piece", "tetra_insight.effect.stacking.single_piece"),
            Map.entry("held_max", "tetra_insight.effect.stacking.held_max"),
            Map.entry("held_sum", "tetra_insight.effect.stacking.held_sum"),
            Map.entry("curios_sum", "tetra_insight.effect.stacking.curios_sum"),
            Map.entry("curios_max", "tetra_insight.effect.stacking.curios_max"),
            Map.entry("unknown", "tetra_insight.effect.stacking.unknown"));

    private EffectApplicabilityJsonParser() {
    }

    public static EffectApplicabilityRuleSet parse(JsonObject root) {
        boolean replace = GsonHelper.getAsBoolean(root, "replace", false);
        JsonArray paths = GsonHelper.getAsJsonArray(root, "paths");
        if (paths.isEmpty()) {
            throw new JsonParseException("paths must contain at least one applicability path");
        }

        List<EffectApplicabilityDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < paths.size(); index++) {
            JsonElement element = paths.get(index);
            if (!element.isJsonObject()) {
                throw new JsonParseException("paths[" + index + "] must be an object");
            }
            definitions.add(parsePath(element.getAsJsonObject(), index));
        }
        return new EffectApplicabilityRuleSet(replace, definitions.stream().distinct().toList());
    }

    private static EffectApplicabilityDefinition parsePath(JsonObject path, int index) {
        List<EffectScope> scopes = parseEnums(
                path, "scopes", EffectScope.class, "paths[" + index + "].scopes");
        List<EffectTrigger> triggers = parseEnums(
                path, "triggers", EffectTrigger.class, "paths[" + index + "].triggers");
        String stacking = resolveTranslationKey(
                GsonHelper.getAsString(path, "stacking"), STACKING_KEYS,
                "paths[" + index + "].stacking");
        return new EffectApplicabilityDefinition(
                scopes, triggers, stacking, MANUAL_EVIDENCE_KEY);
    }

    private static <T extends Enum<T>> List<T> parseEnums(
            JsonObject object, String key, Class<T> enumType, String location) {
        JsonArray values = GsonHelper.getAsJsonArray(object, key);
        LinkedHashSet<T> result = new LinkedHashSet<>();
        for (JsonElement value : values) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new JsonParseException(location + " entries must be strings");
            }
            String token = value.getAsString().toUpperCase(Locale.ROOT);
            try {
                result.add(Enum.valueOf(enumType, token));
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException(
                        location + " contains unsupported value '" + value.getAsString() + "'",
                        exception);
            }
        }
        if (result.isEmpty()) {
            throw new JsonParseException(location + " must contain at least one value");
        }
        return List.copyOf(result);
    }

    private static String resolveTranslationKey(
            String value, Map<String, String> aliases, String location) {
        String normalized = value.toLowerCase(Locale.ROOT);
        String mapped = aliases.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        if (value.contains(".")) {
            return value;
        }
        throw new JsonParseException(
                location + " contains unsupported value '" + value
                        + "'; use a built-in alias or a full translation key");
    }
}
