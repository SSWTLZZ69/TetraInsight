package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.MaterialInsightIndex;
import io.github.createdelight.tetrainsight.integration.tetra.MaterialUsageHierarchyResolver;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialImprovementUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialItemUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialModuleUsageSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialProfileSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialStatPreviewSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageNavigationSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialUsageTreeSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiItem;
import se.mickelus.mutil.gui.GuiRect;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.gui.ZOffsetGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.HoloGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloStatsGui;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloSchematicListItemGui;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

public final class HoloMaterialDossierPanelGui extends ZOffsetGui {
    private static final int HEADER_HEIGHT = 19;
    private static final int CONTENT_MARGIN = 7;
    private static final int ROW_HEIGHT = 16;
    private static final int SCROLL_STEP = 2;
    private static final int STATS_RESERVED_HEIGHT = 64;
    private static final int OPEN_DELAY = 120;
    private static final int OPEN_DURATION = 80;
    private static final int CLOSE_DURATION = 60;
    private static final int PAGE_EXIT_DURATION = 45;
    private static final int PAGE_ENTER_DURATION = 65;
    private static final int TREE_INDENT = 18;
    private static final int ITEM_FOLD_X = TREE_INDENT;
    private static final int ITEM_LABEL_X = TREE_INDENT * 2;
    private static final int SLOT_FOLD_X = TREE_INDENT * 2;
    private static final int SLOT_LABEL_X = TREE_INDENT * 3;
    private static final int MODULE_FOLD_X = TREE_INDENT * 3;
    private static final int MODULE_LINK_X = MODULE_FOLD_X + 14;
    private static final int IMPROVEMENT_LINK_X = MODULE_LINK_X + TREE_INDENT;
    private static final int COLOR_PRIMARY = 0xffffff;
    private static final int COLOR_SECONDARY = 0xaaaaaa;
    private static final int COLOR_MUTED = 0x777777;
    private static final int COLOR_DIVIDER = 0x444444;

    private final int panelWidth;
    private final int panelHeight;
    private final int panelX;
    private final int panelY;
    private final int contentWidth;
    private final int contentHeight;
    private final int usageListHeight;
    private final int visibleUsageRows;
    private final GuiStringOutline title;
    private final HoloSortPageControls definitionControls;
    private final TextButton collapseAllButton;
    private final TextButton expandAllButton;
    private final TabButton materialTab;
    private final TabButton usageTab;
    private final GuiElement pageContent;
    private final ComponentLinesGui materialContent;
    private final GuiElement usageContent;
    private final GuiRect statsDivider;
    private final GuiStringOutline statsTitle;
    private final GuiStringOutline statsHint;
    private final HoloStatsGui statsGui;
    private final GuiRect scrollTrack;
    private final GuiRect scrollThumb;
    private final Consumer<Boolean> onVisibilityChanged;
    private final Set<String> collapsedItems = new HashSet<>();
    private final Set<String> collapsedSlots = new HashSet<>();
    private final Set<String> collapsedModules = new HashSet<>();
    private MaterialProfileSnapshot profile;
    private ItemStack specialStack = ItemStack.EMPTY;
    private boolean specialOnly;
    private MaterialUsageTreeSnapshot usageTree;
    private MaterialUsageTreeSnapshot specialUsageTree;
    private List<FormattedCharSequence> materialLines = List.of();
    private List<UsageRow> usageRows = List.of();
    private boolean usageResolved;
    private boolean statsAvailable;
    private boolean closing;
    private boolean pageTransitioning;
    private Page pageType = Page.MATERIAL;
    private int usageScroll;
    private KeyframeAnimation panelAnimation;
    private KeyframeAnimation pageAnimation;

    public HoloMaterialDossierPanelGui(
            int x,
            int y,
            int width,
            int height,
            Consumer<Boolean> onVisibilityChanged
    ) {
        super(x, y, 240.0D);
        this.panelX = x;
        this.panelY = y;
        this.panelWidth = width;
        this.panelHeight = height;
        this.contentWidth = width - CONTENT_MARGIN * 2 - 4;
        this.contentHeight = height - HEADER_HEIGHT - 7;
        this.usageListHeight = Math.max(ROW_HEIGHT,
                contentHeight - STATS_RESERVED_HEIGHT);
        this.visibleUsageRows = Math.max(1, usageListHeight / ROW_HEIGHT);
        this.onVisibilityChanged = onVisibilityChanged;
        setWidth(width);
        setHeight(height);

        GuiRect backdrop = new GuiRect(0, 0, width, height, 0);
        backdrop.setOpacity(0.97f);
        addChild(backdrop);
        addCornerStrokes();

        title = new GuiStringOutline(6, 4, "");
        addChild(title);

        definitionControls = new HoloSortPageControls(
                () -> moveDefinition(-1),
                () -> moveDefinition(1));
        definitionControls.setX(Math.min(width - 170, Math.max(92, width / 2 - 58)));
        definitionControls.setY(3);
        addChild(definitionControls);

        collapseAllButton = new TextButton(
                width - 148,
                3,
                "[-]",
                () -> setAllBranchesCollapsed(true),
                Component.translatable("tetra_insight.material.dossier.collapse_all"));
        expandAllButton = new TextButton(
                width - 128,
                3,
                "[+]",
                () -> setAllBranchesCollapsed(false),
                Component.translatable("tetra_insight.material.dossier.expand_all"));
        collapseAllButton.setVisible(false);
        expandAllButton.setVisible(false);
        addChild(collapseAllButton);
        addChild(expandAllButton);

        materialTab = new TabButton(width - 108, 3,
                "tetra_insight.material.dossier.tab.material",
                () -> setPageType(Page.MATERIAL));
        usageTab = new TabButton(width - 60, 3,
                "tetra_insight.material.dossier.tab.usage",
                () -> setPageType(Page.USAGE));
        addChild(materialTab);
        addChild(usageTab);
        addChild(new TextButton(width - 12, 3, "x", this::close));

        pageContent = new GuiElement(0, 0, width, height);
        addChild(pageContent);

        materialContent = new ComponentLinesGui(
                CONTENT_MARGIN, HEADER_HEIGHT, contentWidth, contentHeight);
        pageContent.addChild(materialContent);
        usageContent = new GuiElement(
                CONTENT_MARGIN, HEADER_HEIGHT, contentWidth, usageListHeight);
        pageContent.addChild(usageContent);

        int statsY = HEADER_HEIGHT + usageListHeight;
        statsDivider = new GuiRect(
                CONTENT_MARGIN, statsY, contentWidth, 1, COLOR_DIVIDER);
        statsDivider.setVisible(false);
        pageContent.addChild(statsDivider);
        statsTitle = new GuiStringOutline(CONTENT_MARGIN, statsY + 3, "");
        statsTitle.setColor(COLOR_SECONDARY);
        statsTitle.setVisible(false);
        pageContent.addChild(statsTitle);
        statsHint = new GuiStringOutline(
                CONTENT_MARGIN, statsY + 18,
                Component.translatable(
                        "tetra_insight.material.dossier.stats_hint").getString());
        statsHint.setColor(COLOR_MUTED);
        statsHint.setVisible(false);
        pageContent.addChild(statsHint);
        statsGui = new HoloStatsGui(0, statsY + 13);
        ((HoloStatsLayoutAccess) statsGui).tetraInsight$setGridLayout(4, 84);
        statsGui.setVisible(false);
        pageContent.addChild(statsGui);

        scrollTrack = new GuiRect(width - 4, HEADER_HEIGHT, 1, usageListHeight, 0x333333);
        scrollThumb = new GuiRect(width - 5, HEADER_HEIGHT, 3, 12, 0xaaaaaa);
        pageContent.addChild(scrollTrack);
        pageContent.addChild(scrollThumb);
        setVisible(false);
    }

    public void update(MaterialProfileSnapshot profile) {
        boolean changed = specialOnly
                || this.profile == null
                || profile == null
                || !this.profile.materialKey().equals(profile.materialKey());
        specialOnly = false;
        specialStack = ItemStack.EMPTY;
        this.profile = profile;
        if (profile == null) {
            closeImmediately();
            return;
        }

        definitionControls.setVisible(true);
        materialTab.setVisible(true);

        Font font = Minecraft.getInstance().font;
        int titleWidth = Math.max(54, definitionControls.getX() - 10);
        title.setString(font.plainSubstrByWidth(
                MaterialInsightText.materialName(profile).getString(), titleWidth));
        MaterialDossierSession.DefinitionPage definitionPage =
                MaterialDossierSession.pageFor(profile.materialKey());
        definitionControls.update(58, definitionPage.index() + 1, definitionPage.total());

        if (changed) {
            usageResolved = false;
            usageTree = null;
            specialUsageTree = null;
            usageRows = List.of();
            usageScroll = 0;
            collapsedItems.clear();
            collapsedSlots.clear();
            collapsedModules.clear();
        }
        rebuildContent();
        if (MaterialDossierSession.consumeAutoOpen(profile.materialKey())) {
            pageType = Page.MATERIAL;
            usageScroll = 0;
            open();
        }
    }

    public void updateSpecial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            reset();
            return;
        }

        boolean changed = !specialOnly || !sameSpecialStack(specialStack, stack);
        profile = null;
        specialOnly = true;
        specialStack = stack.copy();
        pageType = Page.USAGE;
        definitionControls.setVisible(false);
        materialTab.setVisible(false);
        usageTab.setVisible(true);

        Font font = Minecraft.getInstance().font;
        int titleWidth = Math.max(54, usageTab.getX() - 10);
        title.setString(font.plainSubstrByWidth(
                stack.getHoverName().getString(), titleWidth));

        if (changed) {
            usageResolved = false;
            usageTree = null;
            specialUsageTree = null;
            usageRows = List.of();
            usageScroll = 0;
            collapsedItems.clear();
            collapsedSlots.clear();
            collapsedModules.clear();
        }
        rebuildContent();
    }

    public void open() {
        if ((profile != null || specialOnly) && !isVisible()) {
            closing = false;
            setVisible(true);
            rebuildContent();
            onVisibilityChanged.accept(true);
            animateOpen();
        }
    }

    public void toggle() {
        if (closing || isAnimating()) {
            return;
        }
        if (isVisible()) {
            close();
        } else {
            open();
        }
    }

    public void close() {
        if (!isVisible() || closing) {
            return;
        }
        closing = true;
        stopPageAnimation();
        resetPanelTransform();
        panelAnimation = new KeyframeAnimation(CLOSE_DURATION, this)
                .applyTo(
                        new Applier.Opacity(1f, 0f),
                        new Applier.TranslateY(panelY, panelY + 2f))
                .onStop(completed -> {
                    closing = false;
                    if (completed) {
                        finishClose();
                    } else {
                        resetPanelTransform();
                    }
                });
        panelAnimation.start();
    }

    public void closeImmediately() {
        boolean wasVisible = isVisible();
        stopPanelAnimation();
        stopPageAnimation();
        closing = false;
        setVisible(false);
        resetPanelTransform();
        resetPageContentTransform();
        MaterialDossierSession.cancelAutoOpen();
        if (wasVisible) {
            onVisibilityChanged.accept(false);
        }
    }

    public void reset() {
        closeImmediately();
        profile = null;
        specialStack = ItemStack.EMPTY;
        specialOnly = false;
        usageTree = null;
        specialUsageTree = null;
        materialLines = List.of();
        usageRows = List.of();
        usageResolved = false;
        pageType = Page.MATERIAL;
        usageScroll = 0;
        collapsedItems.clear();
        collapsedSlots.clear();
        collapsedModules.clear();
        clearStats();
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int button) {
        if (!isVisible()) {
            return false;
        }
        if (isAnimating()) {
            return true;
        }
        super.onMouseClick(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (!isVisible()) {
            return false;
        }
        if (isAnimating()) {
            return true;
        }
        if (pageType == Page.USAGE && amount != 0) {
            if (statsGui.isVisible() && statsGui.hasFocus()
                    && statsGui.onMouseScroll(mouseX, mouseY, amount)) {
                return true;
            }
            scrollUsage(amount < 0 ? SCROLL_STEP : -SCROLL_STEP);
        }
        return true;
    }

    private void moveDefinition(int offset) {
        MaterialDossierSession.move(offset).ifPresent(next ->
                animateContentSwap(Integer.signum(offset), () ->
                        ((HoloMaterialNavigationAccess) HoloGui.getInstance())
                                .tetraInsight$navigateMaterial(next.materialKey())));
    }

    private void setPageType(Page pageType) {
        if (this.pageType == pageType || pageTransitioning) {
            return;
        }
        int direction = Integer.compare(pageType.ordinal(), this.pageType.ordinal());
        animateContentSwap(direction, () -> {
            this.pageType = pageType;
            rebuildContent();
        });
    }

    private void rebuildContent() {
        if (!specialOnly && profile == null) {
            materialLines = List.of();
            usageTree = null;
            specialUsageTree = null;
            usageRows = List.of();
            refreshContent();
            return;
        }

        if (specialOnly) {
            pageType = Page.USAGE;
        }
        if (pageType == Page.MATERIAL) {
            materialLines = wrap(materialLines(profile));
        } else if (!usageResolved) {
            usageTree = specialOnly
                    ? new MaterialUsageTreeSnapshot(List.of())
                    : MaterialUsageHierarchyResolver.resolve(profile);
            specialUsageTree = MaterialUsageHierarchyResolver.resolveSpecial(
                    specialOnly ? specialStack : MaterialDossierSession.sourceStack());
            usageResolved = true;
            collapseAllBranchesByDefault();
            rebuildUsageRows();
        }
        materialTab.setActive(pageType == Page.MATERIAL);
        usageTab.setActive(pageType == Page.USAGE);
        refreshContent();
    }

    private static List<Component> materialLines(MaterialProfileSnapshot profile) {
        List<Component> result = new ArrayList<>();
        result.add(Component.translatable(
                        "tetra_insight.material.dossier.category",
                        MaterialInsightText.categoryName(profile),
                        MaterialInsightIndex.usageCount(profile.materialKey()))
                .withStyle(ChatFormatting.WHITE));
        result.add(MaterialInsightText.tendency(profile));
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.axes",
                        MaterialInsightText.number(profile.primary()),
                        MaterialInsightText.number(profile.secondary()),
                        MaterialInsightText.number(profile.tertiary()))
                .withStyle(ChatFormatting.GRAY));
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.capacity",
                        MaterialInsightText.number(profile.durability()),
                        MaterialInsightText.number(profile.integrityGain()),
                        MaterialInsightText.number(profile.integrityCost()),
                        profile.magicCapacity())
                .withStyle(ChatFormatting.GRAY));
        result.add(Component.translatable(
                        "tetra_insight.material.dossier.tools",
                        profile.toolLevel(),
                        MaterialInsightText.number(profile.toolEfficiency()))
                .withStyle(ChatFormatting.GRAY));
        result.add(Component.translatable(
                        "tetra_insight.material.tooltip.intrinsic",
                        profile.attributeCount(),
                        profile.effectCount(),
                        profile.aspectCount(),
                        profile.featureCount(),
                        profile.improvementCount())
                .withStyle(ChatFormatting.DARK_GRAY));
        if (profile.hiddenInGlobalMaterialBrowser()) {
            result.add(Component.translatable("tetra_insight.material.dossier.hidden")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return List.copyOf(result);
    }

    private void rebuildUsageRows() {
        boolean hasRegular = usageTree != null && !usageTree.items().isEmpty();
        boolean hasSpecial = specialUsageTree != null && !specialUsageTree.items().isEmpty();
        if (!hasRegular && !hasSpecial) {
            clearStats();
            usageRows = List.of(UsageRow.message(Component.translatable(
                    "tetra_insight.material.dossier.no_usages").getString()));
            usageScroll = 0;
            refreshVisibleUsageRows();
            return;
        }

        List<UsageRow> rows = new ArrayList<>();
        if (hasSpecial && hasRegular) {
            rows.add(UsageRow.section(Component.translatable(
                    "tetra_insight.material.dossier.section.regular").getString(), false));
        }
        if (hasRegular) {
            appendUsageTreeRows(rows, usageTree, "regular|");
        }
        if (hasSpecial) {
            rows.add(UsageRow.section(Component.translatable(
                    "tetra_insight.material.dossier.section.special").getString(), true));
            appendUsageTreeRows(rows, specialUsageTree, "special|");
        }
        usageRows = List.copyOf(rows);
        rows.stream()
                .filter(row -> row.statPreview() != null)
                .findFirst()
                .ifPresentOrElse(this::showStats, this::clearStats);
        usageScroll = Math.min(usageScroll, maxUsageScroll());
        refreshVisibleUsageRows();
    }

    private void appendUsageTreeRows(
            List<UsageRow> rows,
            MaterialUsageTreeSnapshot tree,
            String keyPrefix
    ) {
        for (MaterialItemUsageSnapshot item : tree.items()) {
            String itemKey = keyPrefix + item.itemId();
            boolean itemCollapsed = collapsedItems.contains(itemKey);
            rows.add(UsageRow.item(item, itemKey, itemCollapsed));
            if (itemCollapsed) {
                continue;
            }

            Map<String, List<MaterialModuleUsageSnapshot>> slots = new LinkedHashMap<>();
            for (MaterialModuleUsageSnapshot module : item.modules()) {
                slots.computeIfAbsent(module.navigation().slot(), ignored -> new ArrayList<>())
                        .add(module);
            }
            for (Map.Entry<String, List<MaterialModuleUsageSnapshot>> entry : slots.entrySet()) {
                List<MaterialModuleUsageSnapshot> modules = entry.getValue();
                String slotKey = itemKey + "|" + entry.getKey();
                boolean slotCollapsed = collapsedSlots.contains(slotKey);
                rows.add(UsageRow.slot(modules.get(0).slotName(), slotKey, slotCollapsed));
                if (slotCollapsed) {
                    continue;
                }

                for (MaterialModuleUsageSnapshot module : modules) {
                    String moduleKey = slotKey + "|" + module.navigation().schematicKey();
                    boolean hasImprovements = !module.improvements().isEmpty();
                    boolean moduleCollapsed = hasImprovements && collapsedModules.contains(moduleKey);
                    rows.add(UsageRow.module(
                            module, moduleKey, moduleCollapsed, hasImprovements));
                    if (moduleCollapsed) {
                        continue;
                    }
                    for (MaterialImprovementUsageSnapshot improvement : module.improvements()) {
                        rows.add(UsageRow.improvement(improvement));
                    }
                }
            }
        }
    }

    private List<FormattedCharSequence> wrap(List<Component> components) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> result = new ArrayList<>();
        for (Component component : components) {
            if (component.getString().isEmpty()) {
                result.add(FormattedCharSequence.EMPTY);
            } else {
                result.addAll(font.split(component, contentWidth));
            }
        }
        if (result.isEmpty()) {
            result.add(FormattedCharSequence.EMPTY);
        }
        return List.copyOf(result);
    }

    private void scrollUsage(int offset) {
        int next = Math.max(0, Math.min(maxUsageScroll(), usageScroll + offset));
        if (next != usageScroll) {
            usageScroll = next;
            refreshVisibleUsageRows();
        }
    }

    private int maxUsageScroll() {
        return Math.max(0, usageRows.size() - visibleUsageRows);
    }

    private void toggleBranch(UsageRow row) {
        Set<String> target = switch (row.type()) {
            case ITEM -> collapsedItems;
            case SLOT -> collapsedSlots;
            case MODULE -> collapsedModules;
            default -> null;
        };
        if (target == null || !row.hasChildren()) {
            return;
        }
        if (!target.add(row.collapseKey())) {
            target.remove(row.collapseKey());
        }
        rebuildUsageRows();
    }

    private void setAllBranchesCollapsed(boolean collapsed) {
        collapsedItems.clear();
        collapsedSlots.clear();
        collapsedModules.clear();
        if (collapsed) {
            collectCollapsedBranches(usageTree, "regular|");
            collectCollapsedBranches(specialUsageTree, "special|");
        }
        usageScroll = 0;
        rebuildUsageRows();
    }

    private void collapseAllBranchesByDefault() {
        collapsedItems.clear();
        collapsedSlots.clear();
        collapsedModules.clear();
        collectCollapsedBranches(usageTree, "regular|");
        collectCollapsedBranches(specialUsageTree, "special|");
        usageScroll = 0;
    }

    private void collectCollapsedBranches(
            MaterialUsageTreeSnapshot tree,
            String keyPrefix
    ) {
        if (tree == null) {
            return;
        }
        for (MaterialItemUsageSnapshot item : tree.items()) {
            String itemKey = keyPrefix + item.itemId();
            collapsedItems.add(itemKey);
            for (MaterialModuleUsageSnapshot module : item.modules()) {
                String slotKey = itemKey + "|" + module.navigation().slot();
                collapsedSlots.add(slotKey);
                if (!module.improvements().isEmpty()) {
                    collapsedModules.add(slotKey + "|"
                            + module.navigation().schematicKey());
                }
            }
        }
    }

    private void refreshContent() {
        boolean usagePage = pageType == Page.USAGE;
        definitionControls.setVisible(!specialOnly);
        materialTab.setVisible(!specialOnly);
        usageTab.setVisible(true);
        collapseAllButton.setVisible(usagePage);
        expandAllButton.setVisible(usagePage);
        materialContent.setVisible(pageType == Page.MATERIAL);
        usageContent.setVisible(usagePage);
        refreshStatsState();
        if (pageType == Page.MATERIAL) {
            materialContent.update(materialLines);
        } else {
            refreshVisibleUsageRows();
        }
        updateScrollBar();
    }

    private void refreshVisibleUsageRows() {
        if (usageContent == null) {
            return;
        }
        usageContent.clearChildren();
        int to = Math.min(usageRows.size(), usageScroll + visibleUsageRows);
        for (int index = usageScroll; index < to; index++) {
            usageContent.addChild(createUsageRow(
                    usageRows.get(index), (index - usageScroll) * ROW_HEIGHT));
        }
        updateScrollBar();
    }

    private void updateScrollBar() {
        if (scrollTrack == null || scrollThumb == null) {
            return;
        }
        boolean visible = pageType == Page.USAGE && usageRows.size() > visibleUsageRows;
        scrollTrack.setVisible(visible);
        scrollThumb.setVisible(visible);
        if (!visible) {
            return;
        }
        int thumbHeight = Math.max(10,
                usageListHeight * visibleUsageRows / usageRows.size());
        int travel = usageListHeight - thumbHeight;
        int thumbY = HEADER_HEIGHT + (maxUsageScroll() == 0
                ? 0
                : travel * usageScroll / maxUsageScroll());
        scrollThumb.setY(thumbY);
        scrollThumb.setHeight(thumbHeight);
    }

    private static boolean sameSpecialStack(ItemStack left, ItemStack right) {
        return left != null
                && right != null
                && !left.isEmpty()
                && !right.isEmpty()
                && ItemStack.isSameItemSameTags(left, right);
    }

    private GuiElement createUsageRow(UsageRow row, int y) {
        return switch (row.type()) {
            case ITEM -> new UsageItemRowGui(
                    0, y, contentWidth, row, () -> toggleBranch(row));
            case SLOT -> new UsageSlotRowGui(
                    0, y, contentWidth, row, () -> toggleBranch(row));
            case MODULE -> new UsageModuleRowGui(
                    0, y, contentWidth, row, () -> toggleBranch(row), this::showStats);
            case IMPROVEMENT -> new UsageLinkRowGui(
                    IMPROVEMENT_LINK_X, y + 1,
                    contentWidth - IMPROVEMENT_LINK_X, row,
                    "tetra_insight.material.dossier.node.improvement",
                    this::showStats);
            case SECTION, SPECIAL_SECTION -> new UsageSectionRowGui(
                    0, y, contentWidth, row);
            case MESSAGE -> new UsageMessageRowGui(0, y, contentWidth, row.name());
        };
    }

    private void addCornerStrokes() {
        addChild(new GuiRect(1, 1, 6, 1, 0xffffff));
        addChild((GuiRect) new GuiRect(-1, 1, 6, 1, 0xffffff)
                .setAttachment(GuiAttachment.topRight));
        addChild((GuiRect) new GuiRect(-1, -1, 6, 1, 0xffffff)
                .setAttachment(GuiAttachment.bottomRight));
        addChild((GuiRect) new GuiRect(1, -1, 6, 1, 0xffffff)
                .setAttachment(GuiAttachment.bottomLeft));
    }

    private void showStats(UsageRow row) {
        MaterialStatPreviewSnapshot preview = row.statPreview();
        if (preview == null || Minecraft.getInstance().player == null) {
            return;
        }
        statsTitle.setString(Component.translatable(
                "tetra_insight.material.dossier.stats", row.name()).getString());
        statsAvailable = true;
        statsGui.update(
                preview.currentStack(),
                preview.previewStack(),
                null,
                null,
                Minecraft.getInstance().player);
        ((HoloStatsComparisonAccess) statsGui)
                .tetraInsight$setComparisonMode(ImprovementComparisonMode.NONE);
        refreshStatsState();
    }

    private void clearStats() {
        if (statsTitle != null) {
            statsAvailable = false;
            statsTitle.setString(Component.translatable(
                    "tetra_insight.material.dossier.stats_idle").getString());
        }
        if (statsGui != null) {
            statsGui.setVisible(false);
        }
        refreshStatsState();
    }

    private void animateOpen() {
        stopPanelAnimation();
        resetPanelTransform();
        panelAnimation = new KeyframeAnimation(OPEN_DURATION, this)
                .applyTo(
                        new Applier.Opacity(0f, 1f),
                        new Applier.TranslateY(panelY + 2f, panelY))
                .withDelay(OPEN_DELAY)
                .onStop(completed -> {
                    if (completed) {
                        resetPanelTransform();
                    }
                });
        panelAnimation.start();
    }

    private void animateContentSwap(int direction, Runnable swap) {
        if (pageTransitioning) {
            return;
        }
        pageTransitioning = true;
        resetPageContentTransform();
        int exitOffset = direction == 0 ? 0 : -direction * 2;
        pageAnimation = new KeyframeAnimation(PAGE_EXIT_DURATION, pageContent)
                .applyTo(
                        new Applier.Opacity(1f, 0f),
                        new Applier.TranslateX(0f, exitOffset))
                .onStop(completed -> {
                    if (!completed) {
                        pageTransitioning = false;
                        resetPageContentTransform();
                        return;
                    }
                    swap.run();
                    int enterOffset = direction == 0 ? 0 : direction * 2;
                    pageAnimation = new KeyframeAnimation(PAGE_ENTER_DURATION, pageContent)
                            .applyTo(
                                    new Applier.Opacity(0f, 1f),
                                    new Applier.TranslateX(enterOffset, 0f))
                            .onStop(entered -> {
                                pageTransitioning = false;
                                resetPageContentTransform();
                            });
                    pageAnimation.start();
                });
        pageAnimation.start();
    }

    private void finishClose() {
        setVisible(false);
        resetPanelTransform();
        MaterialDossierSession.cancelAutoOpen();
        onVisibilityChanged.accept(false);
    }

    private boolean isAnimating() {
        return (panelAnimation != null && panelAnimation.isActive()) || pageTransitioning;
    }

    private void stopPanelAnimation() {
        if (panelAnimation != null && panelAnimation.isActive()) {
            panelAnimation.stop();
        }
        panelAnimation = null;
    }

    private void stopPageAnimation() {
        if (pageAnimation != null && pageAnimation.isActive()) {
            pageAnimation.stop();
        }
        pageAnimation = null;
        pageTransitioning = false;
        resetPageContentTransform();
    }

    private void resetPanelTransform() {
        setX(panelX);
        setY(panelY);
        setOpacity(1f);
    }

    private void resetPageContentTransform() {
        pageContent.setX(0);
        pageContent.setOpacity(1f);
    }

    private void refreshStatsState() {
        if (statsTitle == null || statsHint == null || statsDivider == null
                || statsGui == null) {
            return;
        }
        boolean usagePage = pageType == Page.USAGE;
        boolean showArea = usagePage && (statsAvailable || hasStatCandidates());
        if (!statsAvailable) {
            statsTitle.setString(Component.translatable(
                    "tetra_insight.material.dossier.stats_idle").getString());
        }
        statsDivider.setVisible(showArea);
        statsTitle.setVisible(showArea);
        statsHint.setVisible(showArea && !statsAvailable);
        statsGui.setVisible(usagePage && statsAvailable);
    }

    private boolean hasStatCandidates() {
        return hasStatCandidates(usageTree) || hasStatCandidates(specialUsageTree);
    }

    private static boolean hasStatCandidates(MaterialUsageTreeSnapshot tree) {
        if (tree == null) {
            return false;
        }
        for (MaterialItemUsageSnapshot item : tree.items()) {
            for (MaterialModuleUsageSnapshot module : item.modules()) {
                if (module.statPreview() != null) {
                    return true;
                }
                for (MaterialImprovementUsageSnapshot improvement : module.improvements()) {
                    if (improvement.statPreview() != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private enum Page {
        MATERIAL,
        USAGE
    }

    private enum UsageRowType {
        ITEM,
        SLOT,
        MODULE,
        IMPROVEMENT,
        SECTION,
        SPECIAL_SECTION,
        MESSAGE
    }

    private record UsageRow(
            UsageRowType type,
            String name,
            ItemStack itemStack,
            MaterialUsageNavigationSnapshot navigation,
            MaterialStatPreviewSnapshot statPreview,
            String collapseKey,
            boolean collapsed,
            boolean hasChildren
    ) {
        private static UsageRow item(
                MaterialItemUsageSnapshot item,
                String collapseKey,
                boolean collapsed
        ) {
            return new UsageRow(
                    UsageRowType.ITEM, item.name(), item.itemStack(), null,
                    null, collapseKey, collapsed, true);
        }

        private static UsageRow module(
                MaterialModuleUsageSnapshot module,
                String collapseKey,
                boolean collapsed,
                boolean hasChildren
        ) {
            return new UsageRow(
                    UsageRowType.MODULE, module.name(), ItemStack.EMPTY,
                    module.navigation(), module.statPreview(),
                    collapseKey, collapsed, hasChildren);
        }

        private static UsageRow slot(String slotName, String collapseKey, boolean collapsed) {
            return new UsageRow(
                    UsageRowType.SLOT, slotName.strip(), ItemStack.EMPTY, null,
                    null, collapseKey, collapsed, true);
        }

        private static UsageRow improvement(MaterialImprovementUsageSnapshot improvement) {
            return new UsageRow(
                    UsageRowType.IMPROVEMENT, improvement.name(), ItemStack.EMPTY,
                    improvement.navigation(), improvement.statPreview(),
                    "", false, false);
        }

        private static UsageRow message(String message) {
            return new UsageRow(
                    UsageRowType.MESSAGE, message, ItemStack.EMPTY, null,
                    null, "", false, false);
        }

        private static UsageRow section(String name, boolean special) {
            return new UsageRow(
                    special ? UsageRowType.SPECIAL_SECTION : UsageRowType.SECTION,
                    name, ItemStack.EMPTY, null, null, "", false, false);
        }
    }

    private static final class ComponentLinesGui extends GuiElement {
        private List<FormattedCharSequence> lines = List.of();

        private ComponentLinesGui(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        private void update(List<FormattedCharSequence> lines) {
            this.lines = List.copyOf(lines);
        }

        @Override
        protected void drawChildren(
                GuiGraphics graphics,
                int x,
                int y,
                int mouseX,
                int mouseY,
                int guiLeft,
                int guiTop,
                float partialTicks
        ) {
            Font font = Minecraft.getInstance().font;
            for (int index = 0; index < lines.size(); index++) {
                graphics.drawString(font, lines.get(index), x, y + index * 9, 0xffffff, true);
            }
            super.drawChildren(graphics, x, y, mouseX, mouseY, guiLeft, guiTop, partialTicks);
        }
    }

    private static final class UsageItemRowGui extends GuiClickable {
        private final GuiStringOutline fold;
        private final GuiStringOutline label;

        private UsageItemRowGui(
                int x,
                int y,
                int width,
                UsageRow row,
                Runnable onToggle
        ) {
            super(x, y, width, ROW_HEIGHT, onToggle);
            addChild(new GuiItem(0, 0)
                    .setItem(row.itemStack())
                    .setTooltip(true)
                    .setCountVisibility(GuiItem.CountMode.never));
            fold = new GuiStringOutline(
                    ITEM_FOLD_X, 4, row.collapsed() ? "[+]" : "[-]");
            label = new GuiStringOutline(ITEM_LABEL_X, 4,
                    Minecraft.getInstance().font.plainSubstrByWidth(
                            row.name(), width - ITEM_LABEL_X - 2));
            fold.setColor(COLOR_SECONDARY);
            label.setColor(COLOR_PRIMARY);
            addChild(fold);
            addChild(label);
        }

        @Override
        protected void onFocus() {
            fold.setColor(COLOR_PRIMARY);
            label.setColor(COLOR_PRIMARY);
        }

        @Override
        protected void onBlur() {
            fold.setColor(COLOR_SECONDARY);
            label.setColor(COLOR_PRIMARY);
        }
    }

    private static final class UsageSlotRowGui extends GuiClickable {
        private final GuiStringOutline fold;
        private final GuiStringOutline label;

        private UsageSlotRowGui(
                int x,
                int y,
                int width,
                UsageRow row,
                Runnable onToggle
        ) {
            super(x, y, width, ROW_HEIGHT, onToggle);
            fold = new GuiStringOutline(
                    SLOT_FOLD_X, 4, row.collapsed() ? "[+]" : "[-]");
            label = new GuiStringOutline(SLOT_LABEL_X, 4,
                    Minecraft.getInstance().font.plainSubstrByWidth(
                            row.name(), width - SLOT_LABEL_X - 2));
            fold.setColor(COLOR_MUTED);
            label.setColor(COLOR_SECONDARY);
            addChild(fold);
            addChild(label);
        }

        @Override
        protected void onFocus() {
            fold.setColor(COLOR_PRIMARY);
            label.setColor(COLOR_PRIMARY);
        }

        @Override
        protected void onBlur() {
            fold.setColor(COLOR_MUTED);
            label.setColor(COLOR_SECONDARY);
        }
    }

    private static final class UsageModuleRowGui extends GuiElement {
        private UsageModuleRowGui(
                int x,
                int y,
                int width,
                UsageRow row,
                Runnable onToggle,
                Consumer<UsageRow> onStatsFocus
        ) {
            super(x, y, width, ROW_HEIGHT);
            if (row.hasChildren()) {
                addChild(new TextButton(
                        MODULE_FOLD_X, 4, row.collapsed() ? "+" : "-", onToggle));
            } else {
                GuiStringOutline branchEnd = new GuiStringOutline(
                        MODULE_FOLD_X + 3, 4, "·");
                branchEnd.setColor(COLOR_MUTED);
                addChild(branchEnd);
            }
            addChild(new UsageLinkRowGui(
                    MODULE_LINK_X, 1, width - MODULE_LINK_X, row,
                    "tetra_insight.material.dossier.node.module",
                    onStatsFocus));
        }
    }

    private static final class UsageSectionRowGui extends GuiElement {
        private UsageSectionRowGui(int x, int y, int width, UsageRow row) {
            super(x, y, width, ROW_HEIGHT);
            GuiStringOutline label = new GuiStringOutline(0, 4,
                    Minecraft.getInstance().font.plainSubstrByWidth(
                            row.name(), width - 2));
            label.setColor(row.type() == UsageRowType.SPECIAL_SECTION
                    ? COLOR_PRIMARY
                    : COLOR_SECONDARY);
            addChild(label);
            addChild(new GuiRect(0, ROW_HEIGHT - 2, width, 1,
                    row.type() == UsageRowType.SPECIAL_SECTION
                            ? COLOR_DIVIDER
                            : 0x333333));
        }
    }

    private static final class UsageMessageRowGui extends GuiElement {
        private UsageMessageRowGui(int x, int y, int width, String message) {
            super(x, y, width, ROW_HEIGHT);
            GuiStringOutline label = new GuiStringOutline(0, 4, message);
            label.setColor(COLOR_MUTED);
            addChild(label);
        }
    }

    private static final class UsageLinkRowGui extends HoloSchematicListItemGui {
        private final UsageRow row;
        private final String typeKey;
        private final Consumer<UsageRow> onStatsFocus;

        private UsageLinkRowGui(
                int x,
                int y,
                int width,
                UsageRow row,
                String typeKey,
                Consumer<UsageRow> onStatsFocus
        ) {
            super(x, y, width, schematic(row),
                    () -> MaterialUsageNavigator.open(row.navigation()));
            this.row = row;
            this.typeKey = typeKey;
            this.onStatsFocus = onStatsFocus;
        }

        @Override
        protected void onFocus() {
            super.onFocus();
            if (row.statPreview() != null) {
                onStatsFocus.accept(row);
            }
        }

        @Override
        protected void onBlur() {
            super.onBlur();
        }

        @Override
        public List<Component> getTooltipLines() {
            if (!hasFocus()) {
                return null;
            }
            return List.of(
                    Component.literal(row.name()),
                    Component.translatable(typeKey).withStyle(ChatFormatting.GRAY),
                    Component.translatable("tetra_insight.material.dossier.jump")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }

        private static UpgradeSchematic schematic(UsageRow row) {
            UpgradeSchematic schematic = SchematicRegistry.getSchematic(
                    row.navigation().schematicKey());
            if (schematic == null) {
                throw new IllegalStateException(
                        "Missing Tetra schematic for material usage: "
                                + row.navigation().schematicKey());
            }
            return schematic;
        }
    }

    private static class TextButton extends GuiClickable {
        private final GuiStringOutline label;
        private final Component tooltip;

        private TextButton(int x, int y, String text, Runnable onClick) {
            this(x, y, text, onClick, null);
        }

        private TextButton(
                int x,
                int y,
                String text,
                Runnable onClick,
                Component tooltip
        ) {
            super(x, y, Math.max(9, Minecraft.getInstance().font.width(text) + 2), 9, onClick);
            this.tooltip = tooltip;
            label = new GuiStringOutline(1, 0, text);
            label.setColor(COLOR_SECONDARY);
            addChild(label);
        }

        @Override
        public List<Component> getTooltipLines() {
            return hasFocus() && tooltip != null ? List.of(tooltip) : null;
        }

        @Override
        protected void onFocus() {
            label.setColor(COLOR_PRIMARY);
        }

        @Override
        protected void onBlur() {
            label.setColor(COLOR_SECONDARY);
        }
    }

    private static final class TabButton extends TextButton {
        private boolean active;

        private TabButton(int x, int y, String translationKey, Runnable onClick) {
            super(x, y, Component.translatable(translationKey).getString(), onClick);
        }

        private void setActive(boolean active) {
            this.active = active;
            super.label.setColor(active ? COLOR_PRIMARY : COLOR_SECONDARY);
        }

        @Override
        protected void onBlur() {
            super.label.setColor(active ? COLOR_PRIMARY : COLOR_SECONDARY);
        }
    }
}
