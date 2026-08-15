package io.github.createdelight.tetrainsight.integration.tetra.effect;

import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import se.mickelus.tetra.data.ItemEffectStore;
import se.mickelus.tetra.effect.ItemEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Versioned runtime-consumer index for Tetra 6.17. */
public final class TetraEffectScopeIndex {
    private static final String STACK_ITEM = "tetra_insight.effect.stacking.item";
    private static final String STACK_INVENTORY_MAX = "tetra_insight.effect.stacking.inventory_max";
    private static final String EVIDENCE_TETRA = "tetra_insight.effect.evidence.tetra_6_17";
    private static final String EVIDENCE_DATA = "tetra_insight.effect.evidence.data_trigger";

    private static final Map<String, List<EffectApplicabilityDefinition>> DEFINITIONS = new LinkedHashMap<>();

    static {
        add(EffectScope.MAIN_HAND, EffectTrigger.ATTACK,
                "bleeding", "backstab", "armorPenetration", "crushing", "skewering",
                "sweeping", "truesweep", "sweepingStrike", "criticalStrike", "severing",
                "stun", "quickStrike", "softStrike", "shieldbreaker", "fierySelf",
                "combusting", "bloodbound", "ravenous", "satiating", "sculkTaint",
                "earthbind", "reaching", "janking");
        add(EffectScope.MAIN_HAND, EffectTrigger.MINE_BLOCK,
                "strikingAxe", "strikingPickaxe", "strikingCut", "strikingShovel",
                "strikingHoe", "extraction", "unboundExtraction", "piercingHarvest");
        add(EffectScope.MAIN_HAND, EffectTrigger.RIGHT_CLICK,
                "throwable", "ricochet", "piercing", "bashing", "jab");
        add(EffectScope.WEAPON, EffectTrigger.BLOCK, "blocking", "blockingReflect");
        add(EffectScope.HELD_ITEM, EffectTrigger.USE_ITEM, "unstable", "workable");
        add(EffectScope.INVENTORY, EffectTrigger.TELEPORT, "enderReverb");
        add(EffectScope.INVENTORY, EffectTrigger.GAIN_EXPERIENCE, "intuit");
        add(EffectScope.BOW, EffectTrigger.PROJECTILE,
                "releaseLatch", "flow", "overbowed", "multishot", "zoom", "spread",
                "focus", "focusEcho", "velocity", "rangeCritical");
        add(EffectScope.CROSSBOW, EffectTrigger.PROJECTILE,
                "ammoCapacity", "multishot", "velocity", "piercing");
        add(EffectScope.TOOLBELT, EffectTrigger.TOOLBELT_ACTION,
                "quickSlot", "storageSlot", "potionSlot", "quiverSlot", "quickAccess",
                "cellSocket", "suspendSelf", "suspend", "booster");
        add(EffectScope.TOOL, EffectTrigger.ABILITY,
                "execute", "lunge", "slam", "puncture", "pry", "overpower", "reap",
                "abilityDefensive", "abilityOvercharge", "abilityMomentum", "abilityCombo",
                "abilityRevenge", "abilityOverextend", "abilityExhilaration", "abilitySpeed",
                "abilityEcho");
        add(EffectScope.TOOL, EffectTrigger.MINE_BLOCK,
                "extractionMedialLimit", "extractionLateralLimit", "extractionAxialLimit",
                "extractionAxialAmplify", "extractionPlanarAmplify", "percussionScanner",
                "sweeperRange", "sweeperHorizontalSpread", "sweeperVerticalSpread");
        add(EffectScope.MAIN_HAND, EffectTrigger.ATTACK, "planarSweep", "denailing");
    }

    private TetraEffectScopeIndex() {
    }

    public static List<EffectApplicabilityDefinition> resolve(ItemEffect effect) {
        List<EffectApplicabilityDefinition> definitions = new ArrayList<>(
                DEFINITIONS.getOrDefault(effect.getKey(), List.of()));

        if (ItemEffectStore.onUseEffects.containsKey(effect)) {
            definitions.add(dataDefinition(EffectScope.HELD_ITEM, EffectTrigger.USE_ITEM));
        }
        if (ItemEffectStore.onHitEffects.containsKey(effect)) {
            definitions.add(dataDefinition(EffectScope.MAIN_HAND, EffectTrigger.HIT_ENTITY));
        }
        if (ItemEffectStore.onMineBlockEffects.containsKey(effect)) {
            definitions.add(dataDefinition(EffectScope.MAIN_HAND, EffectTrigger.MINE_BLOCK));
        }
        if (ItemEffectStore.onBreakBlockEffects.containsKey(effect)) {
            definitions.add(dataDefinition(EffectScope.MAIN_HAND, EffectTrigger.BREAK_BLOCK));
        }

        return definitions.stream().distinct().toList();
    }

    public static boolean hasHardcodedDefinition(String effectKey) {
        return DEFINITIONS.containsKey(effectKey);
    }

    private static void add(EffectScope scope, EffectTrigger trigger, String... effectKeys) {
        for (String effectKey : effectKeys) {
            String stacking = scope == EffectScope.INVENTORY ? STACK_INVENTORY_MAX : STACK_ITEM;
            DEFINITIONS.computeIfAbsent(effectKey, ignored -> new ArrayList<>())
                    .add(new EffectApplicabilityDefinition(
                            List.of(scope), List.of(trigger), stacking, EVIDENCE_TETRA));
        }
    }

    private static EffectApplicabilityDefinition dataDefinition(
            EffectScope scope, EffectTrigger trigger) {
        return new EffectApplicabilityDefinition(
                List.of(scope), List.of(trigger), STACK_ITEM, EVIDENCE_DATA);
    }
}
