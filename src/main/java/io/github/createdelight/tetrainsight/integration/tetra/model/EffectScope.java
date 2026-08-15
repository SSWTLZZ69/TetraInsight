package io.github.createdelight.tetrainsight.integration.tetra.model;

public enum EffectScope {
    HELD_ITEM("tetra_insight.effect.scope.held_item"),
    MAIN_HAND("tetra_insight.effect.scope.main_hand"),
    OFF_HAND("tetra_insight.effect.scope.off_hand"),
    ARMOR("tetra_insight.effect.scope.armor"),
    HELMET("tetra_insight.effect.scope.helmet"),
    CURIOS("tetra_insight.effect.scope.curios"),
    TOOLBELT("tetra_insight.effect.scope.toolbelt"),
    BOW("tetra_insight.effect.scope.bow"),
    CROSSBOW("tetra_insight.effect.scope.crossbow"),
    SHIELD("tetra_insight.effect.scope.shield"),
    TOOL("tetra_insight.effect.scope.tool"),
    WEAPON("tetra_insight.effect.scope.weapon"),
    INVENTORY("tetra_insight.effect.scope.inventory"),
    MODULAR_ITEM("tetra_insight.effect.scope.modular_item"),
    UNKNOWN("tetra_insight.effect.scope.unknown");

    private final String translationKey;

    EffectScope(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
