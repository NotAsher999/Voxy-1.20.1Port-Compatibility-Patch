package dev.notasher.voxycompat.mixin;

import dev.notasher.voxycompat.VoxyConfigEnforcer;
import dev.notasher.voxycompat.VoxyCompatibilityPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persists the enable lock after Voxy has established its availability. */
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.VoxyClient", priority = 900, remap = false)
public abstract class VoxyClientConfigLifecycleMixin {
    @Inject(method = "initVoxyClient()V", at = @At("RETURN"))
    private static void voxyCompat$persistEnabledConfigAfterInitialization(final CallbackInfo ci) {
        if (VoxyConfigEnforcer.enforceAfterVoxyInitialization()) {
            VoxyCompatibilityPatch.LOGGER.info(
                    "Restored Voxy enabled and rendering settings after client initialization");
        }
    }
}
