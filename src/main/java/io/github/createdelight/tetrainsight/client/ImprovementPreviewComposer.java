package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.mixin.tetra.OutcomeStackAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.OutcomeStack;
import se.mickelus.tetra.module.schematic.OutcomePreview;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

/**
 * Rebuilds Tetra improvement previews from the base outcome.
 *
 * <p>The GUI mixin owns selection state and navigation. This class owns only
 * preview composition and selection identity, so the same rules are used by
 * click, hover, and comparison paths.</p>
 */
public final class ImprovementPreviewComposer {
    private static final String BOOK_ENCHANT = "book_enchant";

    private ImprovementPreviewComposer() {
    }

    public static List<OutcomeStack> combineSelections(
            List<OutcomeStack> ordinary,
            List<HoloHoningOutcomeStack> honing
    ) {
        List<OutcomeStack> selections = new ArrayList<>(ordinary);
        selections.addAll(honing);
        return selections;
    }

    public static OutcomePreview compose(
            OutcomePreview base,
            String slot,
            List<? extends OutcomeStack> selections
    ) {
        OutcomePreview outcome = base.clone();
        List<OutcomeStack> ordinary = selections.stream()
                .filter(selection -> !isBookEnchant(selection))
                .map(selection -> (OutcomeStack) selection)
                .toList();
        List<OutcomeStack> enchantments = selections.stream()
                .filter(ImprovementPreviewComposer::isBookEnchant)
                .map(selection -> (OutcomeStack) selection)
                .toList();
        outcome = applySelections(ordinary, outcome, slot);
        return applySelections(enchantments, outcome, slot);
    }

    public static boolean sameSelection(OutcomeStack left, OutcomeStack right) {
        OutcomeStackAccessor leftAccess = (OutcomeStackAccessor) left;
        OutcomeStackAccessor rightAccess = (OutcomeStackAccessor) right;
        return leftAccess.tetraInsight$getSchematic().getKey().equals(
                        rightAccess.tetraInsight$getSchematic().getKey())
                && matchesSelection(
                        leftAccess.tetraInsight$getPreview(),
                        rightAccess.tetraInsight$getPreview());
    }

    public static boolean sameSelectionFamily(OutcomeStack left, OutcomeStack right) {
        OutcomeStackAccessor leftAccess = (OutcomeStackAccessor) left;
        OutcomeStackAccessor rightAccess = (OutcomeStackAccessor) right;
        if (left instanceof HoloHoningOutcomeStack leftChain
                && right instanceof HoloHoningOutcomeStack rightChain
                && !leftChain.chainKey().isBlank()
                && leftChain.chainKey().equals(rightChain.chainKey())) {
            return true;
        }
        if (left instanceof HoloHoningOutcomeStack leftChain
                && leftChain.chainKey().equals(
                        rightAccess.tetraInsight$getPreview().variantKey)) {
            return true;
        }
        if (right instanceof HoloHoningOutcomeStack rightChain
                && rightChain.chainKey().equals(
                        leftAccess.tetraInsight$getPreview().variantKey)) {
            return true;
        }
        return leftAccess.tetraInsight$getSchematic().getKey().equals(
                        rightAccess.tetraInsight$getSchematic().getKey())
                && Objects.equals(
                        leftAccess.tetraInsight$getPreview().variantKey,
                        rightAccess.tetraInsight$getPreview().variantKey);
    }

    public static boolean matchesSelection(
            OutcomePreview candidate,
            OutcomePreview requested
    ) {
        if (!Objects.equals(candidate.variantKey, requested.variantKey)
                || candidate.level != requested.level) {
            return false;
        }
        ItemStack[] candidateMaterials = candidate.materials != null
                ? candidate.materials
                : new ItemStack[0];
        ItemStack[] requestedMaterials = requested.materials != null
                ? requested.materials
                : new ItemStack[0];
        if (candidateMaterials.length != requestedMaterials.length) {
            return false;
        }
        for (int index = 0; index < candidateMaterials.length; index++) {
            if (!ItemStack.isSameItemSameTags(
                            candidateMaterials[index], requestedMaterials[index])
                    || candidateMaterials[index].getCount()
                            != requestedMaterials[index].getCount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBookEnchant(OutcomeStack selection) {
        OutcomeStackAccessor access = (OutcomeStackAccessor) selection;
        return BOOK_ENCHANT.equals(unwrapDisplaySchematic(
                access.tetraInsight$getSchematic()).getKey());
    }

    private static OutcomePreview applySelections(
            List<? extends OutcomeStack> selections,
            OutcomePreview base,
            String slot
    ) {
        OutcomePreview outcome = base;
        List<OutcomeStack> pending = new ArrayList<>(selections);
        boolean progressed;
        do {
            progressed = false;
            for (int index = 0; index < pending.size(); index++) {
                OutcomePreview resolved = resolveOutcome(pending.get(index), outcome, slot);
                if (resolved == null) {
                    continue;
                }
                outcome = resolved;
                pending.remove(index--);
                progressed = true;
            }
        } while (progressed && !pending.isEmpty());

        if (!pending.isEmpty()) {
            TetraInsight.LOGGER.debug(
                    "Could not recompose {} improvement selections on the current preview",
                    pending.size());
        }
        return outcome;
    }

    private static OutcomePreview resolveOutcome(
            OutcomeStack selected,
            OutcomePreview base,
            String slot
    ) {
        OutcomeStackAccessor access = (OutcomeStackAccessor) selected;
        OutcomePreview requested = access.tetraInsight$getPreview();
        UpgradeSchematic schematic = unwrapDisplaySchematic(
                access.tetraInsight$getSchematic());
        for (OutcomePreview candidate : schematic.getPreviews(base.itemStack, slot)) {
            if (matchesSelection(candidate, requested)) {
                return candidate;
            }
        }
        return null;
    }

    private static UpgradeSchematic unwrapDisplaySchematic(UpgradeSchematic schematic) {
        if (schematic instanceof HoloDisplaySchematicAccess display) {
            return display.tetraInsight$delegate();
        }
        return schematic;
    }
}
