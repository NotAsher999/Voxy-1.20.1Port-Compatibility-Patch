package dev.notasher.voxycompat.mixin;

import dev.notasher.voxycompat.VoxyConfigEnforcer;
import dev.notasher.voxycompat.VoxyCompatibilityPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents Voxy's settings screen from shutting down a locked-on instance. */
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.config.VoxyConfigScreenPages", priority = 900, remap = false)
public abstract class VoxyConfigScreenPagesMixin {
    @Inject(
            method = "lambda$page$0(Lme/cortex/voxy/client/config/VoxyConfig;Ljava/lang/Boolean;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void voxyCompat$keepVoxyEnabled(
            @Coerce final Object config,
            final Boolean requested,
            final CallbackInfo ci) {
        if (Boolean.FALSE.equals(requested)) {
            VoxyConfigEnforcer.rejectDisableRequest(config);
            VoxyCompatibilityPatch.LOGGER.info("Ignored request to disable locked Voxy setting enabled");
            ci.cancel();
        }
    }

    @Inject(
            method = "lambda$page$10(Lme/cortex/voxy/client/config/VoxyConfig;Ljava/lang/Boolean;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void voxyCompat$keepVoxyRenderingEnabled(
            @Coerce final Object config,
            final Boolean requested,
            final CallbackInfo ci) {
        if (Boolean.FALSE.equals(requested)) {
            VoxyConfigEnforcer.rejectDisableRequest(config);
            VoxyCompatibilityPatch.LOGGER.info(
                    "Ignored request to disable locked Voxy setting enableRendering");
            ci.cancel();
        }
    }
}
