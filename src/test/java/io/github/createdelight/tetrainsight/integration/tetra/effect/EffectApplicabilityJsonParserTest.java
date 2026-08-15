package io.github.createdelight.tetrainsight.integration.tetra.effect;

import com.google.gson.JsonParser;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectApplicabilityJsonParserTest {
    @Test
    void parsesManualHeldEffectPath() {
        EffectApplicabilityRuleSet ruleSet = parse("""
                {
                  "replace": false,
                  "paths": [
                    {
                      "scopes": ["main_hand", "off_hand"],
                      "triggers": ["kill_entity"],
                      "stacking": "held_max"
                    }
                  ]
                }
                """);

        assertFalse(ruleSet.replace());
        assertEquals(1, ruleSet.definitions().size());
        EffectApplicabilityDefinition definition = ruleSet.definitions().get(0);
        assertEquals(List.of(EffectScope.MAIN_HAND, EffectScope.OFF_HAND), definition.scopes());
        assertEquals(List.of(EffectTrigger.KILL_ENTITY), definition.triggers());
        assertEquals("tetra_insight.effect.stacking.held_max",
                definition.stackingTranslationKey());
        assertEquals("tetra_insight.effect.evidence.manual_json",
                definition.evidenceTranslationKey());
    }

    @Test
    void supportsReplaceAndCustomStackingTranslationKeys() {
        EffectApplicabilityRuleSet ruleSet = parse("""
                {
                  "replace": true,
                  "paths": [
                    {
                      "scopes": ["tool"],
                      "triggers": ["ability"],
                      "stacking": "example.effect.stacking.custom"
                    }
                  ]
                }
                """);

        assertTrue(ruleSet.replace());
        assertEquals("example.effect.stacking.custom",
                ruleSet.definitions().get(0).stackingTranslationKey());
        assertEquals("tetra_insight.effect.evidence.manual_json",
                ruleSet.definitions().get(0).evidenceTranslationKey());
    }

    @Test
    void parsesCuriosAndExtendedStackingAliases() {
        EffectApplicabilityRuleSet ruleSet = parse("""
                {
                  "paths": [
                    {
                      "scopes": ["curios"],
                      "triggers": ["heal", "death"],
                      "stacking": "curios_max"
                    }
                  ]
                }
                """);

        EffectApplicabilityDefinition definition = ruleSet.definitions().get(0);
        assertEquals(List.of(EffectScope.CURIOS), definition.scopes());
        assertEquals(List.of(EffectTrigger.HEAL, EffectTrigger.DEATH), definition.triggers());
        assertEquals("tetra_insight.effect.stacking.curios_max",
                definition.stackingTranslationKey());
    }

    @Test
    void rejectsUnsupportedScopeTokens() {
        assertThrows(RuntimeException.class, () -> parse("""
                {
                  "paths": [
                    {
                      "scopes": ["somewhere"],
                      "triggers": ["attack"],
                      "stacking": "item"
                    }
                  ]
                }
                """));
    }

    private static EffectApplicabilityRuleSet parse(String json) {
        return EffectApplicabilityJsonParser.parse(
                JsonParser.parseString(json).getAsJsonObject());
    }
}
