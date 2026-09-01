package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxyWorldActivationContractTest {
    @Test
    void pollingStartsOnlyInsideAWorldAndStopsAfterVerifiedActivation() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String service = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/VoxyWorldActivationService.java"));

        assertTrue(service.contains("POLL_INTERVAL_TICKS = 40"));
        assertTrue(service.contains("if (level == null)"));
        assertTrue(service.contains("resetWorldState();"));
        assertTrue(service.contains("if (level != trackedLevel)"));
        assertTrue(service.contains("pollingComplete || --ticksUntilPoll > 0"));
        assertTrue(service.contains("case SUCCESS"));
        assertTrue(service.contains("pollingComplete = true"));
        assertTrue(service.contains("message.voxy_compat_patch.voxy_enabled"));
    }

    @Test
    void activationUsesVoxyRealSettingPathsAndRejectsTheOldValueLock() throws IOException {
        final Path root = Path.of(System.getProperty("user.dir"));
        final String bridge = Files.readString(root.resolve(
                "src/main/java/dev/notasher/voxycompat/VoxyActivationBridge.java"));
        final String mixins = Files.readString(root.resolve(
                "src/main/resources/voxy-compatibility-patch.mixins.json"));

        assertTrue(bridge.contains("me.cortex.voxy.client.config.VoxyConfigScreenPages"));
        assertTrue(bridge.contains("lambda$page$0"));
        assertTrue(bridge.contains("lambda$page$10"));
        assertTrue(bridge.contains("me.cortex.voxy.commonImpl.VoxyCommon"));
        assertTrue(bridge.contains("me.cortex.voxy.client.ClientSessionEvents"));
        assertTrue(bridge.contains("getVoxyRenderSystem"));
        assertFalse(mixins.contains("VoxyConfigEnforcer"));
        assertFalse(mixins.contains("VoxyConfigLoadMixin"));
        assertFalse(mixins.contains("VoxyConfigSaveMixin"));
        assertFalse(bridge.contains("Files.write"));
    }
}
