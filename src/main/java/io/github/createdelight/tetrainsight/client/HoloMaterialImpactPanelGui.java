package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialInputChannel;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialTranslationEntry;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.mutil.gui.GuiClickable;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiRect;
import se.mickelus.mutil.gui.GuiStringOutline;
import se.mickelus.tetra.gui.ZOffsetGui;

public final class HoloMaterialImpactPanelGui extends ZOffsetGui {
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 100;
    private static final int LINES_PER_PAGE = 7;

    private final GuiElement content;
    private final GuiStringOutline provenanceLabel;
    private final HoloSortPageControls pageControls;
    private final Runnable onClose;
    private List<DisplayLine> lines = List.of();
    private int page;

    public HoloMaterialImpactPanelGui(int x, int y, Runnable onClose) {
        super(x, y, 210.0D);
        this.onClose = onClose;
        setWidth(PANEL_WIDTH);
        setHeight(PANEL_HEIGHT);

        GuiRect backdrop = new GuiRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0);
        backdrop.setOpacity(0.9f);
        addChild(backdrop);
        addCornerStrokes();

        GuiStringOutline title = new GuiStringOutline(
                6,
                4,
                I18n.get("tetra_insight.holo.material_impact.title")
        );
        addChild(title);

        provenanceLabel = new GuiStringOutline(0, 4, "");
        provenanceLabel.setColor(0x7f7f7f);
        addChild(provenanceLabel);

        addChild(new PanelTextButton(
                PANEL_WIDTH - 12,
                3,
                "x",
                this::close
        ));

        content = new GuiElement(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        addChild(content);

        pageControls = new HoloSortPageControls(this::previousPage, this::nextPage);
        pageControls.setX(6);
        pageControls.setY(PANEL_HEIGHT - 12);
        addChild(pageControls);
        setVisible(false);
    }

    public void update(MaterialSchematicSnapshot snapshot) {
        lines = buildLines(snapshot);
        page = 0;
        provenanceLabel.setString(provenanceText(snapshot.displayTranslation().provenance()));
        provenanceLabel.setX(PANEL_WIDTH - 18 - provenanceLabel.getWidth());
        refreshPage();
    }

    public void toggle() {
        if (isVisible()) {
            close();
        } else {
            setVisible(true);
        }
    }

    public void close() {
        setVisible(false);
        onClose.run();
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (isVisible() && totalPages() > 1 && hasFocus() && amount != 0) {
            int previousPage = page;
            if (amount < 0) {
                nextPage();
            } else {
                previousPage();
            }
            return page != previousPage;
        }
        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    private void previousPage() {
        if (page > 0) {
            page--;
            refreshPage();
        }
    }

    private void nextPage() {
        int totalPages = totalPages();
        if (page + 1 < totalPages) {
            page++;
            refreshPage();
        }
    }

    private void refreshPage() {
        content.clearChildren();
        int from = page * LINES_PER_PAGE;
        int to = Math.min(lines.size(), from + LINES_PER_PAGE);
        for (int index = from; index < to; index++) {
            DisplayLine line = lines.get(index);
            content.addChild(new PanelLineGui(
                    7,
                    16 + (index - from) * 10,
                    PANEL_WIDTH - 14,
                    line
            ));
        }
        pageControls.update(PANEL_WIDTH - 12, page + 1, totalPages());
    }

    private int totalPages() {
        return Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
    }

    private static List<DisplayLine> buildLines(MaterialSchematicSnapshot snapshot) {
        List<DisplayLine> result = new ArrayList<>();
        if (snapshot.displayTranslation().provenance() == TranslationProvenance.NO_MATERIAL_SCALING) {
            result.add(DisplayLine.message(I18n.get(
                    "tetra_insight.holo.translation.no_scaling"
            )));
            return List.copyOf(result);
        }

        appendInput(result, snapshot, MaterialInputChannel.PRIMARY,
                "tetra.holo.craft.materials.stat.primary");
        appendInput(result, snapshot, MaterialInputChannel.SECONDARY,
                "tetra.holo.craft.materials.stat.secondary");
        appendInput(result, snapshot, MaterialInputChannel.TERTIARY,
                "tetra.holo.craft.materials.stat.tertiary");
        appendInput(result, snapshot, MaterialInputChannel.DURABILITY,
                "tetra.holo.craft.materials.stat.durability");
        appendInput(result, snapshot, MaterialInputChannel.INTEGRITY,
                "tetra.holo.craft.materials.stat.integrity");
        appendInput(result, snapshot, MaterialInputChannel.MAGIC_CAPACITY,
                "tetra.holo.craft.materials.stat.magic_capacity");
        appendInput(result, snapshot, MaterialInputChannel.TOOL,
                "tetra_insight.holo.material_impact.tool_input");
        return List.copyOf(result);
    }

    private static void appendInput(
            List<DisplayLine> target,
            MaterialSchematicSnapshot snapshot,
            MaterialInputChannel input,
            String labelKey
    ) {
        List<MaterialTranslationEntry> entries = snapshot.displayTranslation().entries().stream()
                .filter(entry -> entry.input() == input)
                .toList();
        String inputName = I18n.get(labelKey);
        if (entries.isEmpty()) {
            target.add(DisplayLine.unused(inputName));
            return;
        }

        target.add(DisplayLine.heading(inputName));
        entries.stream().map(HoloMaterialImpactPanelGui::entryLine).forEach(target::add);
    }

    private static DisplayLine entryLine(MaterialTranslationEntry entry) {
        int level = entry.generatedDisplayLevel();
        String sign = level >= 0 ? "+" : "-";
        String numeral = MaterialTranslationTooltipFormatter.toRomanNumeral(Math.abs(level));
        return new DisplayLine(
                "  " + MaterialTranslationTooltipFormatter.entryName(entry).getString(),
                0xaaaaaa,
                sign + numeral,
                level >= 0 ? 0x55ff55 : 0xff5555
        );
    }

    private static String provenanceText(TranslationProvenance provenance) {
        return I18n.get(switch (provenance) {
            case AUTHOR -> "tetra_insight.holo.material_impact.provenance.author";
            case GENERATED_FROM_EXTRACT -> "tetra_insight.holo.material_impact.provenance.generated";
            case INFERRED_FROM_PREVIEW -> "tetra_insight.holo.material_impact.provenance.inferred";
            case NO_MATERIAL_SCALING -> "tetra_insight.holo.material_impact.provenance.none";
            case UNAVAILABLE -> "tetra_insight.holo.material_impact.provenance.unavailable";
        });
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

    private record DisplayLine(String left, int leftColor, String right, int rightColor) {
        private static DisplayLine heading(String text) {
            return new DisplayLine(text, 0xffffff, "", 0xffffff);
        }

        private static DisplayLine unused(String text) {
            return new DisplayLine(
                    text,
                    0x7f7f7f,
                    I18n.get("tetra_insight.holo.material_impact.unused"),
                    0x404040
            );
        }

        private static DisplayLine message(String text) {
            return new DisplayLine(text, 0x7f7f7f, "", 0x7f7f7f);
        }
    }

    private static final class PanelLineGui extends GuiElement {
        private final GuiStringOutline left;
        private final GuiStringOutline right;

        private PanelLineGui(int x, int y, int width, DisplayLine line) {
            super(x, y, width, 9);
            right = new GuiStringOutline(0, 0, line.right());
            right.setColor(line.rightColor());
            right.setX(width - right.getWidth());
            addChild(right);

            int leftWidth = Math.max(0, width - right.getWidth() - 4);
            String leftText = Minecraft.getInstance().font.plainSubstrByWidth(line.left(), leftWidth);
            left = new GuiStringOutline(0, 0, leftText);
            left.setColor(line.leftColor());
            addChild(left);
        }
    }

    private static final class PanelTextButton extends GuiClickable {
        private final GuiStringOutline label;

        private PanelTextButton(int x, int y, String text, Runnable onClick) {
            super(x, y, 9, 9, onClick);
            label = new GuiStringOutline(1, 0, text);
            addChild(label);
        }

        @Override
        protected void onFocus() {
            label.setColor(0xffffcc);
        }

        @Override
        protected void onBlur() {
            label.setColor(0xffffff);
        }
    }
}
