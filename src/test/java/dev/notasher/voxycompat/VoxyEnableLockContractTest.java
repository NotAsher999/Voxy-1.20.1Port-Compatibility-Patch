package dev.notasher.voxycompat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxyEnableLockContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void mixinsCoverLoadSaveInitializationAndBothDisableSetters() throws IOException {
        final String persistence = readMain("mixin/VoxyConfigPersistenceMixin.java");
        final String lifecycle = readMain("mixin/VoxyClientConfigLifecycleMixin.java");
        final String screen = readMain("mixin/VoxyConfigScreenPagesMixin.java");
        final String mixinConfig = Files.readString(ROOT.resolve(
                "src/main/resources/voxy-compatibility-patch.mixins.json"));

        assertTrue(persistence.contains("me.cortex.voxy.client.config.VoxyConfig"));
        assertTrue(persistence.contains("loadOrCreate()Lme/cortex/voxy/client/config/VoxyConfig;"));
        assertTrue(persistence.contains("method = \"save()V\", at = @At(\"HEAD\")"));
        assertTrue(lifecycle.contains("me.cortex.voxy.client.VoxyClient"));
        assertTrue(lifecycle.contains("method = \"initVoxyClient()V\", at = @At(\"RETURN\")"));
        assertTrue(screen.contains("lambda$page$0(Lme/cortex/voxy/client/config/VoxyConfig;Ljava/lang/Boolean;)V"));
        assertTrue(screen.contains("lambda$page$10(Lme/cortex/voxy/client/config/VoxyConfig;Ljava/lang/Boolean;)V"));
        assertTrue(screen.contains("Boolean.FALSE.equals(requested)"));
        assertTrue(screen.contains("ci.cancel()"));

        assertTrue(mixinConfig.contains("VoxyConfigPersistenceMixin"));
        assertTrue(mixinConfig.contains("VoxyClientConfigLifecycleMixin"));
        assertTrue(mixinConfig.contains("VoxyConfigScreenPagesMixin"));
    }

    @Test
    void enableLockDoesNotBypassVoxysAvailabilityContract() throws IOException {
        final String enforcer = readMain("VoxyConfigEnforcer.java");
        final String persistence = readMain("mixin/VoxyConfigPersistenceMixin.java");
        final String lifecycle = readMain("mixin/VoxyClientConfigLifecycleMixin.java");
        final String screen = readMain("mixin/VoxyConfigScreenPagesMixin.java");
        final String combined = enforcer + persistence + lifecycle + screen;

        assertFalse(combined.contains("VoxyCommon.isAvailable"));
        assertFalse(combined.contains("isRenderingEnabled"));
        assertFalse(combined.contains("createRenderer"));
        assertFalse(combined.contains("createInstance"));
        assertFalse(combined.contains("Capabilities"));
    }

    private static String readMain(final String relative) throws IOException {
        return Files.readString(ROOT.resolve(
                "src/main/java/dev/notasher/voxycompat").resolve(relative).normalize());
    }
}
