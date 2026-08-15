package io.github.createdelight.tetrainsight.integration.tetra.effect;

import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilitySnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilityPathSnapshot;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectApplicabilityState;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectScope;
import io.github.createdelight.tetrainsight.integration.tetra.model.EffectTrigger;
import io.github.createdelight.tetrainsight.integration.tetrawear.TetrawearEffectAdapter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.ItemModularHandheld;
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;
import se.mickelus.tetra.items.modular.impl.crossbow.AbstractModularCrossbowItem;
import se.mickelus.tetra.items.modular.impl.shield.ModularShieldItem;
import se.mickelus.tetra.items.modular.impl.toolbelt.ModularToolbeltItem;

import java.util.List;

public final class EffectApplicabilityResolver {
    private static final String EVIDENCE_UNKNOWN = "tetra_insight.effect.evidence.unknown";
    private static final String STACK_UNKNOWN = "tetra_insight.effect.stacking.unknown";

    private EffectApplicabilityResolver() {
    }

    public static EffectApplicabilitySnapshot resolve(ItemEffect effect,
            ItemStack currentStack, ItemStack previewStack) {
        ItemStack displayStack = previewStack.isEmpty() ? currentStack : previewStack;
        List<EffectApplicabilityDefinition> builtInDefinitions = EffectApplicabilityDefinition.merge(
                TetraEffectScopeIndex.resolve(effect),
                TetrawearEffectAdapter.resolve(effect.getKey()));
        EffectApplicabilityRuleSet resourceRule = EffectApplicabilityResourceIndex.resolve(
                effect.getKey());
        List<EffectApplicabilityDefinition> definitions = resourceRule == null
                ? builtInDefinitions
                : resourceRule.replace()
                        ? resourceRule.definitions()
                        : EffectApplicabilityDefinition.merge(
                                builtInDefinitions, resourceRule.definitions());
        if (definitions.isEmpty()) {
            definitions = List.of(unknownDefinition());
        }

        List<EffectApplicabilityPathSnapshot> paths = definitions.stream()
                .map(definition -> new EffectApplicabilityPathSnapshot(
                        definition.scopes(),
                        definition.triggers(),
                        resolveState(effect, displayStack, definition),
                        definition.stackingTranslationKey(),
                        definition.evidenceTranslationKey()))
                .toList();

        return new EffectApplicabilitySnapshot(
                effect.getKey(),
                paths,
                aggregateState(paths));
    }

    private static EffectApplicabilityDefinition unknownDefinition() {
        return new EffectApplicabilityDefinition(
                List.of(EffectScope.UNKNOWN), List.of(EffectTrigger.UNKNOWN),
                STACK_UNKNOWN, EVIDENCE_UNKNOWN);
    }

    private static EffectApplicabilityState aggregateState(
            List<EffectApplicabilityPathSnapshot> paths) {
        if (paths.stream().anyMatch(path -> path.previewState() == EffectApplicabilityState.ACTIVE)) {
            return EffectApplicabilityState.ACTIVE;
        }
        if (paths.stream().anyMatch(path -> path.previewState()
                == EffectApplicabilityState.PROVIDED_NOT_TRIGGERED)) {
            return EffectApplicabilityState.PROVIDED_NOT_TRIGGERED;
        }
        return EffectApplicabilityState.UNKNOWN;
    }

    private static EffectApplicabilityState resolveState(ItemEffect effect, ItemStack stack,
            EffectApplicabilityDefinition definition) {
        if (definition.scopes().contains(EffectScope.UNKNOWN) || stack.isEmpty()
                || !(stack.getItem() instanceof IModularItem modularItem)) {
            return EffectApplicabilityState.UNKNOWN;
        }
        if (!modularItem.getEffects(stack).contains(effect)) {
            return EffectApplicabilityState.UNKNOWN;
        }

        boolean armorScope = definition.scopes().contains(EffectScope.ARMOR)
                || definition.scopes().contains(EffectScope.HELMET);
        boolean armorStack = TetrawearEffectAdapter.isArmorStack(stack);
        if (armorScope) {
            return armorStack && TetrawearEffectAdapter.matchesArmorScope(stack, definition.scopes())
                    ? EffectApplicabilityState.ACTIVE
                    : EffectApplicabilityState.PROVIDED_NOT_TRIGGERED;
        }
        if (armorStack) {
            return EffectApplicabilityState.PROVIDED_NOT_TRIGGERED;
        }
        boolean curiosScope = definition.scopes().contains(EffectScope.CURIOS);
        boolean curiosStack = isCuriosStack(stack);
        if (curiosScope) {
            return curiosStack
                    ? EffectApplicabilityState.ACTIVE
                    : EffectApplicabilityState.PROVIDED_NOT_TRIGGERED;
        }
        if (curiosStack) {
            return EffectApplicabilityState.PROVIDED_NOT_TRIGGERED;
        }
        return matchesTetraItemScope(stack, definition.scopes())
                ? EffectApplicabilityState.ACTIVE
                : EffectApplicabilityState.PROVIDED_NOT_TRIGGERED;
    }

    private static boolean isCuriosStack(ItemStack stack) {
        return !stack.isEmpty() && implementsInterface(
                stack.getItem().getClass(),
                "top.theillusivec4.curios.api.type.capability.ICurioItem");
    }

    private static boolean implementsInterface(Class<?> type, String targetName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> interfaceType : current.getInterfaces()) {
                if (targetName.equals(interfaceType.getName())
                        || implementsInterface(interfaceType, targetName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesTetraItemScope(ItemStack stack, List<EffectScope> scopes) {
        if (scopes.stream().anyMatch(scope -> scope == EffectScope.HELD_ITEM
                || scope == EffectScope.MAIN_HAND || scope == EffectScope.OFF_HAND
                || scope == EffectScope.INVENTORY || scope == EffectScope.MODULAR_ITEM)) {
            return true;
        }
        Object item = stack.getItem();
        return scopes.stream().anyMatch(scope -> switch (scope) {
            case BOW -> item instanceof BowItem || item instanceof ModularBowItem;
            case CROSSBOW -> item instanceof CrossbowItem
                    || item instanceof AbstractModularCrossbowItem;
            case SHIELD -> item instanceof ModularShieldItem;
            case TOOLBELT -> item instanceof ModularToolbeltItem;
            case TOOL, WEAPON -> item instanceof ItemModularHandheld;
            default -> false;
        });
    }
}
