package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialCandidateSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSlotSnapshot;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.createdelight.tetrainsight.integration.tetra.model.TetraProbeSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class TetraInsightClientCommands {
    private TetraInsightClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        var probe = Commands.literal("probe")
                .executes(context -> showSummary(context.getSource()))
                .then(Commands.literal("dump")
                        .executes(context -> dumpMissingTranslations(context.getSource())))
                .then(Commands.literal("candidates")
                        .then(Commands.argument("query", StringArgumentType.greedyString())
                                .executes(context -> showCandidates(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "query")))));

        event.getDispatcher().register(Commands.literal("tetrainsight").then(probe));
    }

    private static int showSummary(net.minecraft.commands.CommandSourceStack source) {
        TetraProbeSnapshot snapshot = TetraDataProbe.snapshot();
        if (snapshot.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("tetra_insight.command.probe.empty")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable(
                "tetra_insight.command.probe.summary",
                snapshot.materialImprovements().size(),
                snapshot.materialVariants().size(),
                snapshot.materialSchematics().size(),
                snapshot.missingTranslationCount()
        ).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("Linked material schematics: "
                + snapshot.linkedMaterialSchematicCount()
                + "; missing display definitions with linked extracts: "
                + snapshot.missingTranslationWithLinkedDefinitionCount())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Indexed logical material candidates: "
                + snapshot.totalMaterialCandidateCount())
                .withStyle(ChatFormatting.GRAY), false);
        return snapshot.materialSchematics().size();
    }

    private static int dumpMissingTranslations(net.minecraft.commands.CommandSourceStack source) {
        TetraProbeSnapshot snapshot = TetraDataProbe.snapshot();
        showSummary(source);

        int shown = 0;
        for (MaterialSchematicSnapshot schematic : snapshot.materialSchematics()) {
            if (!schematic.hasAuthorTranslation()) {
                source.sendSuccess(() -> Component.literal("- " + schematic.schematicKey()
                        + " [" + schematic.materialSelectors().size() + " selectors, "
                        + schematic.displayTranslation().provenance().name().toLowerCase(java.util.Locale.ROOT)
                        + ", " + schematic.displayTranslation().entries().size() + " entries]")
                        .withStyle(ChatFormatting.GRAY), false);
                shown++;
                if (shown >= 20) {
                    break;
                }
            }
        }
        return shown;
    }

    private static int showCandidates(net.minecraft.commands.CommandSourceStack source, String query) {
        TetraProbeSnapshot snapshot = TetraDataProbe.snapshot();
        MaterialSchematicSnapshot schematic = snapshot.materialSchematics().stream()
                .filter(value -> value.schematicKey().contains(query))
                .findFirst()
                .orElse(null);

        if (schematic == null) {
            source.sendFailure(Component.literal("No material schematic matched: " + query));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(schematic.schematicKey() + ": "
                + schematic.materialSlots().size() + " slots, "
                + schematic.candidateCount() + " logical candidates")
                .withStyle(ChatFormatting.AQUA), false);

        int shown = 0;
        for (MaterialSlotSnapshot slot : schematic.materialSlots()) {
            source.sendSuccess(() -> Component.literal("Slot " + slot.slotIndex() + ": "
                    + slot.candidates().size() + " candidates").withStyle(ChatFormatting.GRAY), false);
            for (MaterialCandidateSnapshot candidate : slot.candidates()) {
                source.sendSuccess(() -> Component.literal("  - " + candidate.materialKey()
                        + " x" + candidate.requiredQuantity()
                        + " [" + format(candidate.primary())
                        + ", " + format(candidate.secondary())
                        + ", " + format(candidate.tertiary())
                        + "] sources=" + candidate.sourceItems().size()
                        + " glyph=#" + String.format(java.util.Locale.ROOT, "%06X",
                                candidate.glyphTint() & 0xffffff)
                        + (candidate.hiddenInGlobalMaterialBrowser() ? " global-hidden" : "")), false);
                shown++;
                if (shown >= 20) {
                    return shown;
                }
            }
        }
        return shown;
    }

    private static String format(Float value) {
        return value == null ? "-" : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
