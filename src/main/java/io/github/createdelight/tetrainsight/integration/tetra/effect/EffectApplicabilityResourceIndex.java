package io.github.createdelight.tetrainsight.integration.tetra.effect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.createdelight.tetrainsight.TetraInsight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EffectApplicabilityResourceIndex {
    public static final String DIRECTORY = "tetra_insight/effect_applicability";
    private static final String JSON_SUFFIX = ".json";

    private static volatile Map<String, EffectApplicabilityRuleSet> ruleSets = Map.of();

    private EffectApplicabilityResourceIndex() {
    }

    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ReloadListener());
    }

    public static EffectApplicabilityRuleSet resolve(String effectKey) {
        return ruleSets.get(effectKey);
    }

    public static int size() {
        return ruleSets.size();
    }

    private static String effectKey(ResourceLocation resourceId) {
        String prefix = DIRECTORY + "/";
        String path = resourceId.getPath();
        String effectPath = path.substring(prefix.length(), path.length() - JSON_SUFFIX.length());
        return ResourceLocation.fromNamespaceAndPath(
                resourceId.getNamespace(), effectPath).toString();
    }

    private static final class ReloadListener extends SimplePreparableReloadListener<
            Map<String, EffectApplicabilityRuleSet>> {
        @Override
        protected Map<String, EffectApplicabilityRuleSet> prepare(
                ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, EffectApplicabilityRuleSet> prepared = new LinkedHashMap<>();
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    DIRECTORY, id -> id.getPath().endsWith(JSON_SUFFIX));
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    prepared.put(effectKey(entry.getKey()), EffectApplicabilityJsonParser.parse(root));
                } catch (Exception exception) {
                    TetraInsight.LOGGER.error(
                            "Skipping invalid effect applicability resource {}",
                            entry.getKey(), exception);
                }
            }
            return Map.copyOf(prepared);
        }

        @Override
        protected void apply(Map<String, EffectApplicabilityRuleSet> prepared,
                ResourceManager resourceManager, ProfilerFiller profiler) {
            ruleSets = prepared;
            int pathCount = prepared.values().stream()
                    .mapToInt(ruleSet -> ruleSet.definitions().size())
                    .sum();
            TetraInsight.LOGGER.info(
                    "Loaded {} manual effect applicability rules with {} paths",
                    prepared.size(), pathCount);
        }
    }
}
