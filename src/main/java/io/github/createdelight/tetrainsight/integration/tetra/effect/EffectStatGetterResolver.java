package io.github.createdelight.tetrainsight.integration.tetra.effect;

import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterAddAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterAndAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterClampAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectEfficiencyAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterEffectLevelAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterMultiplyAccessor;
import io.github.createdelight.tetrainsight.mixin.tetra.StatGetterOrAccessor;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.gui.stats.getter.IStatGetter;
import se.mickelus.tetra.gui.stats.getter.StatGetterAdd;
import se.mickelus.tetra.gui.stats.getter.StatGetterAnd;
import se.mickelus.tetra.gui.stats.getter.StatGetterClamp;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectEfficiency;
import se.mickelus.tetra.gui.stats.getter.StatGetterEffectLevel;
import se.mickelus.tetra.gui.stats.getter.StatGetterMultiply;
import se.mickelus.tetra.gui.stats.getter.StatGetterOr;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class EffectStatGetterResolver {
    private EffectStatGetterResolver() {
    }

    public static Optional<ItemEffect> resolve(IStatGetter getter) {
        Set<ItemEffect> effects = new LinkedHashSet<>();
        collect(getter, effects);
        return effects.size() == 1
                ? Optional.of(effects.iterator().next())
                : Optional.empty();
    }

    private static void collect(IStatGetter getter, Set<ItemEffect> effects) {
        if (getter instanceof StatGetterEffectLevel
                && getter instanceof StatGetterEffectLevelAccessor accessor) {
            effects.add(accessor.tetraInsight$getEffect());
        } else if (getter instanceof StatGetterEffectEfficiency
                && getter instanceof StatGetterEffectEfficiencyAccessor accessor) {
            effects.add(accessor.tetraInsight$getEffect());
        } else if (getter instanceof StatGetterAdd
                && getter instanceof StatGetterAddAccessor accessor) {
            collect(accessor.tetraInsight$getStatGetters(), effects);
        } else if (getter instanceof StatGetterMultiply
                && getter instanceof StatGetterMultiplyAccessor accessor) {
            collect(accessor.tetraInsight$getStatGetters(), effects);
        } else if (getter instanceof StatGetterAnd
                && getter instanceof StatGetterAndAccessor accessor) {
            collect(accessor.tetraInsight$getStatGetters(), effects);
        } else if (getter instanceof StatGetterOr
                && getter instanceof StatGetterOrAccessor accessor) {
            collect(accessor.tetraInsight$getStatGetters(), effects);
        } else if (getter instanceof StatGetterClamp
                && getter instanceof StatGetterClampAccessor accessor) {
            collect(accessor.tetraInsight$getStatGetter(), effects);
        }
    }

    private static void collect(IStatGetter[] getters, Set<ItemEffect> effects) {
        for (IStatGetter getter : getters) {
            collect(getter, effects);
        }
    }
}
