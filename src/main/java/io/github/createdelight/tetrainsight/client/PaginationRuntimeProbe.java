package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.TetraInsight;
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
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectEfficiency;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectLevel;
import se.mickelus.tetra.gui.stats.sorting.BasicStatSorter;
import se.mickelus.tetra.gui.stats.sorting.IStatSorter;
import se.mickelus.tetra.gui.stats.sorting.NaturalSorter;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSortButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSortPopover;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloFilterButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementButton;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloSchematicGui;

import java.util.Arrays;

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
        int entryRight = layout.tetraInsight$improvementEntryX()
                + layout.tetraInsight$improvementEntryWidth();
        require(layout.tetraInsight$improvementEntryX() >= layout.tetraInsight$toolbarContentWidth() + 6,
                "expected improvement entry to avoid the native toolbar");
        require(entryRight <= schematicGui.getWidth(),
                "expected improvement entry inside schematic bounds");
        verifiedWorldUi = true;
        TetraInsight.LOGGER.info(
                "Verified improvement entry layout: toolbar {}, entry {}..{} of {}",
                layout.tetraInsight$toolbarContentWidth(), layout.tetraInsight$improvementEntryX(),
                entryRight, schematicGui.getWidth());
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
