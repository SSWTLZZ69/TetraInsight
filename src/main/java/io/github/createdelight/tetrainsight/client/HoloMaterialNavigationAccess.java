package io.github.createdelight.tetrainsight.client;

public interface HoloMaterialNavigationAccess {
    void tetraInsight$openMaterial(String materialKey, Runnable closeCallback);

    void tetraInsight$navigateMaterial(String materialKey);
}
