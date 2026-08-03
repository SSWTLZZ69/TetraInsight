package io.github.createdelight.tetrainsight.integration.tetra.model;

import net.minecraft.resources.ResourceLocation;
import se.mickelus.tetra.module.data.GlyphData;

public record MaterialUsageGlyphSnapshot(
        ResourceLocation textureLocation,
        int textureX,
        int textureY,
        int tint
) {
    public static MaterialUsageGlyphSnapshot from(GlyphData glyph, Integer tintOverride) {
        GlyphData source = glyph != null ? glyph : new GlyphData(0, 0);
        return new MaterialUsageGlyphSnapshot(
                source.textureLocation,
                source.textureX,
                source.textureY,
                tintOverride != null ? tintOverride : source.tint);
    }
}
