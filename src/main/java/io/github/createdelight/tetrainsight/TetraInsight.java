package io.github.createdelight.tetrainsight;

import com.mojang.logging.LogUtils;
import io.github.createdelight.tetrainsight.client.TetraInsightClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TetraInsight.MOD_ID)
public final class TetraInsight {
    public static final String MOD_ID = "tetra_insight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TetraInsight() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TetraInsightClient::init);
    }
}
