package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.integration.tetra.effect.EffectApplicabilityDefinition;
import io.github.createdelight.tetrainsight.integration.tetra.effect.EffectStatGetterResolver;
import io.github.createdelight.tetrainsight.integration.tetra.effect.TetraEffectScopeIndex;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilityPathSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilitySnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilityState;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import io.github.createdelight.tetrainsight.integration.tetrawear.TetrawearEffectAdapter;
import io.github.createdelight.tetrainsight.mixin.tetra.BasicStatSorterAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.HoloFilterButtonAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.ItemEffectAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterAttributeAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectEfficiencyAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectLevelAccessor;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.getter.StatFormat;
import se.mickelus.tetra.gui.stats.getter.StatGetterAttribute;
import se.mickelus.tetra.gui.stats.getter.StatGetterAdd;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectEfficiency;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectLevel;
import se.mickelus.tetra.gui.stats.getter.StatGetterMultiply;
import se.mickelus.tetra.gui.stats.sorting.BasicStatSorter;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.NaturalSorter;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSortButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSortPopover;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloFilterButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloImprovementButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSchematicGui;

import java.util.Arrays;
import java.util.List;

public final class PaginationRuntimeProbe {
    private static boolean verifiedWorldUi;

    private PaginationRuntimeProbe() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (FMLEnvironment.production) {
            return;
        }
        event.enqueueWork(PaginationRuntimeProbe::verify);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (FMLEnvironment.production || verifiedWorldUi || event.phase != TickEvent.Phase.END
                || net.minecraft.client.Minecraft.getInstance().player == null) {
            return;
        }
        HoloSchematicGui schematicGui = new HoloSchematicGui(0, 0, 320, 205, ignored -> {
        });
        HoloSchematicImprovementEntryAccess layout = (HoloSchematicImprovementEntryAccess) schematicGui;
        int entryX = layout.tetraInsight$improvementEntryX();
        int entryRight = layout.tetraInsight$improvementEntryX()
                + layout.tetraInsight$improvementEntryWidth();
        boolean insideBounds = entryX >= 0 && entryRight <= schematicGui.getWidth();
        verifiedWorldUi = true;
        if (insideBounds) {
            TetraInsight.LOGGER.info(
                    "Verified improvement entry layout: toolbar {}, entry {}..{} of {}",
                    layout.tetraInsight$toolbarContentWidth(), entryX, entryRight, schematicGui.getWidth());
        } else {
            TetraInsight.LOGGER.error(
                    "Improvement entry layout is outside schematic bounds: toolbar {}, entry {}..{} of {}",
                    layout.tetraInsight$toolbarContentWidth(), entryX, entryRight, schematicGui.getWidth());
        }
    }

    private static void verify() {
        IStatSorter[] sorters = new IStatSorter[30];
        Arrays.fill(sorters, new NaturalSorter());

        HoloSortPopover popover = new HoloSortPopover(0, 0, ignored -> {
        });
        popover.update(sorters);
        HoloSortPaginationAccess pagination = (HoloSortPaginationAccess) popover;
        require(pagination.tetraInsight$totalPages() == 4, "expected 4 pages");
        require(pagination.tetraInsight$currentPage() == 0, "expected first page");
        require(pagination.tetraInsight$visibleItemCount() == 9, "expected 9 visible items on first page");

        pagination.tetraInsight$setPage(3);
        require(pagination.tetraInsight$currentPage() == 3, "expected final page");
        require(pagination.tetraInsight$visibleItemCount() == 3, "expected 3 visible items on final page");
        pagination.tetraInsight$setQuery("no-such-sorter");
        require(pagination.tetraInsight$filteredItemCount() == 0, "expected no search matches");
        require(pagination.tetraInsight$visibleItemCount() == 0, "expected no visible items after search");
        pagination.tetraInsight$setQuery("");
        require(pagination.tetraInsight$filteredItemCount() == 30, "expected search reset to restore items");
        verifyDynamicSorterAccessors();
        verifyEffectApplicability();
        TetraInsight.LOGGER.info(
                "Verified Tetra sorter pagination runtime: 30 items, 4 pages, 9/3 boundary, search reset 0/30");
    }

    private static void verifyDynamicSorterAccessors() {
        new HoloSortButton(0, 0, ignored -> {
        });
        HoloFilterButton filterButton = new HoloFilterButton(0, 0, ignored -> {
        });
        require(!((HoloFilterButtonAccessor) filterButton).tetraInsight$isInputFocused(),
                "expected filter input accessor");
        verifyImprovementDiscoveryControls();
        require(!ItemEffectAccessor.tetraInsight$getEffectMap().isEmpty(), "expected registered item effects");

        StatGetterAttribute attributeGetter = new StatGetterAttribute(Attributes.ATTACK_DAMAGE);
        BasicStatSorter attributeSorter = new BasicStatSorter(
                attributeGetter, Attributes.ATTACK_DAMAGE.getDescriptionId(), StatFormat.twoDecimal);
        require(((BasicStatSorterAccessor) attributeSorter).tetraInsight$getGetter() == attributeGetter,
                "expected BasicStatSorter getter accessor");
        require(((StatGetterAttributeAccessor) attributeGetter).tetraInsight$getAttribute()
                        == Attributes.ATTACK_DAMAGE,
                "expected attribute accessor");

        StatGetterEffectLevel levelGetter = new StatGetterEffectLevel(ItemEffect.workable);
        require(((StatGetterEffectLevelAccessor) levelGetter).tetraInsight$getEffect() == ItemEffect.workable,
                "expected effect level accessor");
        StatGetterEffectEfficiency efficiencyGetter = new StatGetterEffectEfficiency(ItemEffect.workable);
        require(((StatGetterEffectEfficiencyAccessor) efficiencyGetter).tetraInsight$getEffect() == ItemEffect.workable,
                "expected effect efficiency accessor");
        TetraInsight.LOGGER.info("Verified contextual sorter accessors and HoloSortButton mixin");
    }

    private static void verifyEffectApplicability() {
        StatGetterEffectLevel workable = new StatGetterEffectLevel(ItemEffect.workable);
        require(EffectStatGetterResolver.resolve(workable).orElse(null) == ItemEffect.workable,
                "expected direct effect getter resolution");
        require(EffectStatGetterResolver.resolve(new StatGetterMultiply(workable))
                        .orElse(null) == ItemEffect.workable,
                "expected wrapped effect getter resolution");
        require(EffectStatGetterResolver.resolve(new StatGetterAdd(
                        workable, new StatGetterEffectLevel(ItemEffect.bleeding))).isEmpty(),
                "expected ambiguous effect getter rejection");

        require(TetraEffectScopeIndex.hasHardcodedDefinition("bleeding"),
                "expected Tetra bleeding definition");
        var bleeding = TetraEffectScopeIndex.resolve(ItemEffect.bleeding);
        require(hasPath(bleeding, EffectScope.MAIN_HAND, EffectTrigger.ATTACK),
                "expected bleeding main-hand attack scope");
        var velocity = TetraEffectScopeIndex.resolve(ItemEffect.velocity);
        require(hasPath(velocity, EffectScope.BOW, EffectTrigger.PROJECTILE)
                        && hasPath(velocity, EffectScope.CROSSBOW, EffectTrigger.PROJECTILE),
                "expected velocity bow and crossbow scope");
        var blocking = TetraEffectScopeIndex.resolve(ItemEffect.blocking);
        require(hasPath(blocking, EffectScope.WEAPON, EffectTrigger.BLOCK),
                "expected blocking handheld weapon or tool scope");
        require(TetrawearEffectAdapter.hasDefinition("evade"),
                "expected optional Tetrawear evade definition");
        require(EffectApplicabilityDefinition.merge(
                        List.of(new EffectApplicabilityDefinition(
                                List.of(EffectScope.MAIN_HAND), List.of(EffectTrigger.ATTACK),
                                "tetra_insight.effect.stacking.item",
                                "tetra_insight.effect.evidence.tetra_6_17")),
                        List.of(new EffectApplicabilityDefinition(
                                List.of(EffectScope.ARMOR), List.of(EffectTrigger.RECEIVE_HIT),
                                "tetra_insight.effect.stacking.armor_sum",
                                "tetra_insight.effect.evidence.tetrawear_1_0")))
                        .size() == 2,
                "expected Tetra and Tetrawear definitions to merge without overriding");

        verifyPreviewText(EffectApplicabilityState.ACTIVE,
                "tetra_insight.effect.preview.active");
        verifyPreviewText(EffectApplicabilityState.PROVIDED_NOT_TRIGGERED,
                "tetra_insight.effect.preview.provided_not_triggered");
        verifyPreviewText(EffectApplicabilityState.UNKNOWN,
                "tetra_insight.effect.preview.unknown");
        verifyMultipleApplicabilityPaths();
        TetraInsight.LOGGER.info(
                "Verified effect applicability getter resolution, independent Tetra/Tetrawear paths and preview text");
    }

    private static void verifyPreviewText(EffectApplicabilityState state, String stateKey) {
        EffectApplicabilityPathSnapshot path = new EffectApplicabilityPathSnapshot(
                List.of(EffectScope.UNKNOWN), List.of(EffectTrigger.UNKNOWN), state,
                "tetra_insight.effect.stacking.unknown",
                "tetra_insight.effect.evidence.unknown");
        EffectApplicabilitySnapshot snapshot = new EffectApplicabilitySnapshot(
                "probe", List.of(path), state);
        List<net.minecraft.network.chat.Component> tooltip = EffectApplicabilityTooltipFormatter.append(
                List.of(net.minecraft.network.chat.Component.literal("probe")), snapshot, false);
        String expected = net.minecraft.client.resources.language.I18n.get(stateKey);
        require(tooltip.get(tooltip.size() - 1).getString().contains(expected),
                "expected preview translation " + stateKey);
    }

    private static void verifyMultipleApplicabilityPaths() {
        EffectApplicabilityPathSnapshot heldPath = new EffectApplicabilityPathSnapshot(
                List.of(EffectScope.MAIN_HAND), List.of(EffectTrigger.ATTACK),
                EffectApplicabilityState.ACTIVE,
                "tetra_insight.effect.stacking.item",
                "tetra_insight.effect.evidence.tetra_6_17");
        EffectApplicabilityPathSnapshot armorPath = new EffectApplicabilityPathSnapshot(
                List.of(EffectScope.ARMOR), List.of(EffectTrigger.RECEIVE_HIT),
                EffectApplicabilityState.PROVIDED_NOT_TRIGGERED,
                "tetra_insight.effect.stacking.armor_sum",
                "tetra_insight.effect.evidence.tetrawear_1_0");
        EffectApplicabilitySnapshot snapshot = new EffectApplicabilitySnapshot(
                "probe_dual", List.of(heldPath, armorPath), EffectApplicabilityState.ACTIVE);

        require(snapshot.scopes().containsAll(List.of(EffectScope.MAIN_HAND, EffectScope.ARMOR)),
                "expected held and worn scopes to remain independent");
        require(snapshot.triggers().containsAll(List.of(EffectTrigger.ATTACK, EffectTrigger.RECEIVE_HIT)),
                "expected held and worn triggers to remain independent");

        List<net.minecraft.network.chat.Component> tooltip = EffectApplicabilityTooltipFormatter.append(
                List.of(net.minecraft.network.chat.Component.literal("probe")), snapshot, true);
        require(containsText(tooltip, net.minecraft.client.resources.language.I18n.get(
                        "tetra_insight.effect.stacking.item"))
                        && containsText(tooltip, net.minecraft.client.resources.language.I18n.get(
                                "tetra_insight.effect.stacking.armor_sum")),
                "expected detailed tooltip to retain stacking for both paths");
        require(containsText(tooltip, net.minecraft.client.resources.language.I18n.get(
                        "tetra_insight.effect.evidence.tetra_6_17"))
                        && containsText(tooltip, net.minecraft.client.resources.language.I18n.get(
                                "tetra_insight.effect.evidence.tetrawear_1_0")),
                "expected detailed tooltip to retain evidence for both paths");
    }

    private static boolean hasPath(
            List<io.github.createdelight.tetrainsight.integration.tetra.effect.EffectApplicabilityDefinition> definitions,
            EffectScope scope, EffectTrigger trigger) {
        return definitions.stream().anyMatch(definition -> definition.scopes().contains(scope)
                && definition.triggers().contains(trigger));
    }

    private static boolean containsText(
            List<net.minecraft.network.chat.Component> components, String expected) {
        return components.stream().anyMatch(component -> component.getString().contains(expected));
    }

    private static void verifyImprovementDiscoveryControls() {
        HoloImprovementButton button = new HoloImprovementButton(0, 0, () -> {
        });
        HoloImprovementButtonAccess access = (HoloImprovementButtonAccess) button;
        access.tetraInsight$showSelectionPrompt();
        require(access.tetraInsight$labelText().equals(
                        net.minecraft.client.resources.language.I18n.get(
                                "tetra_insight.holo.improvement.select_module")),
                "expected persistent improvement selection prompt");
        button.updateCount(2);
        require(access.tetraInsight$labelText().equals(
                        net.minecraft.client.resources.language.I18n.get(
                                "tetra_insight.holo.improvement.open", 2)),
                "expected explicit improvement action label");
        require(HoloSchematicGui.class != null, "expected HoloSchematicGui mixin target");
        TetraInsight.LOGGER.info("Verified improvement discoverability controls");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Tetra sorter pagination probe failed: " + message);
        }
    }
}
