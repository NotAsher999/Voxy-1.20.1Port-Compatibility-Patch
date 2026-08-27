package dev.notasher.voxycompat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(VoxyCompatibilityPatch.MOD_ID)
public final class VoxyCompatibilityPatch {
    public static final String MOD_ID = "voxy_compat_patch";
    public static final Logger LOGGER = LogUtils.getLogger();
}
