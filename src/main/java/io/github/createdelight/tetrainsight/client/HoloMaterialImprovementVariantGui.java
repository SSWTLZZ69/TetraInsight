package io.github.createdelight.tetrainsight.client;

import io.github.createdelight.tetrainsight.TetraInsight;
import io.github.createdelight.tetrainsight.integration.tetra.MaterialGlyphTintResolver;
import io.github.createdelight.tetrainsight.integration.tetra.model.MaterialGlyphTintSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.loading.FMLEnvironment;
import se.mickelus.mutil.gui.GuiItem;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.tetra.blocks.workbench.gui.GuiModuleGlyph;
import se.mickelus.tetra.gui.GuiItemRolling;
import se.mickelus.tetra.gui.GuiTextures;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloImprovementVariantGui;
import se.mickelus.tetra.module.ItemModule;
import se.mickelus.tetra.module.ItemModuleMajor;
import se.mickelus.tetra.module.data.GlyphData;
import se.mickelus.tetra.module.data.ImprovementData;
import se.mickelus.tetra.module.schematic.OutcomePreview;

import java.util.function.Consumer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses the same glyph/material presentation as Tetra's module variants while
 * retaining the improvement variant's native click and hover callbacks.
 */
public class HoloMaterialImprovementVariantGui extends HoloImprovementVariantGui {
    private static final Set<String> tetraInsight$loggedMaterialGlyphs = ConcurrentHashMap.newKeySet();

    private final GuiTexture tetraInsight$backdrop;
    private final GuiItemRolling tetraInsight$material;
    private boolean tetraInsight$muted;

    public HoloMaterialImprovementVariantGui(int x, int y, String label, int labelStart,
            String schematicKey, String slot,
            OutcomePreview preview, boolean nextInSeries,
            Consumer<OutcomePreview> onVariantHover,
            Consumer<OutcomePreview> onVariantBlur,
            Consumer<OutcomePreview> onVariantSelect) {
        super(x, y, label, labelStart, preview, nextInSeries,
                onVariantHover, onVariantBlur, onVariantSelect);

        setX(x);
        clearChildren();
        setWidth(11);

        tetraInsight$backdrop = new GuiTexture(
                0, 0, 11, 11, 68, 0, GuiTextures.workbench);
        addChild(tetraInsight$backdrop);
        addChild(new GuiModuleGlyph(
                2, 2, 8, 8, tetraInsight$resolveMaterialGlyph(
                        schematicKey, preview, slot)).setShift(false));

        tetraInsight$material = new GuiItemRolling(-1, -1)
                .setCountVisibility(GuiItem.CountMode.never)
                .setItems(preview.materials);
    }

    private static GlyphData tetraInsight$resolveMaterialGlyph(
            String schematicKey, OutcomePreview preview, String slot) {
        MaterialGlyphTintSnapshot resolvedTint = MaterialGlyphTintResolver
                .resolve(schematicKey, preview)
                .orElse(null);
        if (resolvedTint != null) {
            GlyphData source = preview.glyph;
            GlyphData tinted = new GlyphData(
                    source.textureLocation,
                    source.textureX,
                    source.textureY,
                    resolvedTint.glyphTint());
            tetraInsight$logResolvedTint(schematicKey, preview, resolvedTint);
            return tinted;
        }

        if (preview.itemStack.getItem() instanceof IModularItem modularItem) {
            ItemModule module = modularItem.getModuleFromSlot(preview.itemStack, slot);
            if (module instanceof ItemModuleMajor majorModule) {
                ImprovementData improvement = majorModule.getImprovement(
                        preview.itemStack, preview.variantKey);
                if (improvement != null && improvement.glyph != null) {
                    return improvement.glyph;
                }
            }
        }
        return preview.glyph;
    }

    private static void tetraInsight$logResolvedTint(String schematicKey,
            OutcomePreview preview, MaterialGlyphTintSnapshot tint) {
        if (FMLEnvironment.production) {
            return;
        }
        String identity = schematicKey + "|" + preview.variantKey + "|" + tint.materialKey();
        if (tetraInsight$loggedMaterialGlyphs.add(identity)) {
            TetraInsight.LOGGER.info(
                    "Resolved material improvement glyph: schematic={}, variant={}, material={}, tint=#{}",
                    schematicKey, preview.variantKey, tint.materialKey(),
                    String.format("%06X", tint.glyphTint() & 0xffffff));
        }
    }

    @Override
    protected void drawChildren(GuiGraphics graphics, int x, int y,
            int mouseX, int mouseY, int guiLeft, int guiTop, float partialTicks) {
        super.drawChildren(graphics, x, y, mouseX, mouseY, guiLeft, guiTop, partialTicks);
        if (Screen.hasShiftDown()) {
            tetraInsight$material.draw(graphics,
                    x + tetraInsight$material.getX(),
                    y + tetraInsight$material.getY(),
                    mouseX, mouseY, guiLeft, guiTop, partialTicks);
        }
    }

    @Override
    protected void onFocus() {
        super.onFocus();
        tetraInsight$backdrop.setColor(0xffffcc);
    }

    @Override
    protected void onBlur() {
        super.onBlur();
        tetraInsight$backdrop.setColor(tetraInsight$muted ? 0x7f7f7f : 0xffffff);
    }

    @Override
    public void setMuted(boolean muted) {
        super.setMuted(muted);
        tetraInsight$muted = muted;
        tetraInsight$backdrop.setColor(muted ? 0x7f7f7f : 0xffffff);
    }
}
