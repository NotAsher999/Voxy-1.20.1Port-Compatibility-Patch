package dev.notasher.voxycompat.mixin;

import dev.notasher.voxycompat.VoxyConfigEnforcer;
import dev.notasher.voxycompat.VoxyCompatibilityPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Locks Voxy's activation values at its load and persistence boundaries. */
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.config.VoxyConfig", priority = 900, remap = false)
public abstract class VoxyConfigPersistenceMixin {
    @Inject(
            method = "loadOrCreate()Lme/cortex/voxy/client/config/VoxyConfig;",
            at = @At("RETURN"))
    private static void voxyCompat$enforceLoadedConfig(final CallbackInfoReturnable<Object> cir) {
        if (VoxyConfigEnforcer.enforceLoadedConfig(cir.getReturnValue())) {
            VoxyCompatibilityPatch.LOGGER.info(
                    "Restored Voxy enabled and rendering settings while loading its client config");
        }
    }

    @Inject(method = "save()V", at = @At("HEAD"))
    private void voxyCompat$enforceBeforeSave(final CallbackInfo ci) {
        if (VoxyConfigEnforcer.enforceBeforeSave(this)) {
            VoxyCompatibilityPatch.LOGGER.info(
                    "Prevented disabled Voxy activation settings from being persisted");
        }
    }
}
