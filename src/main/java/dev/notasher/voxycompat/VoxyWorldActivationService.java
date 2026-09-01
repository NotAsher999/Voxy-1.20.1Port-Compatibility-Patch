package dev.notasher.voxycompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Low-frequency, per-world activation of Voxy through its own runtime path. */
@Mod.EventBusSubscriber(
        modid = VoxyCompatibilityPatch.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoxyWorldActivationService {
    static final int POLL_INTERVAL_TICKS = 40;

    private static ClientLevel trackedLevel;
    private static int ticksUntilPoll;
    private static boolean pollingComplete;
    private static boolean retryFailureLogged;

    private VoxyWorldActivationService() {
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            resetWorldState();
            return;
        }

        if (level != trackedLevel) {
            trackedLevel = level;
            ticksUntilPoll = POLL_INTERVAL_TICKS;
            pollingComplete = false;
            retryFailureLogged = false;
        }

        if (pollingComplete || --ticksUntilPoll > 0) {
            return;
        }
        ticksUntilPoll = POLL_INTERVAL_TICKS;

        final VoxyActivationBridge.Attempt attempt = VoxyActivationBridge.attempt(minecraft.levelRenderer);
        switch (attempt.status()) {
            case WAITING -> {
                // Voxy or its client session is not ready yet; retry at low frequency.
            }
            case RETRY -> {
                if (attempt.failure() != null && !retryFailureLogged) {
                    retryFailureLogged = true;
                    VoxyCompatibilityPatch.LOGGER.warn(
                            "Voxy activation was not ready and will be retried for this world",
                            attempt.failure());
                }
            }
            case SUCCESS -> {
                pollingComplete = true;
                VoxyCompatibilityPatch.LOGGER.info(
                        "Voxy world instance and renderer are active; activation polling stopped for this world");
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.voxy_compat_patch.voxy_enabled"),
                            false);
                }
            }
            case FATAL -> {
                pollingComplete = true;
                VoxyCompatibilityPatch.LOGGER.error(
                        "Voxy activation contract failed; polling stopped for this world",
                        attempt.failure());
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.voxy_compat_patch.voxy_enable_failed"),
                            false);
                }
            }
        }
    }

    private static void resetWorldState() {
        trackedLevel = null;
        ticksUntilPoll = 0;
        pollingComplete = false;
        retryFailureLogged = false;
    }
}
