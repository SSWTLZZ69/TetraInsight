package io.github.createdelight.tetrainsight.integration.tetra.model;

public enum EffectTrigger {
    ATTACK("tetra_insight.effect.trigger.attack"),
    HIT_ENTITY("tetra_insight.effect.trigger.hit_entity"),
    RECEIVE_HIT("tetra_insight.effect.trigger.receive_hit"),
    BLOCK("tetra_insight.effect.trigger.block"),
    RIGHT_CLICK("tetra_insight.effect.trigger.right_click"),
    USE_ITEM("tetra_insight.effect.trigger.use_item"),
    BREAK_BLOCK("tetra_insight.effect.trigger.break_block"),
    MINE_BLOCK("tetra_insight.effect.trigger.mine_block"),
    DODGE("tetra_insight.effect.trigger.dodge"),
    DODGE_FORWARD("tetra_insight.effect.trigger.dodge_forward"),
    DODGE_BACKWARD("tetra_insight.effect.trigger.dodge_backward"),
    WEAR_PASSIVE("tetra_insight.effect.trigger.wear_passive"),
    KILL_ENTITY("tetra_insight.effect.trigger.kill_entity"),
    DEATH("tetra_insight.effect.trigger.death"),
    HEAL("tetra_insight.effect.trigger.heal"),
    TELEPORT("tetra_insight.effect.trigger.teleport"),
    GAIN_EXPERIENCE("tetra_insight.effect.trigger.gain_experience"),
    PROJECTILE("tetra_insight.effect.trigger.projectile"),
    THROW_ITEM("tetra_insight.effect.trigger.throw_item"),
    ABILITY("tetra_insight.effect.trigger.ability"),
    TOOLBELT_ACTION("tetra_insight.effect.trigger.toolbelt_action"),
    UNKNOWN("tetra_insight.effect.trigger.unknown");

    private final String translationKey;

    EffectTrigger(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
