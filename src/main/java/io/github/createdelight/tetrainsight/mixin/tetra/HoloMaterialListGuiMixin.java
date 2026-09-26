package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.HoloMaterialAxisButtonGui;
import io.github.createdelight.tetrainsight.client.HoloMaterialGroupFoldAccess;
import io.github.createdelight.tetrainsight.client.TetraInsightConfig;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierPanelGui;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierLifecycleAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialDossierModalAccess;
import io.github.createdelight.tetrainsight.client.HoloMaterialSelectionAccess;
import io.github.createdelight.tetrainsight.client.HoloSpecialMaterialDossierAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiString;
import se.mickelus.mutil.gui.impl.GuiHorizontalLayoutGroup;
import se.mickelus.mutil.gui.impl.GuiHorizontalScrollable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialDetailGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.material.HoloMaterialListGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloFilterButton;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.MaterialData;
import net.minecraft.world.item.ItemStack;

@Mixin(value = HoloMaterialListGui.class, remap = false)
public abstract class HoloMaterialListGuiMixin
        implements HoloMaterialSelectionAccess, HoloMaterialDossierModalAccess,
        HoloMaterialDossierLifecycleAccess, HoloSpecialMaterialDossierAccess {
    @Shadow
    @Final
    private GuiHorizontalScrollable groupsScroll;

    @Shadow
    @Final
    private GuiHorizontalLayoutGroup groups;

    @Shadow
    @Final
    private HoloMaterialDetailGui detail;

    @Shadow
    private MaterialData hoveredItem;

    @Unique
    private boolean tetraInsight$dossierModal;

    @Shadow
    private void onSelect(MaterialData material) {
    }

    @Unique
    private HoloMaterialDossierPanelGui tetraInsight$specialDossierPanel;

    @Unique
    private boolean tetraInsight$specialDossierOpen;

    @Unique
    private boolean tetraInsight$specialDetailWasVisible;

    @Unique
    private boolean tetraInsight$specialDetailStateCaptured;

    @Unique
    private GuiElement tetraInsight$toolbar;

    @Unique
    private HoloFilterButton tetraInsight$searchField;

    @Unique
    private GuiString tetraInsight$searchHint;

    @Unique
    private final java.util.List<HoloMaterialAxisButtonGui> tetraInsight$axisButtons =
            new java.util.ArrayList<>();

    @Unique
    private final java.util.List<GuiElement> tetraInsight$parkedGroups =
            new java.util.ArrayList<>();

    @Unique
    private java.util.List<GuiElement> tetraInsight$allGroups =
            new java.util.ArrayList<>();

    @Unique
    private String tetraInsight$searchQuery = "";

    @Unique
    private HoloMaterialAxisButtonGui.Axis tetraInsight$sortAxis;

    @Unique
    private boolean tetraInsight$sortAscending;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tetraInsight$addSpecialDossier(
            int x,
            int y,
            int width,
            int height,
            CallbackInfo ci
    ) {
        HoloMaterialListGui self = (HoloMaterialListGui) (Object) this;
        tetraInsight$specialDossierPanel = new HoloMaterialDossierPanelGui(
                0,
                0,
                width,
                205,
                this::tetraInsight$onSpecialDossierVisibilityChanged);
        self.addChild(tetraInsight$specialDossierPanel);

        if (TetraInsightConfig.materialBrowserSearch.get()) {
            tetraInsight$buildToolbar(width);
        }
    }

    @Unique
    private void tetraInsight$buildToolbar(int width) {
        HoloMaterialListGui self = (HoloMaterialListGui) (Object) this;
        tetraInsight$toolbar = new GuiElement(0, 0, width, 12);
        self.addChild(tetraInsight$toolbar);

        tetraInsight$searchField = new HoloFilterButton(0, 1,
                this::tetraInsight$onSearchChange);
        tetraInsight$searchHint = new GuiString(
                11, 1, net.minecraft.client.resources.language.I18n.get(
                        "tetra_insight.holo.material_filter.hint"));
        tetraInsight$searchHint.setColor(0x7f7f7f);
        tetraInsight$toolbar.addChild(tetraInsight$searchField);
        tetraInsight$toolbar.addChild(tetraInsight$searchHint);

        int cursor = width;
        for (int index = HoloMaterialAxisButtonGui.Axis.values().length - 1;
                index >= 0; index--) {
            HoloMaterialAxisButtonGui.Axis axis =
                    HoloMaterialAxisButtonGui.Axis.values()[index];
            HoloMaterialAxisButtonGui button = new HoloMaterialAxisButtonGui(
                    0, 0, axis, this::tetraInsight$onAxisSort);
            cursor -= button.getWidth() + 4;
            button.setX(cursor);
            tetraInsight$axisButtons.add(button);
            tetraInsight$toolbar.addChild(button);
        }

        groupsScroll.setY(12);
        groupsScroll.setHeight(groupsScroll.getHeight() - 12);
        tetraInsight$updateSearchHint();
        tetraInsight$updateAxisButtons();
    }

    @Unique
    private void tetraInsight$onSearchChange(String query) {
        tetraInsight$searchQuery = query == null ? "" : query;
        tetraInsight$updateSearchHint();
        tetraInsight$applyGroupViews();
    }

    /**
     * Left-click cycles each axis button through descending, ascending and
     * unsorted. Right clicks are unreliable inside the holo screen, so the
     * whole interaction lives on the left button.
     */
    @Unique
    private void tetraInsight$onAxisSort(
            HoloMaterialAxisButtonGui.Axis axis) {
        if (tetraInsight$sortAxis != axis) {
            tetraInsight$sortAxis = axis;
            tetraInsight$sortAscending = false;
        } else if (!tetraInsight$sortAscending) {
            tetraInsight$sortAscending = true;
        } else {
            tetraInsight$sortAxis = null;
        }
        tetraInsight$updateAxisButtons();
        tetraInsight$applyGroupViews();
    }

    @Unique
    private void tetraInsight$updateAxisButtons() {
        for (HoloMaterialAxisButtonGui button : tetraInsight$axisButtons) {
            button.updateState(
                    button.axis() == tetraInsight$sortAxis,
                    tetraInsight$sortAscending);
        }
    }

    @Unique
    private void tetraInsight$updateSearchHint() {
        boolean focused = tetraInsight$searchField
                instanceof HoloFilterButtonAccessor accessor
                && accessor.tetraInsight$isInputFocused();
        tetraInsight$searchHint.setVisible(
                tetraInsight$searchQuery.isEmpty() && !focused);
    }

    @Unique
    private java.util.function.Predicate<MaterialData> tetraInsight$searchFilter() {
        String query = tetraInsight$searchQuery.trim()
                .toLowerCase(java.util.Locale.ROOT);
        if (query.isEmpty()) {
            return null;
        }
        return material -> tetraInsight$matchesSearch(material, query);
    }

    @Unique
    private static boolean tetraInsight$matchesSearch(MaterialData material,
            String query) {
        if (material.key != null
                && material.key.toLowerCase(java.util.Locale.ROOT)
                        .contains(query)) {
            return true;
        }
        if (material.category != null
                && (material.category.toLowerCase(java.util.Locale.ROOT)
                        .contains(query)
                        || net.minecraft.client.resources.language.I18n.get(
                                        "tetra.material." + material.category)
                                .toLowerCase(java.util.Locale.ROOT)
                                .contains(query))) {
            return true;
        }
        if (material.material != null && material.material.isValid()) {
            for (net.minecraft.world.item.ItemStack stack
                    : material.material.getApplicableItemStacks()) {
                if (stack != null && !stack.isEmpty()
                        && stack.getHoverName().getString()
                                .toLowerCase(java.util.Locale.ROOT)
                                .contains(query)) {
                    return true;
                }
            }
        }
        if (material.effects != null) {
            for (se.mickelus.tetra.effect.ItemEffect effect
                    : material.effects.getValues()) {
                String key = effect.getKey();
                if (key.toLowerCase(java.util.Locale.ROOT).contains(query)
                        || net.minecraft.client.resources.language.I18n.get(
                                        "tetra.stats." + key)
                                .toLowerCase(java.util.Locale.ROOT)
                                .contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private java.util.Comparator<MaterialData> tetraInsight$currentSorter() {
        if (tetraInsight$sortAxis == null) {
            return null;
        }
        return tetraInsight$sortAscending
                ? tetraInsight$sortAxis.ascending()
                : tetraInsight$sortAxis.descending();
    }

    @Unique
    private void tetraInsight$applyGroupViews() {
        java.util.function.Predicate<MaterialData> filter =
                tetraInsight$searchFilter();
        java.util.Comparator<MaterialData> sorter =
                tetraInsight$currentSorter();
        java.util.List<GuiElement> parked = new java.util.ArrayList<>();
        java.util.List<GuiElement> visible = new java.util.ArrayList<>();
        for (GuiElement child : tetraInsight$allGroups) {
            if (child instanceof HoloMaterialGroupFoldAccess group) {
                int remaining = group.tetraInsight$applyView(filter, sorter);
                if (filter != null && remaining == 0) {
                    child.setVisible(false);
                    parked.add(child);
                    continue;
                }
            }
            child.setVisible(true);
            visible.add(child);
        }
        tetraInsight$parkedGroups.clear();
        tetraInsight$parkedGroups.addAll(parked);
        // GuiElement#getChildren returns an immutable copy and there is no
        // removeChild; rebuild the layout group's content instead.
        groups.clearChildren();
        visible.forEach(groups::addChild);
        tetraInsight$refreshGroupLayout();
    }

    @Inject(method = "updateGroups", at = @At("RETURN"), remap = false)
    private void tetraInsight$configureMaterialGroupFolds(CallbackInfo ci) {
        tetraInsight$parkedGroups.clear();
        tetraInsight$allGroups = new java.util.ArrayList<>(groups.getChildren());
        for (GuiElement child : groups.getChildren()) {
            if (child instanceof HoloMaterialGroupFoldAccess group) {
                group.tetraInsight$configureFold(
                        () -> tetraInsight$toggleGroup(group));
            }
        }
        if (tetraInsight$toolbar != null) {
            tetraInsight$applyGroupViews();
        }
        tetraInsight$refreshGroupLayout();
    }

    @Unique
    private void tetraInsight$toggleGroup(HoloMaterialGroupFoldAccess target) {
        boolean expandTarget = !target.tetraInsight$isExpanded();
        boolean singleExpansion =
                TetraInsightConfig.materialSingleExpansion.get();
        for (GuiElement child : groups.getChildren()) {
            if (child instanceof HoloMaterialGroupFoldAccess group) {
                group.tetraInsight$setExpanded(
                        singleExpansion
                                ? expandTarget && group == target
                                : group == target ? expandTarget
                                        : group.tetraInsight$isExpanded());
            }
        }
        tetraInsight$refreshGroupLayout();
    }

    @Unique
    private void tetraInsight$refreshGroupLayout() {
        groups.forceLayout();
        groupsScroll.markDirty();
        groupsScroll.forceRefreshBounds();
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$blockModalRightClick(
            int mouseX,
            int mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (tetraInsight$dossierModal && button == 1) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void tetraInsight$setMaterialDossierModal(boolean open) {
        tetraInsight$dossierModal = open;
        if (open) {
            groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
            hoveredItem = null;
            groupsScroll.setVisible(false);
            if (tetraInsight$toolbar != null) {
                tetraInsight$toolbar.setVisible(false);
            }
        } else if (tetraInsight$specialDossierOpen) {
            tetraInsight$specialDossierOpen = false;
            if (tetraInsight$specialDossierPanel != null) {
                tetraInsight$specialDossierPanel.closeImmediately();
                tetraInsight$specialDossierPanel.reset();
            }
            tetraInsight$restoreSpecialDetailVisibility();
            groupsScroll.setVisible(true);
        } else {
            groupsScroll.setVisible(true);
        }
        if (tetraInsight$toolbar != null) {
            tetraInsight$toolbar.setVisible(!open);
        }
    }

    @Override
    public boolean tetraInsight$isMaterialDossierModal() {
        return tetraInsight$dossierModal;
    }

    @Override
    public void tetraInsight$resetMaterialDossier() {
        tetraInsight$dossierModal = false;
        boolean hadSpecialDossier = tetraInsight$specialDossierOpen
                || tetraInsight$specialDetailStateCaptured;
        tetraInsight$specialDossierOpen = false;
        groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
        hoveredItem = null;
        onSelect(null);
        groupsScroll.setVisible(true);
        if (tetraInsight$toolbar != null) {
            tetraInsight$toolbar.setVisible(true);
        }
        if (tetraInsight$specialDossierPanel != null) {
            tetraInsight$specialDossierPanel.reset();
        }
        if (hadSpecialDossier) {
            tetraInsight$restoreSpecialDetailVisibility();
        }
        ((HoloMaterialDossierLifecycleAccess) detail)
                .tetraInsight$resetMaterialDossier();
    }

    @Override
    public void tetraInsight$openSpecialMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty() || tetraInsight$specialDossierPanel == null) {
            return;
        }
        if (!tetraInsight$specialDossierOpen) {
            tetraInsight$specialDetailWasVisible = detail.isVisible();
            tetraInsight$specialDetailStateCaptured = true;
        }
        tetraInsight$specialDossierOpen = true;
        tetraInsight$dossierModal = true;
        groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
        hoveredItem = null;
        groupsScroll.setVisible(false);
        if (tetraInsight$toolbar != null) {
            tetraInsight$toolbar.setVisible(false);
        }
        detail.setVisible(false);
        tetraInsight$specialDossierPanel.updateSpecial(stack);
        tetraInsight$specialDossierPanel.open();
    }

    @Unique
    private void tetraInsight$onSpecialDossierVisibilityChanged(boolean visible) {
        if (visible) {
            return;
        }
        tetraInsight$specialDossierOpen = false;
        tetraInsight$dossierModal = false;
        tetraInsight$restoreSpecialDetailVisibility();
        groupsScroll.setVisible(true);
    }

    @Unique
    private void tetraInsight$restoreSpecialDetailVisibility() {
        if (tetraInsight$specialDetailStateCaptured) {
            detail.setVisible(tetraInsight$specialDetailWasVisible);
            tetraInsight$specialDetailStateCaptured = false;
        }
    }

    @Override
    public boolean tetraInsight$selectMaterial(String materialKey) {
        MaterialData material = DataManager.instance.materialData.getData().values().stream()
                .filter(candidate -> candidate != null && materialKey.equals(candidate.key))
                .findFirst()
                .orElse(null);
        if (material == null) {
            return false;
        }
        groupsScroll.updateFocusState(0, 0, -1000000, -1000000);
        hoveredItem = null;
        onSelect(material);
        return true;
    }
}
