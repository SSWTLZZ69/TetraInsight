package io.github.createdelight.tetrainsight.integration.tetrawear;

import io.github.createdelight.tetrainsight.integration.tetra.effect.EffectApplicabilityDefinition;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Optional Tetrawear 1.0.0 adapter without a compile-time dependency. */
public final class TetrawearEffectAdapter {
    private static final String ARMOR_CLASS = "se.mickelus.tetrawear.item.ModularArmor";
    private static final String STACK_ARMOR = "tetra_insight.effect.stacking.armor_sum";
    private static final String EVIDENCE = "tetra_insight.effect.evidence.tetrawear_1_0";

    private static final Map<String, List<EffectApplicabilityDefinition>> DEFINITIONS = new LinkedHashMap<>();

    static {
        add(EffectTrigger.WEAR_PASSIVE,
                "dampenFall", "harvestSpeed", "webWalker", "snowWalker", "magmaWalker",
                "insulating", "elytra", "dazzling");
        add(EffectTrigger.KILL_ENTITY, "lifesteal");
        add(EffectTrigger.RECEIVE_HIT,
                "skeletalSway", "shadowSway", "webbingArmor", "witheringArmor");
        add(EffectTrigger.DODGE, "skitter", "evade", "inertia", "shadowstep");
        add(EffectTrigger.DODGE_FORWARD, "charge");
        add(EffectTrigger.DODGE_BACKWARD, "retreatingShot");
        add("waterBreathing", new EffectApplicabilityDefinition(
                List.of(EffectScope.HELMET), List.of(EffectTrigger.WEAR_PASSIVE),
                "tetra_insight.effect.stacking.single_piece", EVIDENCE));
    }

    private TetrawearEffectAdapter() {
    }

    public static List<EffectApplicabilityDefinition> resolve(String effectKey) {
        return isLoaded()
                ? List.copyOf(DEFINITIONS.getOrDefault(effectKey, List.of()))
                : List.of();
    }

    public static boolean isArmorStack(ItemStack stack) {
        return isLoaded() && !stack.isEmpty()
                && isInstance(stack.getItem().getClass(), ARMOR_CLASS);
    }

    public static boolean matchesArmorScope(ItemStack stack, List<EffectScope> scopes) {
        if (!isArmorStack(stack)) {
            return false;
        }
        if (scopes.contains(EffectScope.HELMET)) {
            return stack.getItem() instanceof ArmorItem armorItem
                    && armorItem.getType() == ArmorItem.Type.HELMET;
        }
        return scopes.contains(EffectScope.ARMOR);
    }

    public static boolean hasDefinition(String effectKey) {
        return DEFINITIONS.containsKey(effectKey);
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("tetrawear");
    }

    private static boolean isInstance(Class<?> type, String targetName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (targetName.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void add(EffectTrigger trigger, String... effectKeys) {
        for (String effectKey : effectKeys) {
            add(effectKey, new EffectApplicabilityDefinition(
                    List.of(EffectScope.ARMOR), List.of(trigger), STACK_ARMOR, EVIDENCE));
        }
    }

    private static void add(String effectKey, EffectApplicabilityDefinition definition) {
        DEFINITIONS.computeIfAbsent(effectKey, ignored -> new ArrayList<>()).add(definition);
    }
}
