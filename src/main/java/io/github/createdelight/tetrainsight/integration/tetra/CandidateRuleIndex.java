package io.github.createdelight.tetrainsight.integration.tetra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class CandidateRuleIndex<K, R> {
    private final Map<K, List<R>> rulesByCandidate;
    private final List<R> fallbackRules;
    private final int ruleCount;

    private CandidateRuleIndex(
            Map<K, List<R>> rulesByCandidate,
            List<R> fallbackRules,
            int ruleCount
    ) {
        this.rulesByCandidate = rulesByCandidate;
        this.fallbackRules = fallbackRules;
        this.ruleCount = ruleCount;
    }

    static <K, R> CandidateRuleIndex<K, R> empty() {
        return new CandidateRuleIndex<>(Map.of(), List.of(), 0);
    }

    static <K, R> CandidateRuleIndex<K, R> build(
            Collection<R> rules,
            Function<R, ? extends Collection<K>> candidateExtractor
    ) {
        LinkedHashMap<K, List<R>> rulesByCandidate = new LinkedHashMap<>();
        List<R> fallbackRules = new ArrayList<>();

        for (R rule : rules) {
            Collection<K> extractedCandidates = candidateExtractor.apply(rule);
            LinkedHashSet<K> candidates = extractedCandidates == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(extractedCandidates);
            candidates.remove(null);
            if (candidates.isEmpty()) {
                fallbackRules.add(rule);
                continue;
            }
            for (K candidate : candidates) {
                rulesByCandidate.computeIfAbsent(candidate, ignored -> new ArrayList<>())
                        .add(rule);
            }
        }

        LinkedHashMap<K, List<R>> immutableRules = new LinkedHashMap<>();
        rulesByCandidate.forEach((candidate, indexedRules) ->
                immutableRules.put(candidate, List.copyOf(indexedRules)));
        return new CandidateRuleIndex<>(
                Map.copyOf(immutableRules),
                List.copyOf(fallbackRules),
                rules.size());
    }

    List<R> candidates(K candidate) {
        List<R> indexedRules = rulesByCandidate.getOrDefault(candidate, List.of());
        if (fallbackRules.isEmpty()) {
            return indexedRules;
        }
        if (indexedRules.isEmpty()) {
            return fallbackRules;
        }
        List<R> combined = new ArrayList<>(indexedRules.size() + fallbackRules.size());
        combined.addAll(indexedRules);
        combined.addAll(fallbackRules);
        return List.copyOf(combined);
    }

    int ruleCount() {
        return ruleCount;
    }

    int candidateCount() {
        return rulesByCandidate.size();
    }

    int fallbackRuleCount() {
        return fallbackRules.size();
    }
}
