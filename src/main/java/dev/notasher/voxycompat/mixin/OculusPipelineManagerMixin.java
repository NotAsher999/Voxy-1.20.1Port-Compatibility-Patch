package dev.notasher.voxycompat.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aligns Voxy's renderer generation with the Oculus pipeline generation.
 *
 * <p>Voxy 0.2.13 captures Iris/Oculus pipeline data when its render system is
 * constructed. If Oculus later replaces that pipeline, the old Voxy renderer
 * retains sampler suppliers backed by destroyed render targets.</p>
 *
 * <p>{@code preparePipeline} runs during ordinary frames and is not a lifecycle
 * signal by itself. A completed {@code destroyPipeline} therefore arms exactly
 * one refresh, consumed only after the replacement pipeline is ready.</p>
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.PipelineManager", remap = false)
public abstract class OculusPipelineManagerMixin {
    @Unique
    private boolean voxyCompat$refreshPending;

    @Inject(method = "destroyPipeline", at = @At("TAIL"), remap = false, require = 1)
    private void voxyCompat$markDestroyedPipeline(final CallbackInfo ci) {
        this.voxyCompat$refreshPending = true;
    }

    @Inject(method = "preparePipeline", at = @At("RETURN"), remap = false, require = 1)
    private void voxyCompat$refreshAfterReplacementPipeline(final CallbackInfoReturnable<Object> cir) {
        if (!this.voxyCompat$refreshPending) {
            return;
        }

        this.voxyCompat$refreshPending = false;
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.levelRenderer != null) {
            minecraft.levelRenderer.allChanged();
        }
    }
}
