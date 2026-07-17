package io.github.createdelight.tetrainsight.integration.tetra.model;

public record MaterialTranslationEntry(
        MaterialInputChannel input,
        MaterialOutputKind outputKind,
        String outputId,
        String operation,
        double actualCoefficient,
        int generatedDisplayLevel
) {
}
