package dev.notasher.voxycompat.mixin;

import dev.notasher.voxycompat.VoxyCompatibilityPatch;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

/**
 * Circuit breaker for the confirmed high-volume NVIDIA diagnostic emitted when
 * a renderer uploads uniforms with no OpenGL program bound.
 *
 * <p>This is deliberately narrower than filtering error id {@code 1282}, which
 * represents many unrelated {@code GL_INVALID_OPERATION} failures. The first
 * messages in each window still pass through Minecraft's normal debug path;
 * repeated copies are counted and summarized without invoking Voxy's expensive
 * per-message stack inspection.</p>
 */
@Pseudo
@Mixin(targets = "com.mojang.blaze3d.platform.GlDebug", priority = 2000, remap = false)
public abstract class GlDebugFloodGuardMixin {
    @Unique
    private static final String VOXY_COMPAT$NO_ACTIVE_PROGRAM =
            "GL_INVALID_OPERATION error generated. No active program.";

    @Unique
    private static final int VOXY_COMPAT$PASSTHROUGH_LIMIT = Math.max(1,
            Integer.getInteger("voxyCompat.noActiveLogBurst", 1));

    @Unique
    private static final long VOXY_COMPAT$WINDOW_SECONDS = Math.max(1L,
            Long.getLong("voxyCompat.noActiveLogWindowSeconds", 10L));

    @Unique
    private static final long VOXY_COMPAT$WINDOW_NANOS =
            TimeUnit.SECONDS.toNanos(VOXY_COMPAT$WINDOW_SECONDS);

    @Unique
    private static long voxyCompat$windowStartNanos;

    @Unique
    private static int voxyCompat$passedInWindow;

    @Unique
    private static long voxyCompat$suppressedInWindow;

    @Inject(
            method = "m_84038_(IIIIIJJ)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private static void voxyCompat$limitNoActiveProgramFlood(
            final int source,
            final int type,
            final int id,
            final int severity,
            final int length,
            final long messagePointer,
            final long userParam,
            final CallbackInfo ci) {
        if (id != 1282) {
            return;
        }

        final String message = GLDebugMessageCallback.getMessage(length, messagePointer);
        if (!VOXY_COMPAT$NO_ACTIVE_PROGRAM.equals(message)) {
            return;
        }

        final long now = System.nanoTime();
        if (voxyCompat$windowStartNanos == 0L) {
            voxyCompat$windowStartNanos = now;
        } else if (now - voxyCompat$windowStartNanos >= VOXY_COMPAT$WINDOW_NANOS) {
            if (voxyCompat$suppressedInWindow > 0L) {
                VoxyCompatibilityPatch.LOGGER.warn(
                        "Suppressed {} repeated 'No active program' OpenGL messages in the previous {} seconds; "
                                + "the underlying render-state error is still active",
                        voxyCompat$suppressedInWindow,
                        VOXY_COMPAT$WINDOW_SECONDS);
            }

            voxyCompat$windowStartNanos = now;
            voxyCompat$passedInWindow = 0;
            voxyCompat$suppressedInWindow = 0L;
        }

        if (voxyCompat$passedInWindow < VOXY_COMPAT$PASSTHROUGH_LIMIT) {
            voxyCompat$passedInWindow++;
            return;
        }

        voxyCompat$suppressedInWindow++;
        ci.cancel();
    }
}
