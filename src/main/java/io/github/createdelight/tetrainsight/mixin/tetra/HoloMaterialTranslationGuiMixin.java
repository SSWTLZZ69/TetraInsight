package io.github.createdelight.tetrainsight.mixin.tetra;

import io.github.createdelight.tetrainsight.client.MaterialTranslationTooltipFormatter;
import io.github.createdelight.tetrainsight.client.MaterialTranslationTooltipAccess;
import io.github.createdelight.tetrainsight.integration.tetra.TetraDataProbe;
import io.github.createdelight.tetrainsight.integration.tetra.model.TranslationProvenance;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.schematic.HoloMaterialTranslationGui;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;
import se.mickelus.tetra.module.schematic.SchematicType;

import java.util.List;

@Mixin(value = HoloMaterialTranslationGui.class, remap = false)
public abstract class HoloMaterialTranslationGuiMixin implements MaterialTranslationTooltipAccess {
    @Shadow
    private List<Component> tooltip;

    @Unique
    private io.github.createdelight.tetrainsight.integration.tetra.model.MaterialSchematicSnapshot
            tetraInsight$schematicSnapshot;

    @Unique
    private SchematicType tetraInsight$schematicType;

    @Override
    public List<Component> tetraInsight$translationTooltip() {
        return tooltip;
    }

    @Inject(method = "update", at = @At("RETURN"), remap = false)
    private void tetraInsight$showGeneratedTranslation(UpgradeSchematic schematic, CallbackInfo ci) {
        tetraInsight$schematicSnapshot = null;
        tetraInsight$schematicType = null;
        if (schematic.getMaterialTranslation() != null) {
            return;
        }

        TetraDataProbe.findSchematic(schematic.getKey())
                .filter(snapshot -> snapshot.displayTranslation().provenance()
                        != TranslationProvenance.UNAVAILABLE)
                .ifPresent(snapshot -> {
                    tetraInsight$schematicSnapshot = snapshot;
                    tetraInsight$schematicType = schematic.getType();
                    tooltip = MaterialTranslationTooltipFormatter.format(snapshot, schematic.getType(), false);
                });
    }

    @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true, remap = false)
    private void tetraInsight$showDetailedFormula(CallbackInfoReturnable<List<Component>> cir) {
        if (tetraInsight$schematicSnapshot != null
                && ((HoloMaterialTranslationGui) (Object) this).hasFocus()) {
            cir.setReturnValue(MaterialTranslationTooltipFormatter.format(
                    tetraInsight$schematicSnapshot,
                    tetraInsight$schematicType,
                    Screen.hasShiftDown()));
        }
    }
}
