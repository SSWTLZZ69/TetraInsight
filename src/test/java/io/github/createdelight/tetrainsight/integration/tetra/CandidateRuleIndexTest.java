package io.github.createdelight.tetrainsight.integration.tetra;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CandidateRuleIndexTest {
    @Test
    void selectsOnlyRulesIndexedForTheCandidatePlusFallbacks() {
        Rule iron = new Rule("iron", List.of("iron_ingot"));
        Rule metals = new Rule("metals", List.of("iron_ingot", "copper_ingot"));
        Rule fallback = new Rule("fallback", List.of());

        CandidateRuleIndex<String, Rule> index = CandidateRuleIndex.build(
                List.of(iron, metals, fallback),
                Rule::candidates);

        assertEquals(List.of(iron, metals, fallback), index.candidates("iron_ingot"));
        assertEquals(List.of(metals, fallback), index.candidates("copper_ingot"));
        assertEquals(List.of(fallback), index.candidates("stick"));
        assertEquals(3, index.ruleCount());
        assertEquals(2, index.candidateCount());
        assertEquals(1, index.fallbackRuleCount());
    }

    @Test
    void removesDuplicateCandidateKeysForOneRule() {
        Rule duplicate = new Rule("duplicate", List.of("gem", "gem"));

        CandidateRuleIndex<String, Rule> index = CandidateRuleIndex.build(
                List.of(duplicate),
                Rule::candidates);

        assertEquals(List.of(duplicate), index.candidates("gem"));
    }

    private record Rule(String name, List<String> candidates) {
    }
}
